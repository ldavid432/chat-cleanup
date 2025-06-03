package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

// TODO: Potentially switch these to block by hiding widgets instead of editing the chat line map
//  Just need to figure out how to determine which message type they are or if we can safely ignore message type
@AllArgsConstructor
public enum ChatBlock
{
	CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeClanInstruction,
		ChatMessageType.CLAN_MESSAGE,
		"To talk in your clan's channel"
	),
	GUEST_CLAN_INSTRUCTION(
		CleanChatChannelsConfig::removeGuestClanInstruction,
		ChatMessageType.CLAN_MESSAGE,
		"To talk, start each line of chat with /// or /gc"
	),
	GROUP_IRON_INSTRUCTION(
		CleanChatChannelsConfig::removeGroupIronInstruction,
		ChatMessageType.CLAN_GIM_MESSAGE,
		"To talk in your Ironman Group's channel"
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
		"Now talking in chat-channel"
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

	public boolean appliesTo(CleanChatChannelsConfig config, ChatMessage event)
	{
		return isEnabled(config) && getFromChatMessageTypes().contains(event.getType()) && event.getMessage().contains(this.message);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	@Getter
	private final ChatMessageType chatMessageType;
	@Getter
	private final String message;

	public List<ChatMessageType> getFromChatMessageTypes()
	{
		return List.of(chatMessageType);
	}

	public static List<ChatMessageType> getBlockedMessageTypes(CleanChatChannelsConfig config)
	{
		return Arrays.stream(ChatBlock.values())
			.filter(chatBlock -> chatBlock.isEnabled(config))
			.map(ChatBlock::getChatMessageType).distinct()
			.collect(Collectors.toList());
	}
}
