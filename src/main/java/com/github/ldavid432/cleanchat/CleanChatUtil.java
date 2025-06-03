package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatTypeModifier;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptEvent;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
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

	// Sizes sourced from: https://github.com/JamesShelton140/aqp-finder
	private static final Map<Character, Integer> CHAR_SIZE_MAP = Map.<Character, Integer>ofEntries(
		// Upper case
		Map.entry('A', 6), Map.entry('B', 5), Map.entry('C', 5), Map.entry('D', 5), Map.entry('E', 4), Map.entry('F', 4), Map.entry('G', 6), Map.entry('H', 5), Map.entry('I', 1), Map.entry('J', 5), Map.entry('K', 5), Map.entry('L', 4), Map.entry('M', 7), Map.entry('N', 6), Map.entry('O', 6), Map.entry('P', 5), Map.entry('Q', 6), Map.entry('R', 5), Map.entry('S', 5), Map.entry('T', 3), Map.entry('U', 6), Map.entry('V', 5), Map.entry('W', 7), Map.entry('X', 5), Map.entry('Y', 5), Map.entry('Z', 5),
		// Lower case
		Map.entry('a', 5), Map.entry('b', 5), Map.entry('c', 4), Map.entry('d', 5), Map.entry('e', 5), Map.entry('f', 4), Map.entry('g', 5), Map.entry('h', 5), Map.entry('i', 1), Map.entry('j', 4), Map.entry('k', 4), Map.entry('l', 1), Map.entry('m', 7), Map.entry('n', 5), Map.entry('o', 5), Map.entry('p', 5), Map.entry('q', 5), Map.entry('r', 3), Map.entry('s', 5), Map.entry('t', 3), Map.entry('u', 5), Map.entry('v', 5), Map.entry('w', 5), Map.entry('x', 5), Map.entry('y', 5), Map.entry('z', 5),
		// Numbers
		Map.entry('0', 6), Map.entry('1', 4), Map.entry('2', 6), Map.entry('3', 5), Map.entry('4', 5), Map.entry('5', 5), Map.entry('6', 6), Map.entry('7', 5), Map.entry('8', 6), Map.entry('9', 6),
		// Symbols
		Map.entry(' ', 1), Map.entry(':', 1), Map.entry(';', 2), Map.entry('"', 3), Map.entry('@', 11), Map.entry('!', 1), Map.entry('.', 1), Map.entry('\'', 2), Map.entry(',', 2), Map.entry('(', 2), Map.entry(')', 2), Map.entry('+', 5), Map.entry('-', 4), Map.entry('=', 6), Map.entry('?', 6), Map.entry('*', 7), Map.entry('/', 4), Map.entry('$', 6), Map.entry('£', 8), Map.entry('^', 6), Map.entry('{', 3), Map.entry('}', 3), Map.entry('[', 3), Map.entry(']', 3), Map.entry('&', 9), Map.entry('#', 11), Map.entry('°', 4),
		// NBSP
		Map.entry('\u00A0', 1));

	public static int getTextLength(String text)
	{
		try
		{
			return text.chars()
				.mapToObj(ch -> (char) ch)
				.map(key -> {
					try
					{
						// Fallback to an average size of 5
						return CHAR_SIZE_MAP.containsKey(key) ? CHAR_SIZE_MAP.get(key) + 2 : (5 + 2);
					} catch (NullPointerException e) {
						log.debug("Couldn't get value for {}", key);
						throw e;
					}
				})
				.reduce(0, Integer::sum);
		}
		catch (NullPointerException e)
		{
			return -1;
		}
	}

}
