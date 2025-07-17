package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.CLAN_INSTRUCTION_MESSAGE;
import static com.github.ldavid432.cleanchat.CleanChatUtil.SCRIPT_REBUILD_CHATBOX;
import static com.github.ldavid432.cleanchat.CleanChatUtil.SCRIPT_SCROLLBAR_MAX;
import static com.github.ldavid432.cleanchat.CleanChatUtil.SCRIPT_SCROLLBAR_MIN;
import static com.github.ldavid432.cleanchat.CleanChatUtil.VARC_INT_CHAT_TAB;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLineCount;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeName;
import com.github.ldavid432.cleanchat.data.ChannelNameRemoval;
import com.github.ldavid432.cleanchat.data.ChatTab;
import com.github.ldavid432.cleanchat.data.IndentMode;
import static java.lang.Math.max;
import static java.lang.Math.min;
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
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import static net.runelite.api.widgets.WidgetPositionMode.ABSOLUTE_CENTER;
import static net.runelite.api.widgets.WidgetPositionMode.ABSOLUTE_TOP;
import static net.runelite.api.widgets.WidgetSizeMode.ABSOLUTE;
import static net.runelite.api.widgets.WidgetSizeMode.MINUS;
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

		private final Widget clickBox;

		// TODO: Maybe group channelType, channelWidth, prefixWidth and nameWidth into an additional data class
		private ChannelNameRemoval channelType = null;

		private int removedWidth = 0;
		// Width of the channel name (if present), ignores timestamp
		private int channelWidth = 0;
		// Width of the timestamp
		private int prefixWidth = 0;
		// width of the username + rank
		private int nameWidth = 0;

		public void onAllWidgets(Consumer<Widget> action)
		{
			Stream.of(channel, rank, name, message).forEach(action);
		}

		public void onNonChannelWidgets(Consumer<Widget> action)
		{
			Stream.of(rank, name, message).forEach(action);
		}
	}

	private int lastScrollHeight = -1;
	private int lastScrollY = -1;
	private int lastChatTab = ChatTab.CLOSED.getValue();
	private boolean chatboxScrolled = false;

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() >= SCRIPT_SCROLLBAR_MIN && event.getScriptId() <= SCRIPT_SCROLLBAR_MAX)
		{
			Object[] args = event.getScriptEvent().getArguments();
			chatboxScrolled = args.length >= 2 && (int) args[1] == InterfaceID.Chatbox.CHATSCROLLBAR;
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
			if (event.getScriptId() >= SCRIPT_SCROLLBAR_MIN && event.getScriptId() <= SCRIPT_SCROLLBAR_MAX && chatboxScrolled)
			{
				chatboxScrolled = false;

				Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
				if (chatbox != null)
				{
					lastScrollHeight = chatbox.getScrollHeight();
					lastScrollY = chatbox.getScrollY();
				}
			}

			return;
		}

		if (!ChannelNameRemoval.anyEnabled(config) && !config.removeGroupIronFromClan() && config.indentationMode() == IndentMode.MESSAGE)
		{
			return;
		}

		checkReplacements();
	}

	public void checkReplacements()
	{
		// FriendsChatManager is null at the first FriendsChatChanged after login so we have to add this check later
		channelNameManager.updateFriendsChatName();

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(VARC_INT_CHAT_TAB));

		if (chatbox != null && selectedChatTab != ChatTab.CLOSED)
		{
			Widget[] chatWidgets = chatbox.getDynamicChildren().clone();
			Widget[] clickboxWidgets = chatbox.getStaticChildren().clone();

			List<ChatWidgetGroup> removedChats = new ArrayList<>();

			// TODO: Make i = 0
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

					return new ChatWidgetGroup(channelWidget, chatWidgets[rankWidgetIndex], chatWidgets[nameWidgetIndex], chatWidgets[messageWidgetIndex], clickboxWidgets[(i - 2) / 4]);
				})
				.filter(group -> {
					// TODO: Move chat blocking in here
					boolean blockChat = false;

					if (!group.getChannel().getText().isEmpty())
					{
						// If the text is not blank we *should* be guaranteed a match
						for (ChannelNameRemoval channelRemoval : ChannelNameRemoval.values())
						{
							String widgetChannelName = sanitizeName(group.getChannel().getText());
							String matchedChannelName = channelRemoval.getNames(channelNameManager).stream()
								.map(CleanChatUtil::sanitizeName)
								.filter(channel -> widgetChannelName.contains("[" + channel + "]"))
								.findFirst()
								.orElse(null);

							if (matchedChannelName != null)
							{
								group.setChannelType(channelRemoval);

								blockChat = checkGroupIronInClan(selectedChatTab, channelRemoval);

								if (!blockChat && channelRemoval.isEnabled(config))
								{
									// Update widget text and removedWidth
									group.setRemovedWidth(getTextLength("[" + matchedChannelName + "]") + updateChannelText(matchedChannelName, group.getChannel()));

									setIndentWidths(group, widgetChannelName, matchedChannelName);
									break;
								}
								else if (!blockChat && !channelRemoval.isEnabled(config))
								{
									setIndentWidths(group, widgetChannelName, matchedChannelName);
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

			if (!removedChats.isEmpty())
			{
				log.debug("Hid {} chat messages", removedChats.size());
			}

			Collections.reverse(displayedChats);

			// Update widgets width, height & text
			updateWidgets(displayedChats);

			// Calculate this after editing messages
			int totalHeight = displayedChats.stream()
				.map(group -> group.getMessage().getHeight())
				.reduce(0, Integer::sum);

			// If we only have a few messages we want to place them at the bottom (chatbox.getHeight()) instead of the top (0).
			//  If placing from the bottom, add padding first
			int y = totalHeight >= chatbox.getHeight() ? 0 : chatbox.getHeight() - totalHeight - 2;

			// Place widgets vertically
			for (ChatWidgetGroup group : displayedChats)
			{
				int widgetY = y;
				group.onAllWidgets(widget -> {
					widget.setOriginalY(widgetY);
					widget.revalidate();
				});

				group.getClickBox().setOriginalY(widgetY);
				group.getClickBox().revalidate();

				y += group.getMessage().getHeight();
			}

			// If placing at the top, add padding last
			if (totalHeight >= chatbox.getHeight())
			{
				y += 2;
			}

			y = max(y, chatbox.getHeight());

			for (ChatWidgetGroup group : removedChats)
			{
				group.onAllWidgets(widget -> {
					widget.setHidden(true);
					widget.setOriginalY(0);
				});

				group.getClickBox().setHidden(true);
				group.getClickBox().setOriginalY(0);
			}

			chatbox.setScrollHeight(y);
			chatbox.revalidateScroll();

			// Replacing this script with Java allows us to avoid a clientThread.invokeLater as well as adjust the logic since we are modifying the true scroll height
			scrollbar_resize(chatbox, selectedChatTab);

			// Store this since rebuildchatbox changes it before we can
			lastScrollHeight = chatbox.getScrollHeight();
			lastScrollY = chatbox.getScrollY();
		}
		lastChatTab = selectedChatTab.getValue();
	}

	private void setIndentWidths(ChatWidgetGroup group, String widgetChannelText, String matchedChannelName)
	{
		int startOfChannel = widgetChannelText.indexOf("[" + matchedChannelName + "]");
		int endOfChannel = startOfChannel + matchedChannelName.length() + 2;

		String prefix = widgetChannelText.substring(0, startOfChannel);
		int prefixWidth = getTextLength(prefix);
		group.setPrefixWidth(prefixWidth);

		String channel = widgetChannelText.substring(startOfChannel, endOfChannel);
		int channelWidth = getTextLength(channel);
		group.setChannelWidth(channelWidth);

		// FC puts name + channel into the channel widget
		if (group.getChannelType() == ChannelNameRemoval.FRIENDS_CHAT)
		{
			// TODO: Can we switch back to getTextLength here?
			// For some reason the fc channel width is the entire length of the chatbox so we can't use getWidth
			int prefixChanelNameWidth = group.getMessage().getOriginalX() - group.getChannel().getOriginalX();
			group.setNameWidth(prefixChanelNameWidth - prefixWidth - channelWidth);
		}
		else
		{
			group.setNameWidth(group.getName().getWidth());
		}

		if (!group.getRank().isHidden())
		{
			group.setNameWidth(group.getNameWidth() + group.getRank().getWidth());
		}
	}

	// Script 72
	private void scrollbar_resize(Widget scrollArea, ChatTab selectedChatTab)
	{
		Widget scrollBarContainer = client.getWidget(InterfaceID.Chatbox.CHATSCROLLBAR);

		int scrollAreaHeight = scrollArea.getScrollHeight();
		if (scrollAreaHeight <= 0)
		{
			scrollAreaHeight = scrollArea.getHeight();
		}

		int scrollBarHeight;
		if (scrollAreaHeight > 0)
		{
			scrollBarHeight = (scrollBarContainer.getHeight() - 32) * scrollArea.getHeight() / scrollAreaHeight;
		}
		else
		{
			scrollBarHeight = scrollBarContainer.getHeight() - 32;
		}
		if (scrollBarHeight < 10)
		{
			scrollBarHeight = 10;
		}

		Widget scrollBar = scrollBarContainer.getChild(1);
		if (scrollBar != null)
		{
			scrollBar.setSize(0, scrollBarHeight, MINUS, ABSOLUTE);
			scrollBar.revalidate();

			scrollbar_vertical_doscroll(scrollBarContainer, scrollArea, scrollBar, selectedChatTab);

			scrollBarContainer.revalidateScroll();
			scrollArea.revalidateScroll();
		}
	}

	// Script 37
	private void scrollbar_vertical_doscroll(Widget scrollBarContainer, Widget scrollArea,
											 Widget scrollBar, ChatTab selectedChatTab)
	{
		int int2;
		if (lastChatTab != selectedChatTab.getValue() && lastChatTab != ChatTab.CLOSED.getValue())
		{
			// Custom logic to store our scroll values since rebuildchatbox will override them
			int2 = max(scrollArea.getScrollHeight() - (lastScrollHeight - lastScrollY), 0);
		}
		else
		{
			int2 = scrollArea.getScrollY();
			int int3 = max(scrollArea.getScrollHeight() - scrollArea.getHeight(), 1);
			int2 = max(min(int2, int3), 0);
		}
		scrollArea.setScrollY(int2);
		scrollArea.revalidateScroll();
		client.setVarcIntValue(7, scrollArea.getScrollY());

		scrollbar_vertical_setdragger(scrollBarContainer, scrollArea, scrollBar);
	}

	// Script 740
	private void scrollbar_vertical_setdragger(Widget scrollBarContainer, Widget scrollArea, Widget scrollBar)
	{
		int int2 = max(scrollArea.getScrollHeight() - scrollArea.getHeight(), 1);
		int int3 = scrollBarContainer.getHeight() - 32 - scrollBar.getHeight();

		int scrollBarPos = max((16 + int3) * scrollArea.getScrollY() / int2, 16);
		scrollBar.setPos(0, scrollBarPos, ABSOLUTE_CENTER, ABSOLUTE_TOP);
		scrollBar.revalidate();

		Widget scrollbarElementTop = scrollBarContainer.getChild(2);
		if (scrollbarElementTop != null)
		{
			scrollbarElementTop.setPos(0, scrollBar.getOriginalY(), ABSOLUTE_CENTER, ABSOLUTE_TOP);
			scrollbarElementTop.revalidate();
		}

		Widget scrollbarElementBottom = scrollBarContainer.getChild(3);
		if (scrollbarElementBottom != null)
		{
			scrollbarElementBottom.setPos(0, scrollBar.getOriginalY() + scrollBar.getHeight() - 5, ABSOLUTE_CENTER, ABSOLUTE_TOP);
			scrollbarElementBottom.revalidate();
		}
	}

	private void updateWidgets(List<ChatWidgetGroup> groups)
	{
		for (ChatWidgetGroup group : groups)
		{
			// Shift widgets X left if channel was removed
			group.onNonChannelWidgets(widget -> {
				widget.setOriginalX(widget.getOriginalX() - group.getRemovedWidth());
				widget.revalidate();
			});

			// Expand the width of messages if channel was removed
			group.getMessage().setOriginalWidth(group.getMessage().getWidth() + group.getRemovedWidth());
			group.getMessage().revalidate();

			// Reduce channel width if it was removed
			group.getChannel().setOriginalWidth(group.getChannel().getOriginalWidth() - group.getRemovedWidth());
			group.getChannel().revalidate();

			// Newline indentation handling

			int indentWidth = 0;
			// TODO: See if there's something that we are missing when measuring so we can avoid adding all these hardcoded offsets
			// Don't need to mess with indentation on messages we don't edit
			// TODO: Potentially handle other message types indent?
			if (group.getChannelType() != null)
			{
				switch (config.indentationMode())
				{
					// Intentionally fallthrough
					case START:
						indentWidth += group.getPrefixWidth();

						if (group.getChannelType().isEnabled(config))
						{
							if (group.getChannelType() == ChannelNameRemoval.FRIENDS_CHAT)
							{
								indentWidth += 1;
							}
							else
							{
								indentWidth -= 2;
							}

						}
					case CHANNEL:
						if (!group.getChannelType().isEnabled(config))
						{
							indentWidth += group.getChannelWidth();
							if (group.getChannelType() != ChannelNameRemoval.FRIENDS_CHAT)
							{
								indentWidth += 1;
							}
							else
							{
								indentWidth += 4;
							}

						}
					case NAME:
						indentWidth += group.getNameWidth();
						if (indentWidth > 0 && group.getChannelType() != ChannelNameRemoval.FRIENDS_CHAT)
						{
							indentWidth += 4;
						}
						else if (group.getChannelType() == ChannelNameRemoval.FRIENDS_CHAT)
						{
							indentWidth -= 4;
						}
					case MESSAGE:
						// Already set by default
				}
			}

			if (indentWidth > 0)
			{
				int numSpaces = max(0, indentWidth / 3);

				// Using spaces to keep the first line at the initial position (+/-2 pixels)
				group.getMessage().setText(" ".repeat(numSpaces) + group.getMessage().getText());
				group.getMessage().setOriginalX(group.getMessage().getOriginalX() - indentWidth);
				group.getMessage().setOriginalWidth(group.getMessage().getOriginalWidth() + indentWidth);
				group.getMessage().revalidate();
			}

			// Adjust the height of messages now that they have been shifted/indented
			if (!group.getMessage().getText().isEmpty() && group.getMessage().getWidth() > 0)
			{
				int numLines = getTextLineCount(group.getMessage().getText(), group.getMessage().getWidth());
				int height = numLines * 14; // Height of each line is always 14
				group.getMessage().setOriginalHeight(height);
				group.getMessage().revalidate();

				group.getClickBox().setOriginalHeight(height);
				group.getClickBox().revalidate();
			}
		}
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
			// Account for color tags when removing name
			// TODO: Target the channel name more precisely, this should do for now to avoid targeting timestamps in brackets
			.replaceFirst("\\[[^]\\[]*" + channelName + ".*]", "");

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
