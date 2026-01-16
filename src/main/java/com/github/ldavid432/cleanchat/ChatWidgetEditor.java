package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.CLAN_INSTRUCTION_MESSAGE;
import static com.github.ldavid432.cleanchat.CleanChatUtil.SCRIPT_REBUILD_CHATBOX;
import static com.github.ldavid432.cleanchat.CleanChatUtil.SCRIPT_SCROLLBAR_MAX;
import static com.github.ldavid432.cleanchat.CleanChatUtil.SCRIPT_SCROLLBAR_MIN;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeName;
import static com.github.ldavid432.cleanchat.CleanChatUtil.wrapWithBrackets;
import static com.github.ldavid432.cleanchat.CleanChatUtil.wrapWithChannelNameRegex;
import com.github.ldavid432.cleanchat.data.ChannelNameRemoval;
import com.github.ldavid432.cleanchat.data.ChatTab;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import static net.runelite.api.widgets.WidgetPositionMode.ABSOLUTE_CENTER;
import static net.runelite.api.widgets.WidgetPositionMode.ABSOLUTE_TOP;
import static net.runelite.api.widgets.WidgetSizeMode.ABSOLUTE;
import static net.runelite.api.widgets.WidgetSizeMode.MINUS;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <ul>
 *     <li>Removes channel name from chat widgets</li>
 *     <li>Indents multi-line messages</li>
 *     <li>Adjusts message height based on name removal and indentation</li>
 * </ul>
 */
@Slf4j
@Singleton
public class ChatWidgetEditor
{
	@Inject
	private CleanChatChannelsConfig config;

	@Inject
	private ChannelNameManager channelNameManager;

	@Inject
	private Client client;

	private int lastScrollDiff = -1;
	private int lastChatTab = ChatTab.CLOSED.getValue();
	private boolean chatboxScrolled = false;

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// Reset scroll
			lastScrollDiff = -1;
			chatboxScrolled = false;
		}
	}

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

	GIM or Clan broadcast after world hopping (before you have reconnected to the channel):
		[0] = nothing
		[1] = message
		[2] = nothing
		[3] = nothing

	Clan instruction message (?) after world hopping:
		[0] = message
		[1] = Did you know? tip
		[2] = nothing
		[3] = nothing

	These appear in the children array in this order even if the individual items aren't rendered
	 ex: Username is hidden for broadcasts, rank icon is hidden for public chat */
	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == SCRIPT_REBUILD_CHATBOX)
		{
			checkReplacements();
		}
		else if (event.getScriptId() >= SCRIPT_SCROLLBAR_MIN && event.getScriptId() <= SCRIPT_SCROLLBAR_MAX && chatboxScrolled)
		{
			chatboxScrolled = false;

			Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
			if (chatbox != null)
			{
				lastScrollDiff = chatbox.getScrollHeight() - chatbox.getScrollY();
				client.setVarcIntValue(7, chatbox.getScrollHeight() - lastScrollDiff);
			}
		}
	}

	public void checkReplacements()
	{
		// FriendsChatManager is null at the first FriendsChatChanged after login so we have to add this check later
		channelNameManager.updateFriendsChatName();

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(VarClientID.CHAT_VIEW));

		if (chatbox != null && selectedChatTab != ChatTab.CLOSED)
		{
			Widget[] chatWidgets = chatbox.getDynamicChildren().clone();
			Widget[] clickboxWidgets = chatbox.getStaticChildren().clone();

			// TODO: Make i = 0
			// TODO: See if we can avoid looping through every single widget even if there is no text there
			// for (int i = 2; i < chats.length; i += 4)
			List<ChatWidgetGroup> displayedChats = Stream.iterate(
					2,
					i -> i < chatWidgets.length,
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
						else if (chatWidgets[messageWidgetIndex].getText().isEmpty())
						{
							return null;
						}
						else
						{
							// Clan/GIM broadcast after world hopping - just use default setup since we mainly care about the message in this case
						}
					}

					return new ChatWidgetGroup(channelWidget, chatWidgets[rankWidgetIndex], chatWidgets[nameWidgetIndex], chatWidgets[messageWidgetIndex], clickboxWidgets[(i - 2) / 4]);
				})
				.filter(Objects::nonNull)
				.peek(group -> {
					if (!group.getChannel().getText().isEmpty())
					{
						Pair<ChannelNameRemoval, String> match = ChannelNameRemoval.findChannelMatch(group.getChannel().getText(), channelNameManager);
						if (match == null) {
							return;
						}

						ChannelNameRemoval channelRemoval = match.getLeft();
						String matchedChannelName = match.getRight();
						String widgetChannelText = sanitizeName(group.getChannel().getText());
						String shortName = channelRemoval.getShortName(channelNameManager);

						group.setChannelType(channelRemoval);

						if (channelRemoval.isEnabled(config))
						{
							group.removeFromChannel(
								wrapWithBrackets(matchedChannelName),
								//language=RegExp
								wrapWithChannelNameRegex(matchedChannelName)
							);

							matchedChannelName = wrapWithBrackets(matchedChannelName);
						}
						else if (shortName != null && !shortName.isBlank() && !channelRemoval.isShortNameDefault(channelNameManager))
						{
							String updatedChannelText = group.replaceChannelName(
								wrapWithBrackets(matchedChannelName),
								//language=RegExp
								wrapWithChannelNameRegex(matchedChannelName),
								shortName
							);

							matchedChannelName = sanitizeName(shortName);
							widgetChannelText = sanitizeName(updatedChannelText);
						}
						else
						{
							matchedChannelName = wrapWithBrackets(matchedChannelName);
						}

						group.indent(config, matchedChannelName, widgetChannelText);
					}

					// Calculate height last
					group.calculateHeight();
				})
				.collect(Collectors.toList());

			Collections.reverse(displayedChats);

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
				group.place(y);

				y += group.getMessage().getHeight();
			}

			// If placing at the top, add padding last
			if (totalHeight >= chatbox.getHeight())
			{
				y += 2;
			}

			y = max(y, chatbox.getHeight());

			chatbox.setScrollHeight(y);
			chatbox.revalidateScroll();

			// Replacing this script with Java allows us to avoid a clientThread.invokeLater as well as adjust the logic since we are modifying the true scroll height
			scrollbar_resize(chatbox);

			// Store this since rebuildchatbox changes the scroll position before we can
			lastScrollDiff = chatbox.getScrollHeight() - chatbox.getScrollY();
		}
		else
		{
			// chat closed - reset scroll
			lastScrollDiff = -1;
			chatboxScrolled = false;
		}
		lastChatTab = selectedChatTab.getValue();
	}

	// Script 72
	private void scrollbar_resize(Widget scrollArea)
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

			scrollbar_vertical_doscroll(scrollBarContainer, scrollArea, scrollBar);

			scrollBarContainer.revalidateScroll();
			scrollArea.revalidateScroll();
		}
	}

	// Script 37
	private void scrollbar_vertical_doscroll(Widget scrollBarContainer, Widget scrollArea, Widget scrollBar)
	{
		int int2;
		if (lastChatTab != ChatTab.CLOSED.getValue() && lastScrollDiff != -1)
		{
			// Custom logic to restore our scroll values since rebuildchatbox will override them
			int2 = max(scrollArea.getScrollHeight() - lastScrollDiff, 0);
		}
		else if (scrollArea.getHeight() < 0)
		{
			// For some reason scrollArea.getHeight is negative after the first login so we just default to 0
			int2 = 0;
			// The scrollHeight is also wrong after first login so just default to the base height
			scrollArea.setScrollHeight(114);
		}
		else
		{
			// Original script logic
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

		int scrollBarPos = max(16 + int3 * scrollArea.getScrollY() / int2, 16);
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

}
