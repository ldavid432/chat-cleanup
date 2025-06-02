package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatTypeModifier;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptEvent;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.util.ColorUtil;
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

	public static String caTag(String caId)
	{
		return "<" + CA_ID_PREFIX + "=" + caId + ">";
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

	public static MenuEntry createCaMenuEntry(Client client, int index, String option, int caId)
	{
		return client.getMenu().createMenuEntry(-1)
			.setOption(option)
			.setTarget(ColorUtil.wrapWithColorTag("Task", Color.WHITE))
			.setType(MenuAction.CC_OP_LOW_PRIORITY)
			.setIdentifier(index)
			.onClick(entry -> client.runScript(7821, index, ScriptEvent.NAME, ChatMessageType.CLAN_MESSAGE.getType(), caId));
	}

	public static MenuEntry[] getCancelEntry(Client client)
	{
		return Arrays.stream(client.getMenu().getMenuEntries()).filter(e -> e.getType() == MenuAction.CANCEL).toArray(MenuEntry[]::new);
	}

	static final String CA_ID_PREFIX = "CA_ID";
	static final Pattern FULL_CA_PATTERN = Pattern.compile(".*" + CA_ID_PREFIX + ":(\\d*)\\|.*");
	static final String PREFIX_CA_PATTERN = CA_ID_PREFIX + ":\\d*\\|";
	static final Pattern TARGET_CA_PATTERN = Pattern.compile(".*<" + CA_ID_PREFIX + "=(\\d*)>.*");

}
