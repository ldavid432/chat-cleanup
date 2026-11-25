package com.github.ldavid432.cleanchat.util;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.awt.Color;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.ColorUtil;

public class ChangeLogUtil
{
	private static final String LAST_SEEN = "lastSeenVersion";

	public static void handleChangelog(ConfigManager configManager, ChatMessageManager chatMessageManager, Client client)
	{
		Integer lastSeen = configManager.getConfiguration(CleanChatChannelsConfig.GROUP, LAST_SEEN, Integer.class);
		if (lastSeen == null || lastSeen >= CleanChatChannelsConfig.CURRENT_VERSION)
		{
			configManager.setConfiguration(CleanChatChannelsConfig.GROUP, LAST_SEEN, CleanChatChannelsConfig.CURRENT_VERSION);
			return;
		}

		StringBuilder builder = new StringBuilder();

		// 2.8.0
		if (lastSeen < 3)
		{
			builder
				.append(changelogLine("Color bar: Configurable bar that appears on each chat message, colored based on message type"))
				.append(changelogLine("Fixed-Width timestamps: Make the timestamps from the chat timestamp plugin all the same width. " +
					"Making your messages all start at the same distance!"));
		}

		// 2.9.0
		if (lastSeen < 4)
		{
			builder
				.append(changelogLine("Added option to hide the separator above the text input"))
				.append(changelogLine("As well as hide the top separator (in transparent mode only)"));
		}

		// 2.12.0
		if (lastSeen < 5)
		{
			builder
				.append(changelogLine("Split private chat is now supported!"))
				.append(changelogLine("Added option to hide username icons (i.e ironman helmets)"))
				.append(changelogLine("Added Option to remove 'To' and 'From' in private chats"))
				.append(changelogLine("Indent mode now applies to all chat types, not just chat channels"));
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

		configManager.setConfiguration(CleanChatChannelsConfig.GROUP, LAST_SEEN, CleanChatChannelsConfig.CURRENT_VERSION);
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
