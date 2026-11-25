package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatChannel;
import com.github.ldavid432.cleanchat.data.ChatTab;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.SCRIPT_REBUILD_CHATBOX;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.SCRIPT_SCROLLBAR_MAX;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.SCRIPT_SCROLLBAR_MIN;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.sanitizeChannelName;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.wrapWithBrackets;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
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

	@Inject
	private TimestampPluginIntegration timestampIntegration;

	private int lastScrollDiff = -1;
	private int lastChatTab = ChatTab.CLOSED.getValue();
	private boolean chatboxScrolled = false;

	@Getter
	public List<ChatWidgetGroup> chatWidgetGroups = List.of();

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
		if (event.getScriptId() >= SCRIPT_SCROLLBAR_MIN && event.getScriptId() <= SCRIPT_SCROLLBAR_MAX && event.getScriptEvent() != null)
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
				client.setVarcIntValue(VarClientID.CHAT_LASTSCROLLPOS, chatbox.getScrollHeight() - lastScrollDiff);
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

			// TODO: See if we can avoid looping through every single widget even if there is no text there
			// for (int i = 0; i < chats.length; i += 4)
			chatWidgetGroups = Stream.iterate(
					0,
					i -> i < chatWidgets.length,
					i -> i + 4
				)
				.filter(i -> !chatWidgets[i].isHidden() || !chatWidgets[i + 1].isHidden() || !chatWidgets[i + 2].isHidden() || !chatWidgets[i + 3].isHidden())
				.map(i -> ChatWidgetGroup.of(
					chatWidgets[i],
					chatWidgets[i + 1],
					chatWidgets[i + 2],
					chatWidgets[i + 3],
					clickboxWidgets[i / 4],
					channelNameManager
				))
				.peek(group -> {
					if (group.getChannelMatch() != null && group.channel != null)
					{
						ChatChannel channel = group.getChannelMatch().getChannel();
						String matchedChannelName = group.getChannelMatch().getText();
						String widgetChannelText = sanitizeChannelName(group.channel.getText());
						String shortName = channel.getShortName(channelNameManager, matchedChannelName);

						if (channel.isChannelNameRemovalEnabled(config))
						{
							group.removeFromChannel(matchedChannelName, client, group.channel);

							matchedChannelName = wrapWithBrackets(matchedChannelName);
						}
						else if (!shortName.isBlank() && !channel.isShortNameDefault(channelNameManager))
						{
							String updatedChannelText = group.replaceChannelName(matchedChannelName, shortName, client, group.channel);

							matchedChannelName = sanitizeChannelName(shortName);
							widgetChannelText = sanitizeChannelName(updatedChannelText);
						}
						else
						{
							matchedChannelName = wrapWithBrackets(matchedChannelName);
						}

						if (channel.isRemoveRankEnabled(config))
						{
							group.removeRank();
						}

						group.calculateChannelWidgetSizes(matchedChannelName, widgetChannelText, client);
					}
					else
					{
						if (config.removePrivateToFrom() && group.name != null && selectedChatTab == ChatTab.PRIVATE)
						{
							group.replaceText("To ", "", "To ", client, group.name, group.message);
							group.replaceText("From ", "", "From ", client, group.name, group.message);
						}

						group.calculateWidgetSizes(client);
					}

					if (!config.hiddenGameModeIcons().isEmpty())
					{
						group.hideGameModeIcons(config.hiddenGameModeIcons(), client);
					}

					group.calculateMessageIndent(config);

					if (timestampIntegration.isFixedWidthTimestampEnabled())
					{
						group.extractTimestamp(timestampIntegration.getTimestampTemplate(), timestampIntegration.getTimestampTemplateWidth(), client);
					}

					group.applyIndent();

					// Calculate height last
					group.calculateHeight(client, 14);
				})
				.collect(Collectors.toList());

			Collections.reverse(chatWidgetGroups);

			// Calculate this after editing messages
			int totalHeight = chatWidgetGroups.stream()
				.map(ChatWidgetGroup::getHeight)
				.reduce(0, Integer::sum);

			// If we only have a few messages we want to place them at the bottom (chatbox.getHeight()) instead of the top (0).
			//  If placing from the bottom, add padding first
			int y = totalHeight >= chatbox.getHeight() ? 0 : chatbox.getHeight() - totalHeight - 2;

			// Place widgets vertically
			for (ChatWidgetGroup group : chatWidgetGroups)
			{
				group.place(y);

				y += group.getHeight();
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
		client.setVarcIntValue(VarClientID.CHAT_LASTSCROLLPOS, scrollArea.getScrollY());

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
