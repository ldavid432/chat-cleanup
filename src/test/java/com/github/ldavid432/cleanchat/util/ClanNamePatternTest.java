package com.github.ldavid432.cleanchat.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.regex.Pattern;

public class ClanNamePatternTest {

    private String pattern;
    private String clanName;

    @Before
    public void setUp() {
        clanName = "F";
        pattern = "\\[<col=[0-9a-fA-F]+>\\s*" + Pattern.quote(clanName) + "\\s*</col>\\]|\\[" + Pattern.quote(clanName) + "\\]";
    }

    @Test
    public void testDetectPlainClanName() {
        String message = "08:32:30 [F] the lil fish:";
        assertTrue(Pattern.compile(pattern).matcher(message).find());
    }

    @Test
    public void testDetectColoredClanName() {
        String message = "08:32:30 [<col=9070ff>F</col>] <img=41>the lil fish:";
        assertTrue(Pattern.compile(pattern).matcher(message).find());
    }

    @Test
    public void testDetectColoredClanNameWithSpaces() {
        String message = "08:32:30 [<col=FF0000> F </col>] the lil fish:";
        assertTrue(Pattern.compile(pattern).matcher(message).find());
    }

    @Test
    public void testDetectClanNameAtStart() {
        String message = "[F] message here";
        assertTrue(Pattern.compile(pattern).matcher(message).find());
    }

    @Test
    public void testDoNotMatchWrongClanName() {
        String message = "08:32:30 [Fake] the lil fish:";
        assertFalse(Pattern.compile(pattern).matcher(message).find());
    }

    @Test
    public void testDoNotMatchSubstringOfClanName() {
        String message = "08:32:30 [FIGHTER] the lil fish:";
        assertFalse(Pattern.compile(pattern).matcher(message).find());
    }

    @Test
    public void testDoNotMatchClanNameOutsideBrackets() {
        String message = "F is a cool clan";
        assertFalse(Pattern.compile(pattern).matcher(message).find());
    }

    @Test
    public void testRemovePlainClanName() {
        String message = "08:32:30 [F]the lil fish:";
        String result = message.replaceAll(pattern, "").trim();
        assertEquals("08:32:30 the lil fish:", result);
    }

    @Test
    public void testRemoveColoredClanName() {
        String message = "08:32:30 [<col=9070ff>F</col>]<img=41>the lil fish:";
        String result = message.replaceAll(pattern, "").trim();
        assertEquals("08:32:30 <img=41>the lil fish:", result);
    }

    @Test
    public void testRemoveColoredClanNameWithSpaces() {
        String message = "08:32:30 [<col=FF0000> F </col>]the lil fish:";
        String result = message.replaceAll(pattern, "").trim();
        assertEquals("08:32:30 the lil fish:", result);
    }

    @Test
    public void testRemoveClanNameAtStart() {
        String message = "[F] this is the message";
        String result = message.replaceAll(pattern, "").trim();
        assertEquals("this is the message", result);
    }

    @Test
    public void testDoNotRemoveWrongClanName() {
        String message = "08:32:30 [Fake] the lil fish:";
        String result = message.replaceAll(pattern, "");
        assertEquals(message, result);
    }

    @Test
    public void testClanNameWithSpecialRegexCharacters() {
        String specialClan = "F.+*?";
        String specialPattern = "\\[<col=[0-9a-fA-F]+>\\s*" + Pattern.quote(specialClan) + "\\s*</col>\\]|\\[" + Pattern.quote(specialClan) + "\\]";
        String message = "[F.+*?] message";
        String result = message.replaceAll(specialPattern, "").trim();
        assertEquals("message", result);
    }

    @Test
    public void testEmptyMessageAfterRemoval() {
        String message = "[F]";
        String result = message.replaceAll(pattern, "").trim();
        assertEquals("", result);
    }
}
