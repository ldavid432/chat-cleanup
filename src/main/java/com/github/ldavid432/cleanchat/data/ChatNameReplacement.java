package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ChatMessageType;

@AllArgsConstructor
public enum ChatNameReplacement implements ChatTypeModifier
{
	CLAN(
		CleanChatChannelsConfig::removeClanName,
		List.of(
			ChatMessageType.CLAN_CHAT,
			ChatMessageType.CLAN_MESSAGE
		),
		ChatMessageType.CHALREQ_CLANCHAT
	),
	GUEST_CLAN(
		CleanChatChannelsConfig::removeGuestClanName,
		List.of(
			ChatMessageType.CLAN_GUEST_CHAT,
			ChatMessageType.CLAN_GUEST_MESSAGE
		),
		ChatMessageType.CHALREQ_CLANCHAT
	),

	FRIENDS_CHAT_NAME(
		CleanChatChannelsConfig::removeFriendsChatName,
		ChatMessageType.FRIENDSCHAT,
		ChatMessageType.CHALREQ_FRIENDSCHAT
	),

	IRON_GROUP_NAME(
		CleanChatChannelsConfig::removeGroupIronName, // regular GIM messages already don't go into clan chat so this is not enabled for removeGroupIronFromClan
		ChatMessageType.CLAN_GIM_CHAT,
		ChatMessageType.TRADE
	),
	IRON_GROUP_MESSAGE_NAME(
		(config) -> config.removeGroupIronName() || config.removeGroupIronFromClan(),
		ChatMessageType.CLAN_GIM_MESSAGE,
		ChatMessageType.TRADE
	),
	;

	ChatNameReplacement(
		Function<CleanChatChannelsConfig, Boolean> isEnabled,
		ChatMessageType fromChatMessageType,
		ChatMessageType toChatMessageType
	)
	{
		this.isEnabled = isEnabled;
		this.fromChatMessageTypes = List.of(fromChatMessageType);
		this.toChatMessageType = toChatMessageType;
	}

	public boolean isEnabled(CleanChatChannelsConfig config)
	{
		return isEnabled.apply(config);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	@Getter
	private final List<ChatMessageType> fromChatMessageTypes;
	@Getter
	private final ChatMessageType toChatMessageType;

}
