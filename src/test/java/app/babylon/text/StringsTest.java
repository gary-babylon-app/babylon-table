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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StringsTest
{
    @Test
    public void leftPadShouldMatchExpectedBehavior()
    {
        assertNull(Strings.leftPad(null, 5, 'x'));
        assertEquals("bat", Strings.leftPad("bat", 1, 'x'));
        assertEquals("bat", Strings.leftPad("bat", 3, 'x'));
        assertEquals("xxbat", Strings.leftPad("bat", 5, 'x'));
        assertEquals("xxx", Strings.leftPad("", 3, 'x'));
    }

    @Test
    public void rightPadShouldMatchExpectedBehavior()
    {
        assertNull(Strings.rightPad(null, 5, 'x'));
        assertEquals("bat", Strings.rightPad("bat", 1, 'x'));
        assertEquals("bat", Strings.rightPad("bat", 3, 'x'));
        assertEquals("batxx", Strings.rightPad("bat", 5, 'x'));
        assertEquals("xxx", Strings.rightPad("", 3, 'x'));
    }

    @Test
    public void toCamelUpperShouldNormalizeCommonSeparatorsAndCase()
    {
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade-date"));
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("TRADE_DATE"));
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade date"));
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade.date"));
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("tradeDate"));
        assertEquals("", Strings.toCamelUpperPreserve(""));
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
    public void stripxShouldTrimWhitespaceAndIngestionCharacters()
    {
        assertNull(Strings.stripx((CharSequence) null));
        assertEquals("", Strings.stripx(""));
        assertEquals("abc", Strings.stripx("abc"));
        assertEquals("abc", Strings.stripx("  abc  "));
        assertEquals("abc", Strings.stripx("\uFEFF abc \u200B"));
        assertEquals("", Strings.stripx(" \u00A0\u200B "));
    }

    @Test
    public void stripxCharArrayShouldTrimRequestedSlice()
    {
        char[] chars = "xx \uFEFFabc\u200B yy".toCharArray();

        assertNull(Strings.stripx(null, 0, 0));
        assertEquals("", Strings.stripx(chars, 0, 0));
        assertEquals("abc", Strings.stripx(chars, 2, 7));
        assertEquals("", Strings.stripx("   ".toCharArray(), 0, 3));
    }
}
