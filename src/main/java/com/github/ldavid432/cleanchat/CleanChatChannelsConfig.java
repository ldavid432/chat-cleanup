package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.GROUP;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(GROUP)
public interface CleanChatChannelsConfig extends Config
{
	String GROUP = "cleanchat";

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

	@ConfigSection(
		name = "Clan Chat",
		description = "Configure clan chat",
		position = 1
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
		keyName = "removeGuestClanInstruction",
		name = "Remove guest startup message",
		description = "Remove message telling you how to chat in your guest clan channel",
		section = clanSection,
		position = 2
	)
	default boolean removeGuestClanInstruction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "removeGuestClanName",
		name = "Remove guest clan name",
		description = "Remove guest clan name from guest clan chat messages",
		section = clanSection,
		position = 3
	)
	default boolean removeGuestClanName()
	{
		return false;
	}

	@ConfigSection(
		name = "Group Iron Chat",
		description = "Configure group iron chat",
		position = 2
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
		description = "Remove GIM name from GIM chat messages.<br>" +
			"Enabling this also enables 'Move GIM broadcasts', which removes GIM broadcasts from the clan tab.",
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
		description = "Remove GIM broadcasts from the clan chat tab, only display them in the group tab.<br>" +
			"This is always enabled if 'Remove GIM name' is enabled.",
		section = ironSection,
		position = 2
	)
	default boolean removeGroupIronFromClan()
	{
		return true;
	}

	@ConfigSection(
		name = "Friends Chat",
		description = "Configure friends chat",
		position = 3
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
}
