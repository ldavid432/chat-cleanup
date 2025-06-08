package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import static com.github.ldavid432.cleanchat.CleanChatUtil.CLAN_INSTRUCTION_MESSAGE;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import net.runelite.client.util.Text;

@AllArgsConstructor
public enum ChatBlock
{
	CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeClanInstruction,
		CLAN_INSTRUCTION_MESSAGE
	),
	GUEST_CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeGuestClanInstruction,
		channel -> channel.getGuestClanName().stream().map(
			name -> "You are now a guest of " + name + ".To talk, start each line of chat with /// or /gc"
		)
	),
	GUEST_CLAN_RECONNECTING(
		CleanChatChannelsConfig::removeGuestClanReconnecting,
		"Attempting to reconnect to guest channel automatically..."
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
		channelNameManager -> channelNameManager.getFriendsChatName().stream().map(name -> "Now talking in chat-channel " + name)
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

	public boolean appliesTo(CleanChatChannelsConfig config, String message, ChannelNameManager channelNameManager)
	{
		return isEnabled(config) && getMessage.apply(channelNameManager).anyMatch(blockedMessage -> Text.removeTags(message).contains(blockedMessage));
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, Stream<String>> getMessage;

	ChatBlock(Function<CleanChatChannelsConfig, Boolean> isEnabled, String message) {
		this.isEnabled = isEnabled;
		this.getMessage = s -> Stream.of(message);
	}

	public static boolean anyEnabled(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values()).anyMatch(block -> block.isEnabled(config));
	}
}
