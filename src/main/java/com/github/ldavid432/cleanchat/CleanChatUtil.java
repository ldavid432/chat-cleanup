package com.github.ldavid432.cleanchat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Text;

@Slf4j
public class CleanChatUtil
{
	public static final String CLAN_INSTRUCTION_MESSAGE = "To talk in your clan's channel, start each line of chat with // or /c.";
	public static final int GUEST_CLAN = -1;
	public static final int SCRIPT_REBUILD_CHATBOX = 84;
	// Scripts 32-36 are all about scrolling in different ways
	public static final int SCRIPT_SCROLLBAR_MIN = 32;
	public static final int SCRIPT_SCROLLBAR_MAX = 36;
	public static final int VARC_INT_CHAT_TAB = 41;
	public static final int MAX_CHANNEL_LIST_SIZE = 128;
	private static final Pattern IMG_TAG_REGEXP = Pattern.compile("<img=[^>]*>");

	public static String sanitizeName(String string)
	{
		return Text.removeTags(string).replace('\u00A0', ' ');
	}

	// Sizes sourced from: https://github.com/JamesShelton140/aqp-finder
	private static final Map<Character, Integer> CHAR_SIZE_MAP = Map.<Character, Integer>ofEntries(
		// Upper case
		Map.entry('A', 6), Map.entry('B', 5), Map.entry('C', 5), Map.entry('D', 5), Map.entry('E', 4), Map.entry('F', 4), Map.entry('G', 6), Map.entry('H', 5), Map.entry('I', 1), Map.entry('J', 5), Map.entry('K', 5), Map.entry('L', 4), Map.entry('M', 7), Map.entry('N', 6), Map.entry('O', 6), Map.entry('P', 5), Map.entry('Q', 6), Map.entry('R', 5), Map.entry('S', 5), Map.entry('T', 3), Map.entry('U', 6), Map.entry('V', 5), Map.entry('W', 7), Map.entry('X', 5), Map.entry('Y', 5), Map.entry('Z', 5),
		// Lower case
		Map.entry('a', 5), Map.entry('b', 5), Map.entry('c', 4), Map.entry('d', 5), Map.entry('e', 5), Map.entry('f', 4), Map.entry('g', 5), Map.entry('h', 5), Map.entry('i', 1), Map.entry('j', 4), Map.entry('k', 4), Map.entry('l', 1), Map.entry('m', 7), Map.entry('n', 5), Map.entry('o', 5), Map.entry('p', 5), Map.entry('q', 5), Map.entry('r', 3), Map.entry('s', 5), Map.entry('t', 3), Map.entry('u', 5), Map.entry('v', 5), Map.entry('w', 5), Map.entry('x', 5), Map.entry('y', 5), Map.entry('z', 5),
		// Numbers
		Map.entry('0', 6), Map.entry('1', 4), Map.entry('2', 6), Map.entry('3', 5), Map.entry('4', 5), Map.entry('5', 5), Map.entry('6', 6), Map.entry('7', 5), Map.entry('8', 6), Map.entry('9', 6),
		// Symbols
		Map.entry(' ', 1), Map.entry(':', 1), Map.entry(';', 2), Map.entry('"', 3), Map.entry('@', 11), Map.entry('!', 1), Map.entry('.', 1), Map.entry('\'', 2), Map.entry(',', 2), Map.entry('(', 2), Map.entry(')', 2), Map.entry('+', 5), Map.entry('-', 4), Map.entry('=', 6), Map.entry('?', 6), Map.entry('*', 7), Map.entry('/', 4), Map.entry('$', 6), Map.entry('£', 8), Map.entry('^', 6), Map.entry('{', 3), Map.entry('}', 3), Map.entry('[', 3), Map.entry(']', 3), Map.entry('&', 9), Map.entry('#', 11), Map.entry('°', 4), Map.entry('<', 5), Map.entry('>', 5), Map.entry('%', 9),
		// NBSP
		Map.entry('\u00A0', 1));

	public static int getTextLength(String text)
	{
		return Text.removeFormattingTags(text).replace("<lt>", "<").replace("<gt>", ">")
			.chars()
			.mapToObj(ch -> (char) ch)
			.map(key -> {
				if (!CHAR_SIZE_MAP.containsKey(key))
				{
					log.debug("Couldn't get length of {}", key);
				}
				return CHAR_SIZE_MAP.getOrDefault(key, 5) + 2;
			})
			.reduce(0, Integer::sum) + getChatIconsWidth(text);
	}

	private static int getChatIconsWidth(String text)
	{
		int imgCount = Math.toIntExact(IMG_TAG_REGEXP.matcher(text).results().count());
		return imgCount * 13; // 11 + 2
	}

	// Mimics 'paraheight' cs2 instruction
	public static int getTextLineCount(String text, int width, int indentSpaces)
	{
		// Positive lookahead ?= makes it so that the delimiter is included in the split strings
		Iterator<String> iterator = List.of(text.split("(?=(<br>)|([ \u00A0]))")).iterator();

		int numLines = 0;
		StringBuilder currentLine = new StringBuilder();

		while (iterator.hasNext())
		{
			String next = iterator.next();

			int currentWidth = getTextLength(currentLine.toString());

			if (currentWidth < width)
			{
				// Start of the line
				if (currentLine.toString().isEmpty())
				{
					currentLine.append(next);
				}
				// Adding a line break (from game messages) - player messages get escaped as <lt>br<gt>
				else if (next.startsWith("<br>"))
				{
					numLines++;
					currentLine = new StringBuilder(next);
				}
				// Adding the next chunk
				else if (currentWidth + getTextLength(next) < width)
				{
					currentLine.append(next);
				}
				// Adding the next chunk after the initial indent
				// `currentWidth + getTextLength(next) > width` is implied
				// add 1 to indentSpaces so that we include the next chunk after the indent on the first line
				else if (numLines == 0 && currentLine.length() <= indentSpaces + 1)
				{
					currentLine.append(next);
				}
				// Width too big, go to next line
				else
				{
					numLines++;
					currentLine = new StringBuilder(next);
				}
			}
			// Width immediately too big, go to next line (this is probably a chunk that will get cut off)
			else
			{
				numLines++;
				currentLine = new StringBuilder(next);
			}
		}
		numLines++;

		return numLines;
	}

}
