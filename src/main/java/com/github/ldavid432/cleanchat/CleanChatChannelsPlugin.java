package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.getChatLineBuffer;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeUsername;
import com.github.ldavid432.cleanchat.data.ChannelNameReplacement;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import static com.github.ldavid432.cleanchat.data.ChatBlock.getBlockedMessageTypes;
import com.github.ldavid432.cleanchat.data.ChatChannel;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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
	private CleanChatChannelsConfig config;

	@Inject
	private ChannelNameManager channelNameManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private ClientThread clientThread;

	private ScheduledExecutorService executor;

	@Provides
	CleanChatChannelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CleanChatChannelsConfig.class);
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

		eventBus.register(channelNameManager);
	}

	@Override
	protected void shutDown() throws Exception
	{
		executor.shutdown();
		executor = null;

		eventBus.unregister(channelNameManager);
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
		if (!getBlockedMessageTypes(config).contains(event.getType()) || event.getType() == ChatMessageType.WELCOME)
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

	private ChatChannel getSelectedChannel()
	{
		return ChatChannel.of(client.getVarcIntValue(41));
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// TODO: Find better script to run after?
		if (event.getScriptId() != 84)
		{
			return;
		}

		List<ChannelNameReplacement> replacements = ChannelNameReplacement.getEnabledReplacements(config);

		if (replacements.isEmpty())
		{
			return;
		}

		// FriendsChatManager is null at the first FriendsChatChanged after login so we have to add this check later
		channelNameManager.setFriendsChatNameIfNeeded();

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);

		if (chatbox != null && getSelectedChannel() != ChatChannel.CLOSED)
		{
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
				[2] = nothing
				[3] = rank icon?

			GIM broadcasts appear in this format:
				[0] = sender
				[1] = chat message
				[2] = nothing
				[3] = nothing

			These appear in the children array in this order even if the individual items aren't rendered
				ex: Username is hidden for broadcasts, rank icon is hidden for public chat */
			Widget[] chatWidgets = chatbox.getDynamicChildren().clone();
			int removedHeight = 0;

			// Since we only need to check either [2] or [0] of each line for the sender we can just iterate over every other child
			for (int i = 0; i < chatWidgets.length; i += 2)
			{
				Widget widget = chatWidgets[i];
				if (!widget.getText().isBlank())
				{
					for (ChannelNameReplacement channelNameToReplace : replacements)
					{
						String name = channelNameToReplace.getName(channelNameManager);
						String formattedName = "[" + name + "]";
						if (sanitizeUsername(widget.getText()).contains(formattedName))
						{
							boolean hideLine = config.removeGroupIronFromClan() && getSelectedChannel() == ChatChannel.CLAN && channelNameToReplace == ChannelNameReplacement.GROUP_IRON;

							int removedWidth = getTextLength(formattedName);

							String newText = widget.getText()
								.replace('\u00A0', ' ')
								// Account for color tags when removing name
								.replaceFirst("\\[.*" + name + ".*]", "");

							// Remove trailing spaces - probably only happens with timestamps turned on
							if (newText.endsWith(" ") || newText.endsWith("\u00A0"))
							{
								newText = newText.substring(0, newText.length() - 1);
								removedWidth += 1;
							}

							// Remove double spaces - mainly found in friends chat since it has sender + username
							if (newText.contains("  ") || newText.contains("\u00A0\u00A0"))
							{
								newText = newText.replaceFirst(" {2}|\u00A0{2}", " ");
								removedWidth += 1;
							}

							// TODO: Remove log
							log.debug("Replaced Text {} with {}", widget.getText(), newText);
							widget.setText(newText);

							if (hideLine)
							{
								widget.setHidden(true);
							}

							if (removedWidth == -1)
							{
								log.debug("Couldn't get text length for text: {}", widget.getText());
								continue;
							}

							widget.setOriginalY(widget.getOriginalY() + removedHeight); // Down
							widget.setOriginalWidth(widget.getOriginalWidth() - removedWidth);
							widget.revalidate();

							int iconWidgetIndex = i + 1;
							int textWidgetIndex = i - 1;
							int nameWidgetIndex = i - 2;

							// Friends chat & gim broadcast widget ordering is different
							if (i % 4 == 0)
							{
								textWidgetIndex = i + 1;
								// Empty widget
								nameWidgetIndex = i + 2;
								// Empty widget in case of GIM broadcasts
								iconWidgetIndex = i + 3;
							} // else i % 4 == 2

							processWidget(iconWidgetIndex, chatWidgets, removedWidth, removedHeight, hideLine);
							processWidget(textWidgetIndex, chatWidgets, removedWidth, removedHeight, hideLine);
							processWidget(nameWidgetIndex, chatWidgets, removedWidth, removedHeight, hideLine);

							if (hideLine)
							{
								// Height of 1 line
								removedHeight += 14;
							}

							// break name replacement loop, not line loop
							break;
						}
					}
				}
			}
		}
	}

	private void processWidget(int index, Widget[] chatWidgets, int removedWidth, int removedHeight, boolean hideLine)
	{
		if (index >= 0 && index < chatWidgets.length)
		{
			Widget widget = chatWidgets[index];
			widget.setOriginalX(widget.getOriginalX() - removedWidth); // Left
			widget.setOriginalY(widget.getOriginalY() + removedHeight); // Down
			if (hideLine)
			{
				widget.setHidden(true);
			}
			widget.revalidate();
		}
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
		getBlockedMessageTypes(config).stream()
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

}
