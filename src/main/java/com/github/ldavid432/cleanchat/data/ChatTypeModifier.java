package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.util.List;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

public interface ChatTypeModifier
{
	List<ChatMessageType> getFromChatMessageTypes();

	boolean isEnabled(CleanChatChannelsConfig config);

	default boolean appliesTo(CleanChatChannelsConfig config, ChatMessage event) {
		return isEnabled(config) && getFromChatMessageTypes().contains(event.getType());
	}
}
