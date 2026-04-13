package com.github.ldavid432.cleanchat.util;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FormatterExtractorTest
{
	@Before
	public void setUp()
	{
		// Setup if needed
	}

	// ============================================================================
	// createFromFormatString Tests
	// ============================================================================

	@Test
	public void testCreateFromFormatStringSimpleFormat()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("HH:mm:ss");

		assertNotNull(result);
		assertEquals(8, result.getFormattedOutput().length());
		assertEquals(3, result.getSegments().size());
	}

	@Test
	public void testCreateFromFormatStringWithQuotedLiterals()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("HH'h'mm'm'ss's'");

		assertNotNull(result);
		assertEquals("HH", result.getSegments().get(0).getToken());
		assertTrue(result.getFormattedOutput().contains("h"));
		assertTrue(result.getFormattedOutput().contains("m"));
		assertTrue(result.getFormattedOutput().contains("s"));
	}

	@Test
	public void testCreateFromFormatStringWithSpaces()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("HH:mm:ss a");

		assertNotNull(result);
		assertTrue(result.getFormattedOutput().contains(":"));
		assertEquals("a", result.getSegments().get(3).getToken());
	}

	@Test
	public void testCreateFromFormatStringYearExpansion()
	{
		FormatterExtractor.ExtractionResult result2Digit =
			FormatterExtractor.createFromFormatString("yy");
		FormatterExtractor.ExtractionResult result4Digit =
			FormatterExtractor.createFromFormatString("yyyy");

		assertEquals(2, result2Digit.getFormattedOutput().length());
		assertEquals(4, result4Digit.getFormattedOutput().length());
	}

	@Test
	public void testCreateFromFormatStringMonthFormats()
	{
		FormatterExtractor.ExtractionResult result1 =
			FormatterExtractor.createFromFormatString("M");
		FormatterExtractor.ExtractionResult result2 =
			FormatterExtractor.createFromFormatString("MM");
		FormatterExtractor.ExtractionResult result3 =
			FormatterExtractor.createFromFormatString("MMM");
		FormatterExtractor.ExtractionResult result4 =
			FormatterExtractor.createFromFormatString("MMMM");

		assertEquals(2, result1.getFormattedOutput().length());
		assertEquals(2, result2.getFormattedOutput().length());
		assertEquals(3, result3.getFormattedOutput().length());
		assertEquals(9, result4.getFormattedOutput().length());
	}

	@Test
	public void testCreateFromFormatStringDayNameFormats()
	{
		FormatterExtractor.ExtractionResult result3 =
			FormatterExtractor.createFromFormatString("EEE");
		FormatterExtractor.ExtractionResult result4 =
			FormatterExtractor.createFromFormatString("EEEE");

		assertEquals(3, result3.getFormattedOutput().length());
		assertEquals(9, result4.getFormattedOutput().length());
	}

	@Test
	public void testCreateFromFormatStringEmptyFormat()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("");

		assertNotNull(result);
		assertEquals(0, result.getSegments().size());
		assertEquals("", result.getFormattedOutput());
	}

	@Test
	public void testCreateFromFormatStringOnlyLiterals()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("'hello world'");

		assertNotNull(result);
		assertEquals(0, result.getSegments().size());
		assertEquals("hello world", result.getFormattedOutput());
	}

	@Test
	public void testCreateFromFormatStringComplexFormat()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("EEEE, MMMM d, yyyy 'at' h:mm a");

		assertNotNull(result);
		assertTrue(result.getSegments().size() >= 6);
		assertTrue(result.getFormattedOutput().contains("at"));
	}

	@Test
	public void testCreateFromFormatStringMilliseconds()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("HH:mm:ss.SSS");

		assertNotNull(result);
		assertTrue(result.getFormattedOutput().contains("."));
	}

	// ============================================================================
	// extractFromText Tests
	// ============================================================================

	@Test
	public void testExtractFromTextSimpleTime()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "12:34:56 Hello");

		assertNotNull(result);
		assertEquals("12:34:56", result.getFormattedOutput());
		assertTrue(result.getRemainingText().contains("Hello"));
	}

	@Test
	public void testExtractFromTextMiddleOfText()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "14:30 is meeting time");

		assertNotNull(result);
		assertEquals("14:30", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextNotFound()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "No timestamp here");

		assertNull(result);
	}

	@Test
	public void testExtractFromTextSingleDigitTime()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("H:m:s");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "5:3:45 Message");

		assertNotNull(result);
		assertEquals("5:3:45", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextFullDateTime()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("yyyy-MM-dd HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "2025-04-13 14:30:25 User said hello");

		assertNotNull(result);
		assertEquals("2025-04-13 14:30:25", result.getFormattedOutput());
		assertTrue(result.getRemainingText().contains("User said hello"));
	}

	@Test
	public void testExtractFromTextMonthAbbrv()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("MMM dd");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "Apr 13 Message");

		assertNotNull(result);
		assertEquals("Apr 13", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextDayName()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("EEEE");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "Monday is here");

		assertNotNull(result);
		assertEquals("Monday", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextShortDay()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("EEE");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "Mon 12:00");

		assertNotNull(result);
		assertEquals("Mon", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextAMPM()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("hh:mm a");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "03:30 PM Message");

		assertNotNull(result);
		assertEquals("03:30 PM", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextSegmentPopulation()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "12:34:56 Message");

		assertNotNull(result);
		assertEquals(3, result.getSegments().size());
		assertEquals("12", result.getSegments().get(0).getValue());
		assertEquals("34", result.getSegments().get(1).getValue());
		assertEquals("56", result.getSegments().get(2).getValue());
	}

	@Test
	public void testExtractFromTextLeadingZeros()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("MM-dd");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "04-01 Message");

		assertNotNull(result);
		assertEquals("04-01", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextNoPartialMatch()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("yyyy-MM-dd HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "2025-04-13");

		assertNull(result);
	}

	@Test
	public void testExtractFromTextYearOnly()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("yyyy");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "2025 started");

		assertNotNull(result);
		assertEquals("2025", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextMonthAsNumber()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("MM");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "04 is April");

		assertNotNull(result);
		assertEquals("04", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextWithMilliseconds()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss.SSS");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "12:34:56.789 Event");

		assertNotNull(result);
		assertEquals("12:34:56.789", result.getFormattedOutput());
	}

	@Test
	public void testExtractFromTextTwoDigitYear()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("yy-MM-dd");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "25-04-13 happened");

		assertNotNull(result);
		assertEquals("25-04-13", result.getFormattedOutput());
	}

	// ============================================================================
	// Color Tag Handling Tests
	// ============================================================================

	@Test
	public void testExtractWithColorTags()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template,
				"<col=ff0000>12:34:56</col> Message");

		assertNotNull(result);
		assertEquals("12:34:56", result.getFormattedOutput());
	}

	@Test
	public void testExtractMultipleColorTags()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template,
				"<col=00ff00>14<col=0000ff>:30</col></col> text");

		assertNotNull(result);
		assertEquals("14:30", result.getFormattedOutput());
	}

	@Test
	public void testExtractColorTagsWithAlpha()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template,
				"<col=ff0000ff>12:34:56</col> Message");

		assertNotNull(result);
		assertEquals("12:34:56", result.getFormattedOutput());
	}

	// ============================================================================
	// iterateOutputParts Tests
	// ============================================================================

	@Test
	public void testIterateOutputPartsSimpleFormat()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "12:34:56 Message");

		final List<String> parts = new ArrayList<>();
		assertNotNull(result);
		FormatterExtractor.iterateOutputParts(result, new FormatterExtractor.OutputPartConsumer()
		{
			@Override
			public void consumeSegment(FormatterExtractor.FormatSegment segment)
			{
				parts.add("SEGMENT:" + segment.getValue());
			}

			@Override
			public void consumeText(String text, int startIndex, int endIndex)
			{
				parts.add("TEXT:" + text);
			}
		});

		assertEquals(5, parts.size());
		assertEquals("SEGMENT:12", parts.get(0));
		assertEquals("TEXT::", parts.get(1));
		assertEquals("SEGMENT:34", parts.get(2));
		assertEquals("TEXT::", parts.get(3));
		assertEquals("SEGMENT:56", parts.get(4));
	}

	@Test
	public void testIterateOutputPartsWithLiterals()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("HH'h'mm'm'");

		final List<String> parts = new ArrayList<>();
		FormatterExtractor.iterateOutputParts(result, new FormatterExtractor.OutputPartConsumer()
		{
			@Override
			public void consumeSegment(FormatterExtractor.FormatSegment segment)
			{
				parts.add("SEGMENT:" + segment.getToken());
			}

			@Override
			public void consumeText(String text, int startIndex, int endIndex)
			{
				parts.add("TEXT:" + text);
			}
		});

		assertFalse(parts.isEmpty());
		boolean hasHmLiteral = false;
		for (String part : parts)
		{
			if (part.contains("h") || part.contains("m"))
			{
				hasHmLiteral = true;
				break;
			}
		}
		assertTrue(hasHmLiteral);
	}

	// ============================================================================
	// FormatSegment Tests
	// ============================================================================

	@Test
	public void testFormatSegmentProperties()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.FormatSegment segment = result.getSegments().get(0);

		assertEquals("HH", segment.getToken());
		assertEquals('H', segment.getTokenChar());
		assertEquals(2, segment.getTokenCount());
		assertEquals(0, segment.getStartIndex());
		assertEquals(2, segment.getEndIndex());
	}

	@Test
	public void testFormatSegmentIndices()
	{
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.createFromFormatString("yyyy-MM-dd");

		assertEquals(4, result.getSegments().get(0).getEndIndex());
		assertEquals(5, result.getSegments().get(1).getStartIndex());
		assertEquals(7, result.getSegments().get(1).getEndIndex());
	}

	// ============================================================================
	// ExtractionResult Tests
	// ============================================================================

	@Test
	public void testExtractionResultComponents()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "12:34 Message");

		assertNotNull(result);
		assertNotNull(result.getFormattedOutput());
		assertNotNull(result.getSegments());
		assertNotNull(result.getRemainingText());
	}

	@Test
	public void testExtractionResultRemainingText()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "12:34:56 User: Hello");

		assertNotNull(result);
		assertEquals(" User: Hello", result.getRemainingText());
	}

	// ============================================================================
	// Complex Integration Tests
	// ============================================================================

	@Test
	public void testComplexDateTimeFormat()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("EEEE, MMMM d, yyyy 'at' hh:mm a");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template,
				"Monday, April 13, 2025 at 03:30 PM - Meeting scheduled");

		assertNotNull(result);
		assertTrue(result.getFormattedOutput().contains("Monday"));
		assertTrue(result.getFormattedOutput().contains("April"));
		assertTrue(result.getFormattedOutput().contains("2025"));
		assertTrue(result.getFormattedOutput().contains("PM"));
	}

	@Test
	public void testISODateFormat()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("yyyy-MM-dd'T'HH:mm:ss");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "2025-04-13T14:30:25 UTC");

		assertNotNull(result);
		assertEquals("2025-04-13T14:30:25", result.getFormattedOutput());
	}

	@Test
	public void testAlternateTimeFormat()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("h:mm a");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "3:30 PM is afternoon");

		assertNotNull(result);
		assertEquals("3:30 PM", result.getFormattedOutput());
	}

	@Test
	public void testNumericMonthFormat()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("MM/dd/yyyy");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "04/13/2025 is today");

		assertNotNull(result);
		assertEquals("04/13/2025", result.getFormattedOutput());
	}

	@Test
	public void testDayOfYearFormat()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("yyyy-DDD");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "2025-103 is day 103");

		assertNotNull(result);
		assertEquals("2025-103", result.getFormattedOutput());
	}

	@Test
	public void testSingleDigitMonthDay()
	{
		FormatterExtractor.ExtractionResult template =
			FormatterExtractor.createFromFormatString("M/d/yyyy");
		FormatterExtractor.ExtractionResult result =
			FormatterExtractor.extractFromText(template, "4/3/2025 is April third");

		assertNotNull(result);
		assertEquals("4/3/2025", result.getFormattedOutput());
	}
}
