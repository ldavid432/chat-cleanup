package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.CA_ID_PREFIX;
import static com.github.ldavid432.cleanchat.CleanChatUtil.FULL_CA_PATTERN;
import static com.github.ldavid432.cleanchat.CleanChatUtil.PREFIX_CA_PATTERN;
import static com.github.ldavid432.cleanchat.CleanChatUtil.TARGET_CA_PATTERN;
import static com.github.ldavid432.cleanchat.CleanChatUtil.caTag;
import static com.github.ldavid432.cleanchat.CleanChatUtil.createCaMenuEntry;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getCancelEntry;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getChatLineBuffer;
import static com.github.ldavid432.cleanchat.CleanChatUtil.imageTag;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeUsername;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import com.github.ldavid432.cleanchat.data.ChatNameReplacement;
import com.github.ldavid432.cleanchat.data.ChatStartupTrigger;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.MessageNode;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@PluginDescriptor(
	name = "Clean Chat",
	description = "Hide clan name and more to clean your chat channels, includes GIM, friends, and clan chats",
	tags = {"clean", "chat", "clan", "friends", "gim", "group", "iron", "ironman", "channel"}
)
public class CleanChatChannelsPlugin extends Plugin
{
	// Random ID used to identify our specific challenge entry
	private static final String CLAN_CHALLENGE_ENTRY_HIDER = "<col=1337000007331>";
	// Random ID used to identify our specific entries
	private static final String CLAN_MESSAGE_ENTRY_HIDER = "<col=2337000007332>";

	private static final String CLEAN_CHAT_SENDER = "clean-chat-plugin";

	@Inject
	private Client client;

	@Inject
	private CleanChatChannelsConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ColorManager colorManager;

	@Inject
	private ChatIconManager chatIconManager;

	private ScheduledExecutorService executor;

	@Provides
	CleanChatChannelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CleanChatChannelsConfig.class);
	}

	private static final List<ChatMessageType> CHAT_MESSAGE_TYPES_TO_PROCESS;

	static
	{
		CHAT_MESSAGE_TYPES_TO_PROCESS = Stream.concat(
			Arrays.stream(ChatBlock.values()).map(ChatBlock::getChatMessageType),
			Arrays.stream(ChatNameReplacement.values()).flatMap(it -> it.getFromChatMessageTypes().stream())
		).distinct().collect(Collectors.toList());
	}

	@Override
	protected void startUp() throws Exception
	{
		if (executor == null || executor.isShutdown())
		{
			executor = Executors.newSingleThreadScheduledExecutor();
		}

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Plugin enabled. Refreshing chat.");
			processAllChatHistory();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		executor.shutdown();
		executor = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (Objects.equals(event.getGroup(), CleanChatChannelsConfig.GROUP))
		{
			log.debug("Config changed. Refreshing chat.");
			processAllChatHistory();
		}
	}

	// Subscribe later since we transform the message type, we want to interfere with other chat plugins as little as possible
	@Subscribe(priority = -1)
	public void onChatMessage(ChatMessage event)
	{
		// Avoid stack overflow, ignore other types
		if ((event.getSender() != null && event.getSender().startsWith(CLEAN_CHAT_SENDER)) || !CHAT_MESSAGE_TYPES_TO_PROCESS.contains(event.getType()))
		{
			return;
		}

		// This is when chat history sends old chats, so we wait a bit for it to populate and then run our stuff
		if (event.getMessage().equals(ChatBlock.WELCOME.getMessage()))
		{
			log.debug("World hopped or logged in. Refreshing chat.");
			// Only process blocks because we want to wait for the individual chats to connect before replacing
			//  This makes startup much less jarring
			clientThread.invokeLater(() -> processChatHistory(CHAT_MESSAGE_TYPES_TO_PROCESS, this::processBlocks));
		}

		// TODO: Possibly insert a placeholder <col> in replacements for icons so the messages can be sent immediately
		//  and the icons can potentially be "lazy loaded"

		// Wait for each of the channels to load before running replacements - otherwise we won't get icon info
		for (ChatStartupTrigger trigger : ChatStartupTrigger.values())
		{
			List<ChatMessageType> messageTypes = trigger.getOutputTypesFor(config, event);
			if (!messageTypes.isEmpty())
			{
				log.debug("{} chat connected, refreshing.", trigger.name().toLowerCase());
				processChatHistory(messageTypes);
				break;
			}
		}

		processMessage(event);
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();

		MenuEntry[] newEntries = menuEntries;

		// Hide all entries for clan broadcasts, except cancel
		if (Arrays.stream(menuEntries).anyMatch(e -> e.getTarget().contains(CLAN_MESSAGE_ENTRY_HIDER)))
		{
			newEntries = getCancelEntry(client);
		}
		// Add back Open and View Task to CA broadcasts
		else if (Arrays.stream(menuEntries).anyMatch(e -> e.getTarget().contains(CA_ID_PREFIX)))
		{
			MenuEntry caEntry = Arrays.stream(menuEntries).filter(e -> e.getTarget().contains(CA_ID_PREFIX)).findFirst().orElse(null);
			if (caEntry != null)
			{
				Matcher matcher = TARGET_CA_PATTERN.matcher(caEntry.getTarget());

				if (matcher.matches() && matcher.group(1) != null)
				{
					try
					{
						int caId = Integer.parseInt(matcher.group(1));

						newEntries = ArrayUtils.addAll(
							getCancelEntry(client),
							// Disabled since it "sends additional actions to the server" whereas View is handled client side
							// createCaMenuEntry(client, 7, "Open", caId),
							createCaMenuEntry(client, 6, "View", caId));
					}
					catch (NumberFormatException ignored)
					{
					}
				}
			}
		}
		// Since clan chat type gets mapped to the clan challenge type we want to block the challenge entry so it seems like a normal message
		else if (Arrays.stream(menuEntries).anyMatch(e -> e.getTarget().contains(CLAN_CHALLENGE_ENTRY_HIDER)))
		{
			newEntries = Arrays.stream(menuEntries)
				.filter(e -> !(Objects.equals(e.getOption(), "Accept challenge") && e.getTarget().contains(CLAN_CHALLENGE_ENTRY_HIDER)))
				.toArray(MenuEntry[]::new);
		}

		client.getMenu().setMenuEntries(newEntries);
	}

	// Block, replace or do nothing
	private void processMessage(ChatMessage event)
	{
		boolean blockApplied = processBlocks(event);
		if (blockApplied)
		{
			return;
		}

		processReplacements(event);
	}

	private boolean processBlocks(ChatMessage event)
	{
		boolean blockMessage = shouldBlockMessage(event);
		if (blockMessage)
		{
			log.debug("Blocking message: {}", event.getMessage());
			removeChatMessage(event.getType(), event.getMessageNode());
			client.refreshChat();
		}
		return blockMessage;
	}

	public void processReplacements(ChatMessage event)
	{
		ChatNameReplacement replacement = getNameReplacement(event);
		if (replacement != null)
		{
			MessageNode oldMessageNode = event.getMessageNode();
			ChatMessageType newType = replacement.getToChatMessageType();
			log.debug("Got replaceable message, replacing {} to {} for {}", event.getType(), newType, event.getMessage());
			Color messageColor = colorManager.getMessageColor(event.getType());
			Color usernameColor = colorManager.getUsernameColor(event.getType());
			boolean nameIsUsername = true;

			// TODO: Use message builders for name + message

			String name = event.getName();

			// If we just put the name straight into the message it will end up being highlighted and/or underlined
			//  so we wrap everything but the first letter in a color type to throw off the highlighting
			if (!name.isEmpty())
			{
				int imgIndex = name.indexOf('>');
				if (imgIndex == -1)
				{
					if (name.length() > 1)
					{
						name = name.charAt(0) + "<col=normal>" + name.substring(1) + "</col>";
					}
					// Not sure how to handle 1 character names ¯\_(ツ)_/¯
				}
				else
				{
					// Account for ironman icons
					// Add 2 because substring is not inclusive, and we want to go 1 character into the real name
					name = name.substring(0, imgIndex + 2) + "<col=normal>" + name.substring(imgIndex + 2) + "</col>";
				}
			}

			switch (event.getType())
			{
				case FRIENDSCHAT:
					// Add friends chat rank
					FriendsChatManager friendsChatManager = client.getFriendsChatManager();
					if (friendsChatManager != null)
					{
						String sanitizedName = sanitizeUsername(name);

						FriendsChatMember member = Arrays.stream(friendsChatManager.getMembers())
							.filter(it -> sanitizeUsername(it.getName()).equals(sanitizedName))
							.findFirst()
							.orElse(null);

						if (member != null && member.getRank() != FriendsChatRank.UNRANKED)
						{
							name = imageTag(chatIconManager.getIconNumber(member.getRank())) + name;
						}
					}
					break;
				case CLAN_CHAT:
					// Add clan rank
					ClanChannel clanChannel = client.getClanChannel(ClanID.CLAN);
					ClanSettings clanSettings = client.getClanSettings(ClanID.CLAN);
					if (clanChannel != null && clanSettings != null)
					{
						String sanitizedName = sanitizeUsername(name);

						ClanChannelMember member = clanChannel.getMembers().stream()
							.filter(it -> sanitizeUsername(it.getName()).equals(sanitizedName))
							.findFirst()
							.orElse(null);

						if (member != null)
						{
							ClanTitle title = clanSettings.titleForRank(member.getRank());
							if (title != null)
							{
								name = imageTag(chatIconManager.getIconNumber(title)) + name;
							}
						}
					}
					break;
				case CLAN_GIM_MESSAGE:
					// Have to add the group name back in this scenario
					if (!config.removeGroupIronName() && config.removeGroupIronFromClan())
					{
						ClanChannel groupIronChannel = client.getClanChannel(ClanID.GROUP_IRONMAN);
						if (groupIronChannel != null)
						{
							String groupIronName = groupIronChannel.getName();
							if (groupIronName != null)
							{
								name = "[" + ColorUtil.wrapWithColorTag(groupIronName, colorManager.getGimNameColor()) + "] " + name;
								nameIsUsername = false;
							}
						}
					}
					break;
				case CLAN_MESSAGE:
					if (event.getMessage().startsWith(CA_ID_PREFIX))
					{
						Matcher matcher = FULL_CA_PATTERN.matcher(event.getMessage());
						if (matcher.matches() && matcher.groupCount() == 1)
						{
							name += caTag(matcher.group(1));

						}

						oldMessageNode.setValue(oldMessageNode.getValue().replaceFirst(PREFIX_CA_PATTERN, ""));

						if (oldMessageNode.getRuneLiteFormatMessage() != null)
						{
							oldMessageNode.setRuneLiteFormatMessage(oldMessageNode.getRuneLiteFormatMessage().replaceFirst(PREFIX_CA_PATTERN, ""));
						}
					}
					else
					{
						name += CLAN_MESSAGE_ENTRY_HIDER;
					}

					break;
			}

			removeChatMessage(event.getType(), oldMessageNode);

			String menuName = name + CLAN_CHALLENGE_ENTRY_HIDER;

			if (!sanitizeUsername(name).isBlank() && nameIsUsername)
			{
				name = ColorUtil.wrapWithColorTag(name, usernameColor);
				name += ": ";
			}

			String message = oldMessageNode.getValue();
			String rlFormatMessage = oldMessageNode.getRuneLiteFormatMessage();

			MessageNode newNode = client.addChatMessage(
				newType,
				menuName,
				name + ColorUtil.colorTag(messageColor) + (rlFormatMessage != null ? rlFormatMessage : message),
				CLEAN_CHAT_SENDER,
				false
			);

			newNode.setTimestamp(oldMessageNode.getTimestamp());

			if (event.getMessage().startsWith("!"))
			{
				processChatCommand(oldMessageNode, newNode, name, messageColor, message, rlFormatMessage);
			}
		}
	}

	private boolean shouldBlockMessage(ChatMessage event)
	{
		return Stream.of(ChatBlock.values()).anyMatch(it -> it.appliesTo(config, event));
	}

	private ChatNameReplacement getNameReplacement(ChatMessage event)
	{
		return Stream.of(ChatNameReplacement.values())
			.filter(it -> it.appliesTo(config, event))
			.findFirst()
			.orElse(null);
	}

	private void removeChatMessage(ChatMessageType chatMessageType, MessageNode messageNode)
	{
		ChatLineBuffer buffer = getChatLineBuffer(client, chatMessageType);
		if (buffer != null)
		{
			buffer.removeMessageNode(messageNode);
		}
	}

	private void processAllChatHistory()
	{
		processChatHistory(CHAT_MESSAGE_TYPES_TO_PROCESS);
	}

	private void processChatHistory(List<ChatMessageType> types)
	{
		processChatHistory(types, this::processMessage);
	}

	private void processChatHistory(List<ChatMessageType> types, Consumer<ChatMessage> processor)
	{
		types.stream()
			.flatMap(type -> {
				ChatLineBuffer buffer = getChatLineBuffer(client, type);
				if (buffer == null)
				{
					return Stream.empty();
				}
				return Arrays.stream(buffer.getLines().clone()).filter(Objects::nonNull).map(node -> Pair.of(type, node));
			})
			.sorted(Comparator.comparingInt(pair -> pair.getValue().getTimestamp()))
			.forEach(pair -> {
				MessageNode messageNode = pair.getValue();
				ChatMessageType type = pair.getKey();
				// Ignore message types that don't match (this will only happen with gim chat vs clan chat)
				if (messageNode == null || type != messageNode.getType())
				{
					return;
				}
				ChatMessage event = new ChatMessage(messageNode, type, messageNode.getName(), messageNode.getValue(), messageNode.getSender(), messageNode.getTimestamp());
				clientThread.invoke(() -> processor.accept(event));
			});
	}

	// Pass the old message contents in from where we use them so the comparison is accurate to what is displayed
	//  Probably not totally necessary but, just in case
	private void processChatCommand(final MessageNode oldNode, final MessageNode newNode, final String name, final Color messageColor,
									final String oldNodeOriginalMessage, final String oldNodeOriginalRLFormatMessage)
	{
		final int timeout = config.chatCommandTimeout();

		log.debug("Replaceable chat command found, waiting up to {}.25s for it to update.", timeout);

		if (executor == null || executor.isShutdown())
		{
			return;
		}

		Runnable task = new FixedCountRunnable(
			(cancel) -> {
				if (!Objects.equals(oldNode.getRuneLiteFormatMessage(), oldNodeOriginalRLFormatMessage) && oldNode.getRuneLiteFormatMessage() != null)
				{
					newNode.setRuneLiteFormatMessage(name + ColorUtil.colorTag(messageColor) + oldNode.getRuneLiteFormatMessage());
					client.refreshChat();
					cancel.run();
					return;
				}

				if (!Objects.equals(oldNode.getValue(), oldNodeOriginalMessage) && oldNode.getValue() != null)
				{
					newNode.setRuneLiteFormatMessage(name + ColorUtil.colorTag(messageColor) + oldNode.getValue());
					client.refreshChat();
					cancel.run();
					return;
				}
			},
			timeout * 2,
			() -> log.debug("No chat command update found after waiting {}.25s", timeout)
		);

		executor.scheduleAtFixedRate(task, 250, 500, TimeUnit.MILLISECONDS);
	}
}
