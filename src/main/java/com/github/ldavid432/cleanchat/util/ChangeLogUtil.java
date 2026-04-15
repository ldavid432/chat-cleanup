package com.github.ldavid432.cleanchat.util;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.awt.Color;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.ColorUtil;

public class ChangeLogUtil
{
	public static void handleChangelog(CleanChatChannelsConfig config, ChatMessageManager chatMessageManager, Client client)
	{
		if (config.getLastSeenVersion() >= CleanChatChannelsConfig.CURRENT_VERSION)
		{
			return;
		}

		StringBuilder builder = new StringBuilder();

		// Since last seen version wasn't in 1.0 checking for only it will trigger for everyone who installs the plugin.
		//  By only triggering this during startup while not logged in we can "better" attempt to determine if this is a previous install or not.
		//  Still not totally accurate but better than nothing.

		// 2.8.0
		if (config.getLastSeenVersion() < 3)
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				// Existing install (theoretically)
				builder
					.append(changelogLine("Color bar: Configurable bar that appears on each chat message, colored based on message type"))
					.append(changelogLine("Fixed-Width timestamps: Make the timestamps from the chat timestamp plugin all the same width. " +
						"Making your messages all start at the same distance!"));
			}
		}

		if (builder.length() != 0) {
			builder.insert(0, changelogLine("Clean chat has been updated!", true, false));
			int lastNewlineIndex = builder.lastIndexOf("<br>");
			builder.replace(lastNewlineIndex, lastNewlineIndex + 4, "");

			chatMessageManager.queue(
				QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(builder.toString())
					.build()
			);
		}

		config.setLstSeenVersion(CleanChatChannelsConfig.CURRENT_VERSION);
	}

	private static String changelogLine(String text)
	{
		return changelogLine(text, true);
	}

	private static String changelogLine(String text, boolean showNewline)
	{
		return changelogLine(text, showNewline, true);
	}

	private static String changelogLine(String text, boolean showNewline, boolean showBullet)
	{
		return ColorUtil.wrapWithColorTag((showBullet ? "* " : "") + text + (showNewline ? "<br>" : ""), Color.RED);
	}
}
