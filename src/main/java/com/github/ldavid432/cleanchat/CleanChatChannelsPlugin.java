package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.CLAN_INSTRUCTION_MESSAGE;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeUsername;
import com.github.ldavid432.cleanchat.data.ChannelNameReplacement;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import com.github.ldavid432.cleanchat.data.ChatTab;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
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
import net.runelite.client.util.Text;

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

	@Provides
	CleanChatChannelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CleanChatChannelsConfig.class);
	}

	@Data
	private static class ChatWidgetGroup
	{
		private final Widget channel;
		private final Widget rank;
		private final Widget name;
		private final Widget message;

		private int removedWidth = 0;

		public void onAllWidgets(Consumer<Widget> action)
		{
			Stream.of(channel, rank, name, message).forEach(action);
		}

		public void onNonChannelWidgets(Consumer<Widget> action)
		{
			Stream.of(rank, name, message).forEach(action);
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(channelNameManager);
		channelNameManager.startup();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Plugin enabled. Refreshing chat.");
			client.refreshChat();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(channelNameManager);

		// Remove all our shenanigans
		log.debug("Plugin disabled. Refreshing chat.");
		client.refreshChat();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (Objects.equals(event.getGroup(), CleanChatChannelsConfig.GROUP))
		{
			log.debug("Config changed. Refreshing chat.");
			client.refreshChat();
		}
	}

	/*
	Most chats appear in this format as dynamic children on the chatbox scroll area:
		// bottom chat line
		[0] = username
		[1] = chat message
		[2] = timestamp + channel name (timestamp only if that plugin is on, but is pretty common so should definitely account for it)
		[3] = rank icon
		// Next chat line
		[4] = next username
		etc...

	However, some are special:

	Friends chats:
		[0] = channel + username
		[1] = chat message
		[2] = nothing
		[3] = rank icon

	Friends broadcasts (attempting to join... etc):
		[0] = timestamp + message
		[1] = nothing (except the now talking message specifically, has the CLAN chat join message here...)
		[2] = nothing
		[3] = nothing

	GIM broadcasts:
		[0] = channel
		[1] = chat message
		[2] = nothing
		[3] = nothing

	Console message:
		[0] = timestamp + message
		[1] = nothing
		[2] = nothing
		[3] = nothing

	These appear in the children array in this order even if the individual items aren't rendered
		ex: Username is hidden for broadcasts, rank icon is hidden for public chat */
	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// rebuildchatbox script
		if (event.getScriptId() != 84)
		{
			return;
		}

		if (!ChannelNameReplacement.anyEnabled(config) && !ChatBlock.anyEnabled(config))
		{
			return;
		}

		// FriendsChatManager is null at the first FriendsChatChanged after login so we have to add this check later
		channelNameManager.setFriendsChatName();

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(41));

		if (chatbox != null && selectedChatTab != ChatTab.CLOSED)
		{
			Widget[] chatWidgets = chatbox.getDynamicChildren().clone();

			List<ChatWidgetGroup> removedChats = new ArrayList<>();

			// for (int i = 2; i < chats.length; i += 4)
			List<ChatWidgetGroup> displayedChats = Stream.iterate(
					2,
					// If [0] and [2] are empty we've reached the end of the populated chats
					i -> i < chatWidgets.length && !(chatWidgets[i].getText().isEmpty() && chatWidgets[i - 2].getText().isEmpty()),
					i -> i + 4
				)
				.map(i -> {
					int rankWidgetIndex = i + 1; //    [3]
					int messageWidgetIndex = i - 1; // [1]
					int nameWidgetIndex = i - 2; //    [0]

					Widget channelWidget = chatWidgets[i];
					if (channelWidget.getText().isEmpty())
					{
						// Channel is not at [2]. This is either a message with channel at [0] or a message without a channel

						if (!chatWidgets[i - 2].getText().isEmpty())
						{
							// Channel is at [0], this is a special message, adjust indices accordingly

							Widget messageWidget = chatWidgets[i - 1];
							// For some reason the fc now talking message specifically, has the CLAN chat join message here...
							if (messageWidget.getText().isEmpty() || Text.removeTags(messageWidget.getText()).equals(CLAN_INSTRUCTION_MESSAGE))
							{
								// Friends chat message

								messageWidgetIndex = i - 2; // [0]
								// Empty widget in this case, but still good to handle it
								nameWidgetIndex = i - 1; //    [1]
							}
							else
							{
								// Other special message

								channelWidget = chatWidgets[i - 2]; // [0]
								// Empty widget in this case, but still good to handle it
								nameWidgetIndex = i; //                [2]
							}
						}
						else
						{
							// Shouldn't see this unless it's an empty line, which we aren't looking at
						}
					}

					return new ChatWidgetGroup(channelWidget, chatWidgets[rankWidgetIndex], chatWidgets[nameWidgetIndex], chatWidgets[messageWidgetIndex]);
				})
				.filter(group -> {
					String message = group.getMessage().getText();

					boolean blockChat = shouldBlockMessage(message);

					if (!group.getChannel().getText().isEmpty())
					{
						// If the text is not blank we *should* be guaranteed a match
						for (ChannelNameReplacement channelNameToReplace : ChannelNameReplacement.values())
						{
							String widgetChannelName = sanitizeUsername(group.getChannel().getText());
							String matchedChannelName = channelNameToReplace.getNames(channelNameManager).stream()
								.filter(channel -> widgetChannelName.contains("[" + channel + "]"))
								.findFirst()
								.orElse(null);

							if (matchedChannelName != null)
							{
								blockChat = blockChat || checkGroupIronInClan(selectedChatTab, channelNameToReplace);

								if (!blockChat && channelNameToReplace.isEnabled(config))
								{
									// Update widget text and removedWidth
									group.setRemovedWidth(getTextLength("[" + matchedChannelName + "]") + updateChannelText(matchedChannelName, group.getChannel()));
									break;
								}
							}
						}
					}

					if (blockChat)
					{
						removedChats.add(group);
					}

					return !blockChat;
				})
				.collect(Collectors.toList());

			log.debug("Processed {} chat messages", displayedChats.size());

			Collections.reverse(displayedChats);

			int totalHeight = displayedChats.stream()
				.map(it -> it.getMessage().getHeight())
				.reduce(0, Integer::sum);

			// If we only have a few messages we want to place them at the bottom (chatbox.getHeight()) instead of the top (0).
			//  If placing from the bottom, add padding first
			int y = totalHeight >= chatbox.getHeight() ? 0 : chatbox.getHeight() - totalHeight - 2;

			for (ChatWidgetGroup group : displayedChats)
			{
				int widgetY = y;
				group.onNonChannelWidgets(widget -> updateWidget(widget, group.getRemovedWidth(), widgetY));

				group.getChannel().setOriginalY(widgetY);
				group.getChannel().setOriginalWidth(group.getChannel().getOriginalWidth() - group.getRemovedWidth());
				group.getChannel().revalidate();

				y += group.getMessage().getHeight();
			}

			// If placing at the top, add padding last
			if (totalHeight >= chatbox.getHeight()) {
				y += 2;
			}

			for (ChatWidgetGroup group : removedChats)
			{
				group.onAllWidgets(widget -> {
					widget.setHidden(true);
					widget.setOriginalY(0);
				});
			}

			chatbox.setScrollHeight(y);
			chatbox.revalidateScroll();

			clientThread.invokeLater(() -> client.runScript(ScriptID.UPDATE_SCROLLBAR, CleanChatUtil.Chatbox_SCROLLBAR, InterfaceID.Chatbox.SCROLLAREA, chatbox.getScrollY()));
		}
	}

	private void updateWidget(Widget widget, int removedWidth, int y)
	{
		widget.setOriginalX(widget.getOriginalX() - removedWidth); // Shift left
		widget.setOriginalY(y);
		widget.revalidate();
	}

	private boolean shouldBlockMessage(String message)
	{
		if (message == null)
		{
			return false;
		}
		return Stream.of(ChatBlock.values()).anyMatch(block -> block.appliesTo(config, message, channelNameManager));
	}

	private boolean checkGroupIronInClan(ChatTab chatTab, ChannelNameReplacement channelNameToReplace)
	{
		return config.removeGroupIronFromClan() && chatTab == ChatTab.CLAN && channelNameToReplace == ChannelNameReplacement.GROUP_IRON;
	}

	/**
	 * @return Any extra space removed
	 */
	private int updateChannelText(String channelName, Widget channelWidget)
	{
		int removedWidth = 0;

		String newText = channelWidget.getText()
			.replace('\u00A0', ' ')
			// Account for color tags when removing name
			.replaceFirst("\\[.*" + channelName + ".*]", "");

		// Remove trailing spaces - probably only happens with timestamps turned on
		if (newText.endsWith(" ") || newText.endsWith("\u00A0"))
		{
			newText = newText.substring(0, newText.length() - 1);
			removedWidth += getTextLength(" ");
		}

		// Remove double spaces - mainly found in friends chat since it has sender + username
		if (newText.contains("  ") || newText.contains("\u00A0\u00A0"))
		{
			newText = newText.replaceFirst(" {2}|\u00A0{2}", " ");
			removedWidth += getTextLength(" ");
		}

		channelWidget.setText(newText);

		return removedWidth;
	}

}
