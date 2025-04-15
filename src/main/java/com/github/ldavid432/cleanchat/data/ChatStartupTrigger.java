package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import com.github.ldavid432.cleanchat.CleanChatUtil;
import java.util.List;
import lombok.AllArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

@AllArgsConstructor
public enum ChatStartupTrigger
{
	CLAN(ChatBlock.CLAN_INSTRUCTION, ChatNameReplacement.CLAN),
	GUEST_CLAN(ChatBlock.GUEST_CLAN_INSTRUCTION, ChatNameReplacement.GUEST_CLAN),
	FRIENDS(ChatBlock.FRIENDS_CHAT_INSTRUCTION, ChatNameReplacement.FRIENDS_CHAT_NAME),
	GROUP_IRON(ChatBlock.GROUP_IRON_INSTRUCTION, ChatNameReplacement.IRON_GROUP_NAME, ChatNameReplacement.IRON_GROUP_MESSAGE_NAME),
	;

	ChatStartupTrigger(ChatTypeModifier trigger, ChatTypeModifier... output)
	{
		this(trigger, CleanChatUtil.getChatMessageTypes(output));
	}

	private final ChatTypeModifier trigger;
	private final List<ChatMessageType> outputTypes;

	public List<ChatMessageType> getOutputTypesFor(CleanChatChannelsConfig config, ChatMessage event)
	{
		if (trigger.appliesTo(config, event))
		{
			return outputTypes;
		}
		else
		{
			return List.of();
		}
	}
}
