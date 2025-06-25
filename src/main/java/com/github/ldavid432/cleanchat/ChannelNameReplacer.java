package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.CLAN_INSTRUCTION_MESSAGE;
import static com.github.ldavid432.cleanchat.CleanChatUtil.SCRIPT_REBUILD_CHATBOX;
import static com.github.ldavid432.cleanchat.CleanChatUtil.VARC_INT_CHAT_TAB;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeName;
import com.github.ldavid432.cleanchat.data.ChannelNameRemoval;
import com.github.ldavid432.cleanchat.data.ChatTab;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Removes channel name from chat widgets
 */
@Slf4j
public class ChannelNameReplacer
{
	@Inject
	private CleanChatChannelsConfig config;

	@Inject
	private ChannelNameManager channelNameManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Client client;

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

	Friends game messages:
		[0] = timestamp + message
		[1] = nothing (except for the 'now talking...' message specifically which has the CLAN chat join message here...)
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
		if (event.getScriptId() != SCRIPT_REBUILD_CHATBOX)
		{
			return;
		}

		if (!ChannelNameRemoval.anyEnabled(config) && !config.removeGroupIronFromClan())
		{
			return;
		}

		checkReplacements();
	}

	public void checkReplacements() {
		// FriendsChatManager is null at the first FriendsChatChanged after login so we have to add this check later
		channelNameManager.updateFriendsChatName();

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(VARC_INT_CHAT_TAB));

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
					// This method can be used to block other kinds of messages, but it causes the chat and scrollbar to
					//  jump around when the chatbox is reloaded so for now we just use it to block group iron broadcasts in the clan chat
					//  and use an alternate method in onChatMessage for the channel startup message blocking

					boolean blockChat = false;

					if (!group.getChannel().getText().isEmpty())
					{
						// If the text is not blank we *should* be guaranteed a match
						for (ChannelNameRemoval channelRemoval : ChannelNameRemoval.values())
						{
							String widgetChannelName = sanitizeName(group.getChannel().getText());
							String matchedChannelName = channelRemoval.getNames(channelNameManager).stream()
								.filter(channel -> widgetChannelName.contains("[" + channel + "]"))
								.findFirst()
								.orElse(null);

							if (matchedChannelName != null)
							{
								blockChat = checkGroupIronInClan(selectedChatTab, channelRemoval);

								if (!blockChat && channelRemoval.isEnabled(config))
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

			if (!removedChats.isEmpty()) {
				log.debug("Hid {} chat messages", removedChats.size());
			}

			Collections.reverse(displayedChats);

			int totalHeight = displayedChats.stream()
				.map(group -> group.getMessage().getHeight())
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
			if (totalHeight >= chatbox.getHeight())
			{
				y += 2;
			}

			for (ChatWidgetGroup group : removedChats)
			{
				group.onAllWidgets(widget -> {
					widget.setHidden(true);
					widget.setOriginalY(0);
				});
			}

			if (!removedChats.isEmpty())
			{
				chatbox.setScrollHeight(y);
				chatbox.revalidateScroll();

				clientThread.invokeLater(() -> client.runScript(ScriptID.UPDATE_SCROLLBAR, InterfaceID.Chatbox.CHATSCROLLBAR, InterfaceID.Chatbox.SCROLLAREA, chatbox.getScrollY()));
			}
		}
	}

	private void updateWidget(Widget widget, int removedWidth, int y)
	{
		widget.setOriginalX(widget.getOriginalX() - removedWidth); // Shift left
		widget.setOriginalY(y);
		widget.revalidate();
	}

	private boolean checkGroupIronInClan(ChatTab chatTab, ChannelNameRemoval channelRemoval)
	{
		return config.removeGroupIronFromClan() && chatTab == ChatTab.CLAN && channelRemoval == ChannelNameRemoval.GROUP_IRON;
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
		if (newText.endsWith(" "))
		{
			newText = newText.substring(0, newText.length() - 1);
			removedWidth += getTextLength(" ");
		}

		// Remove double spaces - mainly found in friends chat since it has sender + username
		if (newText.contains("  "))
		{
			newText = newText.replaceFirst(" {2}", " ");
			removedWidth += getTextLength(" ");
		}

		channelWidget.setText(newText);

		return removedWidth;
	}
}
