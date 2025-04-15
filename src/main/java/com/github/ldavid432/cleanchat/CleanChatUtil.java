package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatTypeModifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.util.Text;

public class CleanChatUtil
{
	public static String sanitizeUsername(String string)
	{
		return Text.removeTags(string).replace(' ', ' ');
	}

	public static String imageTag(int imageNumber)
	{
		return new ChatMessageBuilder().img(imageNumber).build();
	}

	public static ChatMessageType sanitizeMessageType(ChatMessageType chatMessageType)
	{
		// GIM chats are actually just clan chats - only matters when getting the chat line buffers
		switch (chatMessageType)
		{
			case CLAN_GIM_CHAT:
				return ChatMessageType.CLAN_CHAT;
			case CLAN_GIM_MESSAGE:
				return ChatMessageType.CLAN_MESSAGE;
			default:
				return chatMessageType;
		}
	}

	@Nullable
	public static ChatLineBuffer getChatLineBuffer(Client client, ChatMessageType chatMessageType)
	{
		return client.getChatLineMap().get(sanitizeMessageType(chatMessageType).getType());
	}

	public static List<ChatMessageType> getChatMessageTypes(ChatTypeModifier... modifiers)
	{
		return Arrays.stream(modifiers).flatMap(modifier -> modifier.getFromChatMessageTypes().stream()).distinct().collect(Collectors.toList());
	}

}
