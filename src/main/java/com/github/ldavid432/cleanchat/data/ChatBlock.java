package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import com.github.ldavid432.cleanchat.CleanChatChannelsPlugin;
import java.util.Arrays;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import net.runelite.client.util.Text;

@AllArgsConstructor
public enum ChatBlock
{
	CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeClanInstruction,
		"To talk in your clan's channel, start each line of chat with // or /c."
	),
	// TODO: Confirm the newline/spacing
	GUEST_CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeGuestClanInstruction,
		channel -> "You are now a guest of " + channel.getGuestClanName() + ".\nTo talk, start each line of chat with /// or /gc"
	),
	GROUP_IRON_INSTRUCTION(
		CleanChatChannelsConfig::removeGroupIronInstruction,
		"To talk in your Ironman Group's channel, start each line of chat with //// or /g."
	),
	FRIENDS_CHAT_INSTRUCTION(
		CleanChatChannelsConfig::removeFriendsChatStartup,
		"To talk, start each line of chat with the / symbol."
	),
	FRIENDS_CHAT_ATTEMPTING(
		CleanChatChannelsConfig::removeFriendsAttempting,
		"Attempting to join chat-channel..."
	),
	FRIENDS_CHAT_NOW_TALKING(
		CleanChatChannelsConfig::removeFriendsNowTalking,
		channelNameManager -> "Now talking in chat-channel " + channelNameManager.getFriendsChatName()
	),
	WELCOME(
		CleanChatChannelsConfig::removeWelcome,
		"Welcome to Old School RuneScape."
	),
	;

	public boolean isEnabled(CleanChatChannelsConfig config)
	{
		return isEnabled.apply(config);
	}

	public boolean appliesTo(CleanChatChannelsPlugin plugin, String message, ChannelNameManager channelNameManager)
	{
		return isEnabled(plugin.getConfig()) && Text.removeTags(message).equals(getMessage.apply(channelNameManager));
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, String> getMessage;

	ChatBlock(Function<CleanChatChannelsConfig, Boolean> isEnabled, String message) {
		this.isEnabled = isEnabled;
		this.getMessage = s -> message;
	}

	public static boolean anyEnabled(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values()).anyMatch(block -> block.isEnabled(config));
	}
}
