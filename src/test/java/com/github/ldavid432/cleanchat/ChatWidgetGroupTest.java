package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatChannel;
import java.awt.Color;
import java.util.List;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.ColorUtil;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChatWidgetGroupTest
{

	@Test
	public void testPublicChat()
	{
		assertChatWidgetGroup(
			"[08:49:05] Name:",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			null,
			true,
			null,
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			"[08:49:05] Name:",
			null,
			false,
			null
		);

		assertChatWidgetGroup(
			"[08:49:05] Message",
			null,
			null,
			true,
			null,
			"[08:49:05] Message",
			null,
			null,
			false,
			null
		);
	}

	@Test
	public void testGroupIronChat()
	{
		// GIM chat
		assertChatWidgetGroup(
			"Name:",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			"[08:49:05] [<col=9070ff>Group</col>]",
			true,
			"Group",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			"Name:",
			"[08:49:05] [<col=9070ff>Group</col>]",
			false,
			ChatChannel.GROUP_IRON
		);

		// GIM message
		assertChatWidgetGroup(
			"[08:49:05] [<col=9070ff>Group</col>]",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			null,
			true,
			"Group",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			null,
			"[08:49:05] [<col=9070ff>Group</col>]",
			false,
			ChatChannel.GROUP_IRON
		);
	}

	@Test
	public void testClanChat()
	{
		// chat
		assertChatWidgetGroup(
			"Name:",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			"[08:49:05] [<col=9070ff>Clan</col>]",
			false,
			"Clan",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			"Name:",
			"[08:49:05] [<col=9070ff>Clan</col>]",
			true,
			ChatChannel.CLAN
		);

		// message
		assertChatWidgetGroup(
			null,
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			"[08:49:05] [<col=9070ff>Clan</col>]",
			true,
			"Clan",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			null,
			"[08:49:05] [<col=9070ff>Clan</col>]",
			false,
			ChatChannel.CLAN
		);
	}

	@Test
	public void testFriendsChat()
	{
		// chat
		assertChatWidgetGroup(
			"[08:49:05] [<col=9070ff>FC</col>] Name:",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			null,
			true,
			"FC",
			ColorUtil.wrapWithColorTag("Message", Color.BLUE),
			null,
			"[08:49:05] [<col=9070ff>FC</col>] Name:",
			false,
			ChatChannel.FRIENDS_CHAT
		);

		// notification
		assertChatWidgetGroup(
			"[08:49:05] Message",
			null,
			null,
			true,
			null,
			"[08:49:05] Message",
			null,
			null,
			false,
			null // Cannot even identify channel due to lack of sender widget
		);
	}

	public void assertChatWidgetGroup(
		String actualWidget0Text,
		String actualWidget1Text,
		String actualWidget2Text,
		boolean actualIsWidget3Hidden,
		String actualChannelName,
		String expectedMessageText,
		String expectedNameText,
		String expectedChannelText,
		boolean expectRank,
		ChatChannel chatChannel
	) {
		ChannelNameManager channelNameManager = mock();
		if (actualChannelName != null && chatChannel != null)
		{
			switch (chatChannel)
			{
				case CLAN:
					when(channelNameManager.getClanNames()).thenReturn(List.of(actualChannelName));
					break;
				case GUEST_CLAN:
					when(channelNameManager.getGuestClanNames()).thenReturn(List.of(actualChannelName));
					break;
				case FRIENDS_CHAT:
					when(channelNameManager.getFriendsChatNames()).thenReturn(List.of(actualChannelName));
					break;
				case GROUP_IRON:
					when(channelNameManager.getGroupIronNames()).thenReturn(List.of(actualChannelName));
					break;
			}
		}
		ChatWidgetGroup actual = ChatWidgetGroup.of(
			mockWidget(actualWidget0Text),
			mockWidget(actualWidget1Text),
			mockWidget(actualWidget2Text),
			mockWidget(actualIsWidget3Hidden ? null : ""),
			mock(),
			channelNameManager
		);

		assertEquals(expectedMessageText, actual.message.getText());

		int unusedSize = 0;

		if (expectedNameText != null)
		{
			assertEquals(expectedNameText, actual.name.getText());
		}
		else
		{
			assertNull(actual.name);
			unusedSize++;
		}

		if (expectedChannelText != null)
		{
			assertEquals(expectedChannelText, actual.channel.getText());
		}
		else
		{
			assertNull(actual.channel);
			unusedSize++;
		}

		if (expectRank)
		{
			assertNotNull(actual.rank);
			assertNull(actual.rank.getText());
		}
		else
		{
			assertNull(actual.rank);
			unusedSize++;
		}

		assertEquals(chatChannel, actual.getChannelType());

		for (Widget widget : actual.unused)
		{
			assertNull(widget.getText());
		}
		assertEquals(unusedSize, actual.unused.size());
	}

	private Widget mockWidget(String text)
	{
		Widget widget = mock();
		if (text != null && !text.isBlank())
		{
			when(widget.getText()).thenReturn(text);
		}
		when(widget.isHidden()).thenReturn(text == null);
		when(widget.getOriginalX()).thenReturn(0);
		when(widget.getCanvasLocation()).thenReturn(new Point(0, 0));
		return widget;
	}
}
