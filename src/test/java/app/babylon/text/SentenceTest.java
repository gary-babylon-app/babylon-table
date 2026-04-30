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

        Integer actual = Sentence.firstIn((s, start, length) -> {
            calls.incrementAndGet();
            assertEquals("abc 123 xyz", s.toString());
            return parseInteger(s, start, length);
        }, "abc 123 xyz");

        assertEquals(123, actual);
        assertEquals(2, calls.get());
    }

    private Integer parseIntegerSlice(CharSequence s, int start, int length)
    {
        return parseInteger(s, start, length);
    }

    private Integer parseInteger(CharSequence s, int start, int length)
    {
        try
        {
            return Integer.valueOf(s.subSequence(start, start + length).toString());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
