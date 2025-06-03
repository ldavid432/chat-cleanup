package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.getChatLineBuffer;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeUsername;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import com.github.ldavid432.cleanchat.data.NameReplacement;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.FriendsChatChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@PluginDescriptor(
	name = "Clean Chat",
	description = "Hide clan name and more to clean your chat channels, includes GIM, friends, and clan chats",
	tags = {"clean", "chat", "clan", "friends", "gim", "group", "iron", "ironman", "channel"}
)
public class CleanChatChannelsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	@Getter
	private CleanChatChannelsConfig config;

	@Inject
	private ClientThread clientThread;

	private ScheduledExecutorService executor;

	@Getter
	private String clanName = null;
	@Getter
	private String guestClanName = null;
	@Getter
	private String friendsChatName = null;
	@Getter
	private String groupIronName = null;
	private boolean inFriendsChat = false;

	private List<String> getNamesToRemove()
	{
		return Stream.of(
				config.removeClanName() ? clanName : null,
				config.removeGuestClanName() ? guestClanName : null,
				config.removeFriendsChatName() ? friendsChatName : null,
				config.removeGroupIronName() ? groupIronName : null)
			.filter(Objects::nonNull)
			.map(name -> name.replace('\u00A0', ' '))
			.collect(Collectors.toList());
	}

	@Provides
	CleanChatChannelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CleanChatChannelsConfig.class);
	}

	private static final List<ChatMessageType> CHAT_MESSAGE_TYPES_TO_PROCESS = Arrays.stream(ChatBlock.values()).map(ChatBlock::getChatMessageType).distinct().collect(Collectors.toList());

	// TODO: Remove
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (event.getCommand().equals("cleanchat"))
		{
			switch (event.getArguments()[0].toUpperCase())
			{
				case "A":
					break;
				case "CA":
					client.addChatMessage(
						ChatMessageType.CLAN_MESSAGE,
						"",
						"CA_ID:532|the lil fish has completed a hard combat task: Fortified",
						"Click Clique",
						true
					);
					break;
				case "F":
					client.addChatMessage(
						ChatMessageType.FRIENDSCHAT,
						"<img=41>the lil fish",
						"Test friends chat .",
						"Fish",
						true
					);
					break;
				case "FM":
					client.addChatMessage(
						ChatMessageType.FRIENDSCHATNOTIFICATION,
						"",
						"Test friends notification",
						null,
						true
					);
					break;
				case "C":
					client.addChatMessage(
						ChatMessageType.CLAN_CHAT,
						"<img=41>the lil fish",
						"Test clan chat",
						"Click Clique",
						true
					);
					break;
				case "CM":
					client.addChatMessage(
						ChatMessageType.CLAN_MESSAGE,
						"",
						"Test clan message",
						"Click Clique",
						true
					);
					break;
				case "GC":
					client.addChatMessage(
						ChatMessageType.CLAN_GUEST_CHAT,
						"<img=41>the lil fish",
						"Test guest clan chat",
						"My Guest Clan",
						true
					);
					break;
				case "GCM":
					client.addChatMessage(
						ChatMessageType.CLAN_GUEST_MESSAGE,
						"",
						"Test guest clan message",
						"My Guest Clan",
						true
					);
					break;
				case "G":
					client.addChatMessage(
						ChatMessageType.CLAN_GIM_CHAT,
						"<img=41>the lil fish",
						"Test GIM chat",
						"Konars\u00A0Simps",
						true
					);
					break;
				case "GM":
					client.addChatMessage(
						ChatMessageType.CLAN_GIM_MESSAGE,
						"",
						"Test GIM message",
						"Konars\u00A0Simps",
						true
					);
					break;
			}
		}
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
			processChatHistory();
			client.refreshChat();
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
			processChatHistory();
			client.refreshChat();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// Avoid stack overflow, ignore other types
		if (!CHAT_MESSAGE_TYPES_TO_PROCESS.contains(event.getType()))
		{
			return;
		}

		// This is when chat history sends old chats, so we wait a bit for it to populate and then run our stuff
		if (event.getMessage().equals(ChatBlock.WELCOME.getMessage()))
		{
			log.debug("World hopped or logged in. Refreshing chat.");
			clientThread.invokeLater(this::processChatHistory);
		}

		processBlocks(event);
	}

	private void processBlocks(ChatMessage event)
	{
		boolean blockMessage = shouldBlockMessage(event);
		if (blockMessage)
		{
			log.debug("Blocking message: {}", event.getMessage());
			removeChatMessage(event.getType(), event.getMessageNode());
			client.refreshChat();
		}
	}

	private boolean shouldBlockMessage(ChatMessage event)
	{
		return Stream.of(ChatBlock.values()).anyMatch(it -> it.appliesTo(config, event));
	}

	private void removeChatMessage(ChatMessageType chatMessageType, MessageNode messageNode)
	{
		ChatLineBuffer buffer = getChatLineBuffer(client, chatMessageType);
		if (buffer != null)
		{
			buffer.removeMessageNode(messageNode);
		}
	}

	private void processChatHistory()
	{
		CHAT_MESSAGE_TYPES_TO_PROCESS.stream()
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
				clientThread.invoke(() -> processBlocks(event));
			});
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (!Objects.equals(84, event.getScriptId()) || getNamesToRemove().isEmpty()) return;

		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		if (friendsChatManager != null)
		{
			friendsChatName = friendsChatManager.getName();
		}

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);

		if (chatbox != null)
		{
			Widget[] lines = chatbox.getDynamicChildren().clone();

			/*
			Most chats appear in this format as dynamic children:
				// bottom chat line
				[0] = username
				[1] = chat message
				[2] = sender (clan name, also includes timestamp if that plugin is on)
				[3] = rank icon
				// Next chat line
				[4] = next username
				etc...

			Friends chats appear in this format:
				[0] = sender + username
				[1] = chat message
				[2] = nothing?
				[3] = rank icon?

			These appear in the children array in this order even if the individual items aren't rendered
				ex: Username is hidden for broadcasts, rank icon is hidden for public chat

			Since we only need to check either [2] or [0] of each line for the sender we can iterate over every other child */
			for (int i = 0; i < lines.length; i += 2)
			{
				Widget widget = lines[i];
				if (!widget.getText().isBlank()) {
					// FriendsChatManager is null at login so we have to add this check later
					if (inFriendsChat && friendsChatName == null)
					{
						setFriendsChatName();
					}

					log.debug("Names to remove {}", getNamesToRemove());
					for (NameReplacement replacement : NameReplacement.getEnabledReplacements(config))
					{
						String name = replacement.getName(this);
						String formattedName = "[" + name + "]";
						if (sanitizeUsername(widget.getText()).contains(formattedName)) {
							// Account for color tags
							// TODO: Possibly invert NBSP handling
							String newText = widget.getText().replace('\u00A0', ' ').replaceFirst("\\[.*" + name + ".*]", "");
							// TODO: Potentially remove any trailing spaces - should only be left if chat timestamps is on - also friends chat username + sender combo
							// TODO: Remove log
							log.debug("Replaced Text {} with {}", widget.getText(), newText);
							widget.setText(newText);

							int removedLength = getTextLength(formattedName);

							if (removedLength == -1)
							{
								log.debug("Couldn't get text length for text: {}", widget.getText());
								continue;
							}

							widget.setOriginalWidth(widget.getOriginalWidth() - removedLength);
							widget.revalidate();

							int iconWidgetIndex = i + 1;
							int textWidgetIndex = i - 1;
							int nameWidgetIndex = i - 2;

							// Friends chat widget ordering is different
							if (replacement == NameReplacement.FRIENDS_CHAT) {
								textWidgetIndex = i + 1;
								// Not actually name in this case
								nameWidgetIndex = i + 2;
								iconWidgetIndex = i + 3;
							}

							if (iconWidgetIndex >= 0 && iconWidgetIndex < lines.length) {
								Widget iconWidget = lines[iconWidgetIndex];
								shiftWidgetLeft(iconWidget, removedLength);
							}

							if (textWidgetIndex >= 0 && textWidgetIndex < lines.length) {
								Widget textWidget = lines[textWidgetIndex];
								shiftWidgetLeft(textWidget, removedLength);
							}

							if (nameWidgetIndex >= 0 && nameWidgetIndex < lines.length) {
								Widget nameWidget = lines[nameWidgetIndex];
								shiftWidgetLeft(nameWidget, removedLength);
							}

							// break name replacement loop, not line loop
							break;
						}
					}
				}
			}
		}
	}

	private void shiftWidgetLeft(Widget widget, int shift)
	{
		widget.setOriginalX(widget.getOriginalX() - shift);
		widget.revalidate();
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		String channelName = event.getClanChannel() != null ? event.getClanChannel().getName() : null;

		log.debug("Connected to clan: {} - ID: {}", channelName, event.getClanId());

		switch (event.getClanId()) {
			case -1:
				guestClanName = channelName;
				break;
			case ClanID.CLAN:
				clanName = channelName;
				break;
			case ClanID.GROUP_IRONMAN:
				groupIronName = channelName;
				break;
		}
	}

	@Subscribe
	public void onFriendsChatChanged(FriendsChatChanged event)
	{
		log.debug("fc change: {}", event.isJoined());
		inFriendsChat = event.isJoined();

		if (event.isJoined()) {
			setFriendsChatName();
		} else {
			friendsChatName = null;
		}
	}

	private void setFriendsChatName()
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		// This is null at login
		if (friendsChatManager != null)
		{
			friendsChatName = friendsChatManager.getName();
		}
	}

}
