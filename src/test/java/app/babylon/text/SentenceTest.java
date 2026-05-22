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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class SentenceTest
{
    @Test
    public void firstInShouldReturnFirstMatch()
    {
        Integer actual = Sentence.firstIn(this::parseIntegerSlice, "abc 123 xyz 456");
        assertEquals(123, actual);
    }

    @Test
    public void firstInShouldBindSliceParserMethodReferences()
    {
        Currency actual = Sentence.firstIn(Currencys::parse, "pay USD 120 tomorrow");
        assertEquals(Currency.getInstance("USD"), actual);
    }

    @Test
    public void lastInShouldReturnLastMatch()
    {
        Integer actual = Sentence.lastIn(this::parseIntegerSlice, "abc 123 xyz 456");
        assertEquals(456, actual);
    }

    @Test
    public void lastInShouldUseConfiguredSeparator()
    {
        Integer actual = Sentence.lastIn(this::parseIntegerSlice, "abc,123,xyz,456", ',');
        assertEquals(456, actual);
    }

    @Test
    public void onlyInShouldReturnOnlyMatch()
    {
        Integer actual = Sentence.onlyIn(this::parseIntegerSlice, "abc 123 xyz");
        assertEquals(123, actual);
    }

    @Test
    public void onlyInShouldReturnNullWhenNoMatch()
    {
        Integer actual = Sentence.onlyIn(this::parseIntegerSlice, "abc xyz");
        assertNull(actual);
    }

    @Test
    public void onlyInShouldReturnNullWhenMultipleMatches()
    {
        Integer actual = Sentence.onlyIn(this::parseIntegerSlice, "123 abc 456");
        assertNull(actual);
    }

    @Test
    public void onlyInShouldBindSliceParserMethodReferences()
    {
        Currency actual = Sentence.onlyIn(Currencys::parse, "pay USD tomorrow");
        assertEquals(Currency.getInstance("USD"), actual);
    }

    @Test
    public void parserShouldReceiveSourceAndSliceBounds()
    {
        AtomicInteger calls = new AtomicInteger();

        Integer actual = Sentence.firstIn((s, start, end) -> {
            calls.incrementAndGet();
            assertEquals("abc 123 xyz", s.toString());
            return parseInteger(s, start, end);
        }, "abc 123 xyz");

        assertEquals(123, actual);
        assertEquals(2, calls.get());
    }

    @Test
    public void dropFirstWordShouldReturnRemainingSentence()
    {
        assertEquals("Satrix MSCI Emerging Markets ETF 0.9989",
                Sentence.dropFirstWord("Bought Satrix MSCI Emerging Markets ETF 0.9989"));
        assertEquals("Satrix", Sentence.dropFirstWord("  Bought   Satrix  "));
        assertNull(Sentence.dropFirstWord("Bought"));
        assertNull(Sentence.dropFirstWord(" "));
        assertNull(Sentence.dropFirstWord(null));
    }

    @Test
    public void dropLastWordShouldReturnRemainingSentence()
    {
        assertEquals("Bought Satrix MSCI Emerging Markets ETF",
                Sentence.dropLastWord("Bought Satrix MSCI Emerging Markets ETF 0.9989"));
        assertEquals("Bought", Sentence.dropLastWord("  Bought   Satrix  "));
        assertNull(Sentence.dropLastWord("Bought"));
        assertNull(Sentence.dropLastWord(" "));
        assertNull(Sentence.dropLastWord(null));
    }

    private Integer parseIntegerSlice(CharSequence s, int start, int end)
    {
        return parseInteger(s, start, end);
    }

    private Integer parseInteger(CharSequence s, int start, int end)
    {
        try
        {
            return Integer.valueOf(s.subSequence(start, end).toString());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
