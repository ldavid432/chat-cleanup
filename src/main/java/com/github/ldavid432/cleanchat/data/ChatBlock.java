package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import static com.github.ldavid432.cleanchat.CleanChatUtil.CLAN_INSTRUCTION_MESSAGE;
import java.util.function.Function;
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
		// "You are now a guest of x" is also included in this message, they are separated by a <br>
		"To talk, start each line of chat with /// or /gc"
	),
	GUEST_CLAN_RECONNECTING(
		CleanChatChannelsConfig::removeGuestClanReconnecting,
		"Attempting to reconnect to guest channel automatically..."
	),
	GROUP_IRON_INSTRUCTION(
		CleanChatChannelsConfig::removeGroupIronInstruction,
		"To talk in your Ironman Group's channel"
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
		"Now talking in chat-channel"
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

	public boolean appliesTo(CleanChatChannelsConfig config, String message)
	{
		return isEnabled(config) && Text.removeTags(message).contains(this.message);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final String message;
}
