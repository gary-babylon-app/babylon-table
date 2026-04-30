/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.text;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

public class StringsTest
{
    @Test
    public void leftPadShouldMatchExpectedBehavior()
    {
        assertNull(Strings.leftPad(null, 5, 'x'));
        assertEquals("bat", Strings.leftPad("bat", 1, 'x').toString());
        assertEquals("bat", Strings.leftPad("bat", 3, 'x').toString());
        assertEquals("xxbat", Strings.leftPad("bat", 5, 'x').toString());
        assertEquals("xxx", Strings.leftPad("", 3, 'x').toString());
    }

    @Test
    public void rightPadShouldMatchExpectedBehavior()
    {
        assertNull(Strings.rightPad(null, 5, 'x'));
        assertEquals("bat", Strings.rightPad("bat", 1, 'x').toString());
        assertEquals("bat", Strings.rightPad("bat", 3, 'x').toString());
        assertEquals("batxx", Strings.rightPad("bat", 5, 'x').toString());
        assertEquals("xxx", Strings.rightPad("", 3, 'x').toString());
    }

    @Test
    public void toCamelUpperShouldNormalizeCommonSeparatorsAndCase()
    {
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade-date").toString());
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("TRADE_DATE").toString());
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade date").toString());
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade.date").toString());
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("tradeDate").toString());
        assertEquals("", Strings.toCamelUpperPreserve("").toString());
        assertNull(Strings.toCamelUpperPreserve(null));
    }

    @Test
    public void isAlphaAndIsAlphaNumericShouldRecogniseBasicCharacters()
    {
        assertTrue(Strings.isAlpha('a'));
        assertTrue(Strings.isAlpha('Z'));
        assertFalse(Strings.isAlpha('1'));
        assertFalse(Strings.isAlpha('-'));

        assertTrue(Strings.isAlphaNumeric('a'));
        assertTrue(Strings.isAlphaNumeric('Z'));
        assertTrue(Strings.isAlphaNumeric('1'));
        assertFalse(Strings.isAlphaNumeric('-'));
    }

    @Test
    public void isEmptyShouldRecogniseNullAndEmptyCharSequences()
    {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("abc"));
    }

    @Test
    public void isStripxEmptyShouldRecogniseEmptySlicesAfterStripping()
    {
        assertTrue(Strings.isStripxEmpty(null, 0, 1));
        assertTrue(Strings.isStripxEmpty("abc", 1, 0));
        assertTrue(Strings.isStripxEmpty("xx  yy", 2, 2));
        assertTrue(Strings.isStripxEmpty("xx\uFEFF\u00A0\u200Byy", 2, 3));
        assertFalse(Strings.isStripxEmpty("xx ab yy", 2, 3));
        assertFalse(Strings.isStripxEmpty("xxabc yy", 2, 3));
    }

    @Test
    public void indexOfShouldFindHyphenInWholeSequenceAndSlices()
    {
        assertEquals(5, Strings.indexOf("trade-date", '-'));
        assertEquals(-1, Strings.indexOf("tradedate", '-'));
        assertEquals(7, Strings.indexOf("xxtrade-dateyy", 2, 10, '-'));
        assertEquals(-1, Strings.indexOf("xxtrade-dateyy", 0, 2, '-'));
        assertEquals(-1, Strings.indexOf(null, '-'));
    }

    @Test
    public void indexOfAnyShouldFindAnyRequestedCharacterInWholeSequenceAndSlices()
    {
        assertEquals(5, Strings.indexOfAny("trade,date", ',', ';'));
        assertEquals(5, Strings.indexOfAny("trade;date", ',', ';'));
        assertEquals(5, Strings.indexOfAny("trade|date", ',', ';', '|'));
        assertEquals(5, Strings.indexOfAny("trade\ndate", ',', ';', '|', '\n'));

        assertEquals(7, Strings.indexOfAny("xxtrade,dateyy", 2, 10, ',', ';'));
        assertEquals(7, Strings.indexOfAny("xxtrade;dateyy", 2, 10, ',', ';'));
        assertEquals(7, Strings.indexOfAny("xxtrade|dateyy", 2, 10, ',', ';', '|'));
        assertEquals(7, Strings.indexOfAny("xxtrade\ndateyy", 2, 10, ',', ';', '|', '\n'));

        assertEquals(-1, Strings.indexOfAny("tradedate", ',', ';'));
        assertEquals(-1, Strings.indexOfAny("tradedate", ',', ';', '|'));
        assertEquals(-1, Strings.indexOfAny("tradedate", ',', ';', '|', '\n'));
        assertEquals(-1, Strings.indexOfAny("xxtrade,dateyy", 0, 2, ',', ';', '|', '\n'));
        assertEquals(-1, Strings.indexOfAny(null, ',', ';'));
    }

    @Test
    public void lastIndexOfShouldFindLastHyphenInWholeSequenceAndSlices()
    {
        assertEquals(10, Strings.lastIndexOf("trade-date-end", '-'));
        assertEquals(-1, Strings.lastIndexOf("tradedate", '-'));
        assertEquals(12, Strings.lastIndexOf("xxtrade-date-endyy", 2, 14, '-'));
        assertEquals(7, Strings.lastIndexOf("xxtrade-date-endyy", 2, 10, '-'));
        assertEquals(-1, Strings.lastIndexOf(null, '-'));
    }

    @Test
    public void traceShouldReturnSplitterIndexes()
    {
        CharSequence s = "xx alpha, beta ,gamma yy";

        assertEquals(bitSet(8, 15), Strings.trace(s, 3, 18, ','));
    }

    @Test
    public void traceShouldReturnEverySplitterIndex()
    {
        CharSequence s = "a,, b, ,";

        assertEquals(bitSet(1, 2, 5, 7), Strings.trace(s, ','));
    }

    @Test
    public void splitterShouldPreserveEmptyFieldsWhenRequested()
    {
        CharSequence s = "a,, b, ,";

        assertArrayEquals(new String[]
        {"a", "", "b", "", ""}, Strings.splitter().withRemoveEmpty(false).split(s));
    }

    @Test
    public void splitterConfigurationShouldBeImmutable()
    {
        Strings.Splitter splitter = Strings.splitter();
        Strings.Splitter keepEmpty = splitter.withRemoveEmpty(false);
        Strings.Splitter pipe = splitter.withSplitter('|');

        assertArrayEquals(new String[]
        {"a", "b"}, splitter.split("a,,b"));
        assertArrayEquals(new String[]
        {"a", "", "b"}, keepEmpty.split("a,,b"));
        assertArrayEquals(new String[]
        {"a", "b"}, pipe.split("a|b"));
        assertArrayEquals(new String[]
        {"a|b"}, splitter.split("a|b"));
    }

    @Test
    public void traceShouldReturnEmptyArrayForNullOrEmptyInput()
    {
        assertTrue(Strings.trace(null, ',').isEmpty());
        assertTrue(Strings.trace("", ',').isEmpty());
        assertTrue(Strings.trace("abc", 1, 0, ',').isEmpty());
    }

    @Test
    public void splitShouldMaterializeStrippedTokens()
    {
        assertArrayEquals(new String[]
        {"alpha", "beta", "gamma"}, Strings.split(" alpha, beta ,, gamma ,", ','));
        assertArrayEquals(new String[]
        {"alpha", "beta", "gamma"}, Strings.split(" alpha, beta ,, gamma ,"));
        assertArrayEquals(new String[0], Strings.split(null, ','));
    }

    @Test
    public void compactShouldRemoveNullHoles()
    {
        String[] compact = new String[]
        {"a", "b"};

        assertArrayEquals(new String[0], Strings.compact(null));
        assertSame(compact, Strings.compact(compact));
        assertArrayEquals(new String[]
        {"a", "b", "c"}, Strings.compact(new String[]
        {"a", null, "b", null, "c"}));
    }

    @Test
    public void compactShouldRemovePredicateMatches()
    {
        String[] compact = new String[]
        {"a", "", "b"};

        assertSame(compact, Strings.compact(compact, null));
        assertSame(compact, Strings.compact(compact, s -> false));
        assertArrayEquals(new String[]
        {"a", "b"}, Strings.compact(new String[]
        {"", "a", null, "b", ""}, Strings.EMPTY_OR_NULL));
    }

    @Test
    public void splitterShouldRemoveEmptyFieldsAfterStrippingByDefault()
    {
        assertArrayEquals(new String[]
        {"a", "b"}, Strings.splitter().split(" a, ,b,, "));
    }

    @Test
    public void splitterShouldUseStripxWhenStripping()
    {
        assertArrayEquals(new String[]
        {"a", "b"}, Strings.splitter().split("\uFEFFa\u200B,\u00A0\u200B,b\uFFFD"));
        assertArrayEquals(new String[]
        {"\uFEFFa\u200B", "\u00A0\u200B", "b\uFFFD"},
                Strings.splitter().withStripping(false).split("\uFEFFa\u200B,\u00A0\u200B,b\uFFFD"));
    }

    @Test
    public void splitterShouldPreserveWhitespaceWhenStripIsFalse()
    {
        assertArrayEquals(new String[]
        {" a", " b "}, Strings.splitter().withStripping(false).split(" a, b "));
        assertArrayEquals(new String[]
        {" a", " ", "b"}, Strings.splitter().withStripping(false).withRemoveEmpty(true).split(" a, ,b"));
    }

    @Test
    public void splitterShouldSupportCustomDelimiterAndSlices()
    {
        CharSequence s = "xx a | b | c yy";

        assertEquals(bitSet(5, 9), Strings.trace(s, 3, 9, '|'));
        assertArrayEquals(new String[]
        {"a", "b", "c"}, Strings.splitter().withSplitter('|').split(s, 3, 9));
    }

    @Test
    public void isWholeNumberShouldRecogniseSignedDigitStrings()
    {
        assertTrue(Strings.isWholeNumber("123"));
        assertTrue(Strings.isWholeNumber("-123"));
        assertTrue(Strings.isWholeNumber("+123"));
        assertFalse(Strings.isWholeNumber(null));
        assertFalse(Strings.isWholeNumber(""));
        assertFalse(Strings.isWholeNumber("+"));
        assertFalse(Strings.isWholeNumber("12 3"));
        assertFalse(Strings.isWholeNumber("12.3"));
    }

    @Test
    public void isWholeNumberShouldRecogniseSignedDigitSlices()
    {
        assertTrue(Strings.isWholeNumber("x123y", 1, 3));
        assertTrue(Strings.isWholeNumber("x-123y", 1, 4));
        assertTrue(Strings.isWholeNumber("x+123y", 1, 4));
        assertTrue(Strings.isWholeNumber("123%", 0, 3));
        assertFalse(Strings.isWholeNumber(null, 0, 1));
        assertFalse(Strings.isWholeNumber("123", 0, 0));
        assertFalse(Strings.isWholeNumber("x+y", 1, 1));
        assertFalse(Strings.isWholeNumber("12.3%", 0, 4));
        assertFalse(Strings.isWholeNumber("x12.3y", 1, 4));
        assertFalse(Strings.isWholeNumber("x12 3y", 1, 4));
    }

    @Test
    public void isIntShouldRecogniseIntBoundaries()
    {
        assertTrue(Strings.isInt("2147483646"));
        assertTrue(Strings.isInt("2147483647"));
        assertTrue(Strings.isInt("-2147483647"));
        assertTrue(Strings.isInt("-2147483648"));
        assertTrue(Strings.isInt("+2147483647"));
        assertTrue(Strings.isInt("999999999"));
        assertFalse(Strings.isInt("2147483648"));
        assertFalse(Strings.isInt("2147483649"));
        assertFalse(Strings.isInt("-2147483649"));
        assertFalse(Strings.isInt("-2147483650"));
        assertFalse(Strings.isInt("99999999999"));
        assertFalse(Strings.isInt("+"));
        assertFalse(Strings.isInt("12.3"));
    }

    @Test
    public void isIntShouldRecogniseIntSlices()
    {
        assertTrue(Strings.isInt("2147483647%", 0, 10));
        assertTrue(Strings.isInt("x-2147483648y", 1, 11));
        assertFalse(Strings.isInt("2147483648%", 0, 10));
        assertFalse(Strings.isInt("12.3%", 0, 4));
    }

    @Test
    public void isLongShouldRecogniseLongBoundaries()
    {
        assertTrue(Strings.isLong("9223372036854775806"));
        assertTrue(Strings.isLong("9223372036854775807"));
        assertTrue(Strings.isLong("-9223372036854775807"));
        assertTrue(Strings.isLong("-9223372036854775808"));
        assertTrue(Strings.isLong("+9223372036854775807"));
        assertFalse(Strings.isLong("9223372036854775808"));
        assertFalse(Strings.isLong("9223372036854775809"));
        assertFalse(Strings.isLong("-9223372036854775809"));
        assertFalse(Strings.isLong("-9223372036854775810"));
        assertFalse(Strings.isLong("92233720368547758070"));
        assertFalse(Strings.isLong("+"));
        assertFalse(Strings.isLong("12.3"));
    }

    @Test
    public void isLongShouldRecogniseLongSlices()
    {
        assertTrue(Strings.isLong("9223372036854775807%", 0, 19));
        assertTrue(Strings.isLong("x-9223372036854775808y", 1, 20));
        assertFalse(Strings.isLong("9223372036854775808%", 0, 19));
        assertFalse(Strings.isLong("12.3%", 0, 4));
    }

    @Test
    public void stripShouldTrimWhitespaceOnly()
    {
        String plain = "abc";

        assertNull(Strings.strip((CharSequence) null));
        assertEquals("", Strings.strip(""));
        assertSame(plain, Strings.strip(plain));
        assertEquals("abc", Strings.strip("  abc  "));
        assertEquals("abc", Strings.strip("\n\tabc\r"));
        assertEquals("\uFEFF abc \u200B", Strings.strip("\uFEFF abc \u200B"));
        assertEquals("", Strings.strip(" \n\t "));
    }

    @Test
    public void stripStartAndStripEndShouldTrimRequestedSlice()
    {
        CharSequence s = "xx  abc  yy";

        assertEquals(0, Strings.stripStart(s, 0, 0));
        assertEquals(0, Strings.stripEnd(s, 0, 0));
        assertEquals(4, Strings.stripStart(s, 2, 7));
        assertEquals(7, Strings.stripEnd(s, 2, 7));
        assertEquals(3, Strings.stripStart("xx\nabc\ryy", 2, 5));
        assertEquals(6, Strings.stripEnd("xx\nabc\ryy", 2, 5));
        assertEquals(2, Strings.stripStart("xx\uFEFFabc yy", 2, 5));
        assertEquals(6, Strings.stripEnd("xx\uFEFFabc yy", 2, 5));
    }

    @Test
    public void stripxShouldTrimWhitespaceAndIngestionCharacters()
    {
        String plain = "abc";

        assertNull(Strings.stripx((CharSequence) null));
        assertEquals("", Strings.stripx(""));
        assertSame(plain, Strings.stripx(plain));
        assertEquals("abc", Strings.stripx("  abc  "));
        assertEquals("abc", Strings.stripx("\uFEFF abc \u200B"));
        assertEquals("", Strings.stripx(" \u00A0\u200B "));
    }

    @Test
    public void removeDiacriticsShouldFastPathAsciiAndRemoveMarks()
    {
        String plain = "Trade Date";

        assertNull(Strings.removeDiacritics(null));
        assertSame(plain, Strings.removeDiacritics(plain));
        assertEquals("Trade", Strings.removeDiacritics("xxTradeyy", 2, 5));
        assertEquals("Cafe", Strings.removeDiacritics("Caf\u00E9"));
        assertEquals("Angstrom", Strings.removeDiacritics("\u00C5ngstr\u00F6m"));
        assertEquals("Sao Paulo", Strings.removeDiacritics("S\u00E3o Paulo"));
    }

    private static BitSet bitSet(int... indexes)
    {
        BitSet bits = new BitSet();
        for (int index : indexes)
        {
            bits.set(index);
        }
        return bits;
    }
}
