package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import static com.github.ldavid432.cleanchat.CleanChatUtil.CLAN_INSTRUCTION_MESSAGE;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;

@AllArgsConstructor
public enum ChatBlock
{
	CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeClanInstruction,
		ChatMessageType.CLAN_MESSAGE,
		CLAN_INSTRUCTION_MESSAGE
	),
	GUEST_CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeGuestClanInstruction,
		ChatMessageType.CLAN_GUEST_MESSAGE,
		channel -> channel.getGuestClanName().stream().map(
			name -> "You are now a guest of " + name + ".To talk, start each line of chat with /// or /gc"
		)
	),
	GUEST_CLAN_RECONNECTING(
		CleanChatChannelsConfig::removeGuestClanReconnecting,
		ChatMessageType.CLAN_GUEST_MESSAGE,
		"Attempting to reconnect to guest channel automatically..."
	),
	GROUP_IRON_INSTRUCTION(
		CleanChatChannelsConfig::removeGroupIronInstruction,
		ChatMessageType.CLAN_GIM_MESSAGE,
		"To talk in your Ironman Group's channel, start each line of chat with //// or /g."
	),
	FRIENDS_CHAT_INSTRUCTION(
		CleanChatChannelsConfig::removeFriendsChatStartup,
		ChatMessageType.FRIENDSCHATNOTIFICATION,
		"To talk, start each line of chat with the / symbol."
	),
	FRIENDS_CHAT_ATTEMPTING(
		CleanChatChannelsConfig::removeFriendsAttempting,
		ChatMessageType.FRIENDSCHATNOTIFICATION,
		"Attempting to join chat-channel..."
	),
	FRIENDS_CHAT_NOW_TALKING(
		CleanChatChannelsConfig::removeFriendsNowTalking,
		ChatMessageType.FRIENDSCHATNOTIFICATION,
		channelNameManager -> channelNameManager.getFriendsChatName().stream().map(name -> "Now talking in chat-channel " + name)
	),
	WELCOME(
		CleanChatChannelsConfig::removeWelcome,
		ChatMessageType.WELCOME,
		"Welcome to Old School RuneScape."
	),
	;

	public boolean isEnabled(CleanChatChannelsConfig config)
	{
		return isEnabled.apply(config);
	}

	public boolean appliesTo(CleanChatChannelsConfig config, String message, ChatMessageType chatMessageType, ChannelNameManager channelNameManager)
	{
		return isEnabled(config) && chatMessageType == this.chatMessageType && getMessage.apply(channelNameManager).anyMatch(blockedMessage -> Text.removeTags(message).contains(blockedMessage));
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final ChatMessageType chatMessageType;
	private final Function<ChannelNameManager, Stream<String>> getMessage;

	ChatBlock(Function<CleanChatChannelsConfig, Boolean> isEnabled,  ChatMessageType chatMessageType, String message) {
		this.isEnabled = isEnabled;
		this.chatMessageType = chatMessageType;
		this.getMessage = s -> Stream.of(message);
	}

	public static boolean anyEnabled(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values()).anyMatch(block -> block.isEnabled(config));
	}
}
