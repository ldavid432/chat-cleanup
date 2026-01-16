package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.GROUP;
import static com.github.ldavid432.cleanchat.CleanChatUtil.CURRENT_CLAN_REPLACER;
import static com.github.ldavid432.cleanchat.CleanChatUtil.wrapWithBrackets;
import com.github.ldavid432.cleanchat.data.IndentMode;
import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import static net.runelite.client.util.ColorUtil.wrapWithColorTag;

@ConfigGroup(GROUP)
public interface CleanChatChannelsConfig extends Config
{
	String GROUP = "cleanchat";
	String HIDE_SCROLLBAR_KEY = "hideScrollbar";
	int CURRENT_VERSION = 2;
	String DEFAULT_CUSTOM_CHANNEL_NAME = wrapWithBrackets(wrapWithColorTag(CURRENT_CLAN_REPLACER, Color.BLUE));

	@ConfigItem(
		keyName = "lastSeenVersion",
		name = "",
		description = "",
		hidden = true
	)
	default int getLastSeenVersion()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "lastSeenVersion",
		name = "",
		description = ""
	)
	void setLstSeenVersion(int lastSeenVersion);

	@ConfigItem(
		keyName = "removeWelcome",
		name = "Remove welcome message",
		description = "Remove 'Welcome to RuneScape' message",
		position = 0
	)
	default boolean removeWelcome()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lineBreakIndentationMode",
		name = "Indent Mode",
		description = "Adjust the starting indent for the 2nd (and further) lines in multi-line messages (only in chat channels)<br>" +
			"Message - Default, start directly under the first line<br>" +
			"Name - Start under the username<br>" +
			"Channel - Start under the channel name and after the timestamp. If channel removal is on, acts like Name<br>" +
			"Start - Start at the start of the chat box<br>" +
			"Note: Turning this on may make multi-line messages shift one or two pixels to the right",
		position = 1
	)
	default IndentMode indentationMode()
	{
		return IndentMode.MESSAGE;
	}

	@ConfigItem(
		keyName = HIDE_SCROLLBAR_KEY,
		name = "Hide Scrollbar",
		description = "Hide the chatbox scrollbar. You can still scroll with the mouse wheel like normal.",
		position = 2
	)
	default boolean hideScrollbar()
	{
		return false;
	}

	@ConfigSection(
		name = "Clan Chat",
		description = "Configure clan chat",
		position = 10
	)
	String clanSection = "clanSection";

	@ConfigItem(
		keyName = "removeClanInstruction",
		name = "Remove startup message",
		description = "Remove message telling you how to chat in your clan channel",
		section = clanSection,
		position = 0
	)
	default boolean removeClanInstruction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "removeClanName",
		name = "Remove clan name",
		description = "Remove clan name from clan chat messages",
		section = clanSection,
		position = 1
	)
	default boolean removeClanName()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shortClanName",
		name = "Custom clan name",
		description = "Replace your clan name with a custom one in clan chat messages.<br>" +
			"Leave blank (or the default value) to disable<br>" +
			"You can reference your current clan name using $$<br>" +
			"See the plugin details page for more info (right-click plugin name and click support)",
		section = clanSection,
		position = 2
	)
	default String getShortClanName()
	{
		return DEFAULT_CUSTOM_CHANNEL_NAME;
	}

	@ConfigSection(
		name = "Guest Clan Chat",
		description = "Configure guest clan chat",
		position = 15
	)
	String guestClanSection = "guestClanSection";

	@ConfigItem(
		keyName = "removeGuestClanInstruction",
		name = "Remove startup message",
		description = "Remove message telling you how to chat in your guest clan channel",
		section = guestClanSection,
		position = 0
	)
	default boolean removeGuestClanInstruction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "removeGuestClanReconnecting",
		name = "Remove reconnecting message",
		description = "Remove 'Attempting to reconnect...' message in your guest clan channel",
		section = guestClanSection,
		position = 1
	)
	default boolean removeGuestClanReconnecting()
	{
		return false;
	}

	@ConfigItem(
		keyName = "removeGuestClanName",
		name = "Remove guest clan name",
		description = "Remove guest clan name from guest clan chat messages",
		section = guestClanSection,
		position = 2
	)
	default boolean removeGuestClanName()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shortGuestClanName",
		name = "Custom guest clan name",
		description = "Replace your guest clan name with a custom one in guest clan chat messages. Leave blank to disable" +
			"Leave blank (or the default value) to disable<br>" +
			"You can reference your current clan name using $$<br>" +
			"See the plugin details page for more info (right-click plugin name and click support)",
		section = guestClanSection,
		position = 3
	)
	default String getShortGuestClanName()
	{
		return DEFAULT_CUSTOM_CHANNEL_NAME;
	}

	@ConfigSection(
		name = "Group Iron Chat",
		description = "Configure group iron chat",
		position = 20
	)
	String ironSection = "ironSection";

	@ConfigItem(
		keyName = "removeGroupIronInstruction",
		name = "Remove startup message",
		description = "Remove message telling you how to chat in your GIM channel",
		section = ironSection,
		position = 0
	)
	default boolean removeGroupIronInstruction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "removeGroupIronName",
		name = "Remove GIM name",
		description = "Remove GIM name from GIM chat messages",
		section = ironSection,
		position = 1
	)
	default boolean removeGroupIronName()
	{
		return false;
	}

	@ConfigItem(
		keyName = "moveGroupIronBroadcasts",
		name = "Move GIM broadcasts",
		description = "Remove GIM broadcasts from the clan chat tab, only display them in the group tab",
		section = ironSection,
		position = 2
	)
	default boolean removeGroupIronFromClan()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shortGroupIronName",
		name = "Custom GIM name",
		description = "Replace your GIM name with a custom one in GIM chat messages. Leave blank to disable" +
			"Leave blank (or the default value) to disable<br>" +
			"You can reference your current clan name using $$<br>" +
			"See the plugin details page for more info (right-click plugin name and click support)",
		section = ironSection,
		position = 3
	)
	default String getShortGroupIronName()
	{
		return DEFAULT_CUSTOM_CHANNEL_NAME;
	}

	@ConfigSection(
		name = "Friends Chat",
		description = "Configure friends chat",
		position = 30
	)
	String friendsSection = "friendsSection";

	@ConfigItem(
		keyName = "removeFriendsChatInstruction",
		name = "Remove startup message",
		description = "Remove message telling you how to chat in your friends channel",
		section = friendsSection,
		position = 0
	)
	default boolean removeFriendsChatStartup()
	{
		return true;
	}

	@ConfigItem(
		keyName = "removeFriendsChatName",
		name = "Remove friends chat name",
		description = "Remove friends chat name from friends chat channel messages",
		section = friendsSection,
		position = 1
	)
	default boolean removeFriendsChatName()
	{
		return false;
	}

	@ConfigItem(
		keyName = "removeFriendsAttempting",
		name = "Remove 'attempting to join'",
		description = "Remove 'Attempting to join...' message from friends chat channel",
		section = friendsSection,
		position = 2
	)
	default boolean removeFriendsAttempting()
	{
		return false;
	}

	@ConfigItem(
		keyName = "removeFriendsNowTalking",
		name = "Remove 'now talking in'",
		description = "Remove 'Now talking in chat-channel...' message from friends chat channel",
		section = friendsSection,
		position = 3
	)
	default boolean removeFriendsNowTalking()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shortFriendsName",
		name = "Custom friends chat name",
		description = "Replace your friends chat name with a custom one in friends chat messages. Leave blank to disable" +
			"Leave blank (or the default value) to disable<br>" +
			"You can reference your current clan name using $$<br>" +
			"See the plugin details page for more info (right-click plugin name and click support)",
		section = friendsSection,
		position = 4
	)
	default String getShortFriendsName()
	{
		return DEFAULT_CUSTOM_CHANNEL_NAME;
	}
}
