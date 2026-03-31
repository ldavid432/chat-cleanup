package com.github.ldavid432.cleanchat.util;

import static com.github.ldavid432.cleanchat.util.SimpleDateFormatUtil.formatTokenToRegex;
import static com.github.ldavid432.cleanchat.util.SimpleDateFormatUtil.getExpandedSize;
import static com.github.ldavid432.cleanchat.util.SimpleDateFormatUtil.isFormatChar;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FormatterExtractor {

	/**
	 * Represents a segment of the formatted output and its source format token
	 */
	@Value
	public static class FormatSegment
	{
		public String token;      // The format token (e.g., "yyyy", "MM", "dd")
		public char tokenChar;    // The token character (e.g., 'y', 'M', 'd')
		public int tokenCount;    // The number of repetitions (e.g., 4 for "yyyy")
		public int startIndex;    // Start position in the output string
		public int endIndex;      // End position in the output string (exclusive)
		public String value;      // Placeholder value until matched against actual output
	}

	/**
	 * Result containing the formatted output structure and segment information
	 */
	@Value
	public static class ExtractionResult
	{
		String formattedOutput;  // The template output with placeholders
		List<FormatSegment> segments;
		String remainingText;
	}

	/**
	 * Create an ExtractionResult from just the format string.
	 * This expands format characters to their maximum sizes and creates the template.
	 */
	public static ExtractionResult createFromFormatString(String formatString)
	{
		List<FormatSegment> segments = new ArrayList<>();
		StringBuilder templateOutput = new StringBuilder();
		int currentPos = 0;
		int i = 0;

		while (i < formatString.length())
		{
			char c = formatString.charAt(i);

			// Handle quoted literals
			if (c == '\'')
			{
				i++;
				while (i < formatString.length() && formatString.charAt(i) != '\'')
				{
					templateOutput.append(formatString.charAt(i));
					currentPos++;
					i++;
				}
				i++; // Skip closing quote
				continue;
			}

			// Check if this is a format character
			if (isFormatChar(c))
			{
				// Count consecutive identical format characters
				int count = 1;
				while (i + count < formatString.length() && formatString.charAt(i + count) == c)
				{
					count++;
				}

				String token = String.valueOf(c).repeat(count);
				int expandedSize = getExpandedSize(c, count);

				// Create placeholder in template (spaces for now)
				String placeholder = " ".repeat(expandedSize);
				int startIndex = currentPos;
				int endIndex = currentPos + expandedSize;

				templateOutput.append(placeholder);
				segments.add(new FormatSegment(token, c, count, startIndex, endIndex, null));

				currentPos = endIndex;
				i += count;
			}
			else
			{
				// This is a literal character, not a format token
				templateOutput.append(c);
				currentPos++;
				i++;
			}
		}

		return new ExtractionResult(templateOutput.toString(), segments, "");
	}

	/**
	 * Extract the actual formatted output from text using the ExtractionResult template,
	 * and populate the segment values.
	 * This version handles color formatting tags like <col=000000>text</col>
	 */
	public static ExtractionResult extractFromText(ExtractionResult template, String text)
	{
		// Remove color formatting tags for matching purposes
		String cleanedText = removeColorTags(text);

		String regex = formatToRegexFromSegments(template.segments, template.formattedOutput);

		Pattern pattern = Pattern.compile("^" + regex);
		Matcher matcher = pattern.matcher(cleanedText);

		if (matcher.find())
		{
			String formattedOutput = matcher.group(0);
			int matchedLength = formattedOutput.length();

			// Find where the match ends in the original text (accounting for tags)
			String remainingText = text.substring(findOriginalPosition(text, matchedLength));

			// Populate segment values from the actual output
			List<FormatSegment> populatedSegments = new ArrayList<>();
			for (FormatSegment seg : template.segments)
			{
				String value = formattedOutput.substring(seg.startIndex, seg.endIndex);
				populatedSegments.add(new FormatSegment(seg.token, seg.tokenChar, seg.tokenCount,
					seg.startIndex, seg.endIndex, value));
			}

			return new ExtractionResult(formattedOutput, populatedSegments, remainingText);
		}

		return null;
	}

	/**
	 * Find the position in the original text corresponding to a cleaned text position
	 */
	private static int findOriginalPosition(String originalText, int cleanedPosition)
	{
		int cleanedCount = 0;
		int originalPos = 0;

		while (originalPos < originalText.length() && cleanedCount < cleanedPosition)
		{
			// Check if we're at the start of a color tag
			if (originalText.startsWith("<col=", originalPos))
			{
				// Skip the opening tag
				int closeTag = originalText.indexOf(">", originalPos);
				if (closeTag != -1)
				{
					originalPos = closeTag + 1;
					continue;
				}
			}

			// Check if we're at a closing tag
			if (originalText.startsWith("</col>", originalPos))
			{
				originalPos += 6;
				continue;
			}

			// Regular character
			cleanedCount++;
			originalPos++;
		}

		return originalPos;
	}

	/**
	 * Remove color formatting tags like <col=000000>text</col> from the text
	 */
	private static String removeColorTags(String text)
	{
		// Pattern to match <col=XXXXXX> and </col> tags
		return text.replaceAll("<col=[0-9a-fA-F]{6,8}>|</col>", "");
	}

	/**
	 * Build regex from the segment list and the template (for extracting from actual text)
	 */
	private static String formatToRegexFromSegments(List<FormatSegment> segments, String template)
	{
		StringBuilder regex = new StringBuilder();
		int templatePos = 0;

		for (FormatSegment seg : segments)
		{
			// Add any literal text before this segment
			if (templatePos < seg.startIndex)
			{
				String literal = template.substring(templatePos, seg.startIndex);
				regex.append(Pattern.quote(literal));
			}

			// Add the regex pattern for this segment
			String regexPart = formatTokenToRegex(seg.tokenChar, seg.tokenCount);
			regex.append("(").append(regexPart).append(")");

			templatePos = seg.endIndex;
		}

		// Add any remaining literal text after the last segment
		if (templatePos < template.length())
		{
			String literal = template.substring(templatePos);
			regex.append(Pattern.quote(literal));
		}

		return regex.toString();
	}

	/**
	 * Callback interface for processing each part of the output
	 */
	public interface OutputPartConsumer
	{
		void consumeSegment(FormatSegment segment);

		void consumeText(String text, int startIndex, int endIndex);
	}

	/**
	 * Iterate through the formatted output, calling the consumer for each segment
	 * (format token) or literal text portion.
	 */
	public static void iterateOutputParts(ExtractionResult result, OutputPartConsumer consumer)
	{
		List<FormatSegment> segments = result.segments;
		String output = result.formattedOutput;

		int currentPos = 0;

		for (FormatSegment segment : segments)
		{

			// If there's text before this segment, process it as literal text
			if (currentPos < segment.startIndex)
			{
				String literalText = output.substring(currentPos, segment.startIndex);
				consumer.consumeText(literalText, 0, literalText.length());
				currentPos = segment.startIndex;
			}

			// Process the segment
			consumer.consumeSegment(segment);
			currentPos = segment.endIndex;
		}

		// Process any remaining text after the last segment
		if (currentPos < output.length())
		{
			String remainingText = output.substring(currentPos);
			consumer.consumeText(remainingText, 0, remainingText.length());
		}
	}
}
