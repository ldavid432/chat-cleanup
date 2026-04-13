package com.github.ldavid432.cleanchat.util;

public class SimpleDateFormatUtil
{

	// TODO: This technically should just be all chars a-z I think?
	/**
	 * Determine if a character is a valid SimpleDateFormat format character
	 */
	static boolean isFormatChar(char c)
	{
		return c == 'y' || c == 'Y' ||
			c == 'M' ||
			c == 'w' || c == 'W' ||
			c == 'd' || c == 'D' ||
			c == 'F' ||
			c == 'E' ||
			c == 'u' ||
			c == 'a' ||
			c == 'H' || c == 'h' ||
			c == 'K' || c == 'k' ||
			c == 'm' ||
			c == 's' || c == 'S' ||
			c == 'z' || c == 'Z' ||
			c == 'X';
	}

	/**
	 * Convert a single format token to regex
	 */
	static String formatTokenToRegex(char token, int count)
	{
		switch (token)
		{
			case 'y': // Year
			case 'Y': // Week-based year
				if (count == 2)
				{
					return "\\d{2}";
				}
				else
				{
					return "\\d{4}";
				}

			case 'M': // Month
				if (count == 1)
				{
					return "\\d{1,2}";
				}
				if (count == 2)
				{
					return "\\d{2}";
				}
				if (count == 3)
				{
					return "[A-Z][a-z]{2}";
				}
				return "[A-Z][a-z]+";

			case 'w': // Week of year
				if (count == 1)
				{
					return "\\d{1,2}";
				}
				return "\\d{2}";

			case 'W': // Week of month
				return "\\d";

			case 'd': // Day of month
				if (count == 1)
				{
					return "\\d{1,2}";
				}
				return "\\d{2}";

			case 'D': // Day of year
				if (count == 1)
				{
					return "\\d{1,3}";
				}
				if (count == 2)
				{
					return "\\d{1,3}";
				}
				return "\\d{3}";

			case 'F': // Day of week in month
				return "\\d";

			case 'E': // Day name
				if (count < 4)
				{
					return "[A-Z][a-z]{2}";
				}
				return "[A-Z][a-z]+";

			case 'u': // Day of week (1-7, Monday = 1)
				return "\\d";

			case 'a': // AM/PM
				return "AM|PM";

			case 'H': // Hour (0-23)
				if (count == 1)
				{
					return "\\d{1,2}";
				}
				return "\\d{2}";

			case 'h': // Hour (1-12)
				if (count == 1)
				{
					return "\\d{1,2}";
				}
				return "\\d{2}";

			case 'K': // Hour (0-11)
			case 'k': // Hour (1-24)
				if (count == 1)
				{
					return "\\d{1,2}";
				}
				return "\\d{2}";

			case 'm': // Minute
			case 's': // Second
				if (count == 1)
				{
					return "\\d{1,2}";
				}
				return "\\d{2}";


			case 'S': // Millisecond/Nanosecond
				return "\\d{" + count + "}";

			case 'z': // Time zone abbreviation
				return "[A-Z]{3,4}";

			case 'Z': // Time zone offset
				return "[+-]\\d{4}";

			case 'X': // ISO time zone (±HH:mm, ±HHMM, or Z)
				if (count == 1)
				{
					return "[+-]\\d{2}|Z";
				}
				if (count == 2)
				{
					return "[+-]\\d{2}:\\d{2}|Z";
				}
				return "[+-]\\d{2}:\\d{2}:\\d{2}|Z";

			default:
				return ".{1," + count + "}";
		}
	}

	/**
	 * Get the maximum expanded size for a format token
	 */
	static int getExpandedSize(char token, int count)
	{
		switch (token)
		{
			case 'y': // Year
				return count == 2 ? 2 : 4;
			case 'Y': // Week-based year
				return 4;
			case 'M': // Month
				if (count == 1 || count == 2)
					return 2;
				if (count == 3)
					return 3;
				return 9; // "September"
			case 'w': // Week of year
				return 2;
			case 'W': // Week of month
				return 1;
			case 'd': // Day of month
				return 2;
			case 'D': // Day of year
				return 3;
			case 'F': // Day of week in month
				return 1;
			case 'E': // Day name
				return count < 4 ? 3 : 9; // "Mon" or "Monday"
			case 'u': // Day of week (1-7)
				return 1;
			case 'a': // AM/PM
				return 2;
			case 'H': // Hour (0-23)
				return 2;
			case 'h': // Hour (1-12)
				return 2;
			case 'K': // Hour (0-11)
				return 2;
			case 'k': // Hour (1-24)
				return 2;
			case 'm': // Minute
				return 2;
			case 's': // Second
				return 2;
			case 'S': // Millisecond
				return count;
			case 'z': // Time zone
				return 4; // "EST"
			case 'Z': // Time zone offset
				return 5; // "+0000"
			case 'X': // ISO time zone
				return 6; // "+00:00"
			default:
				return count;
		}
	}
}
