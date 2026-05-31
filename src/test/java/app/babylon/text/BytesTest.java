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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class BytesTest
{
    @Test
    public void stripxStartAndStripxEndShouldTrimAsciiWhitespace()
    {
        ByteString bytes = bytes("xx \tabc\r\nyy");

        assertEquals(4, Bytes.stripxStart(bytes, 2, 9));
        assertEquals(7, Bytes.stripxEnd(bytes, 2, 9));
    }

    @Test
    public void stripxStartAndStripxEndShouldTrimLatin1NoBreakSpace()
    {
        ByteString bytes = bytes('x', 'x', 0xA0, 'a', 'b', 'c', 0xA0, 'y', 'y');

        assertEquals(3, Bytes.stripxStart(bytes, 2, 7));
        assertEquals(6, Bytes.stripxEnd(bytes, 2, 7));
    }

    @Test
    public void stripxStartAndStripxEndShouldTrimUtf8NoBreakSpace()
    {
        ByteString bytes = bytes("xx\u00A0abc\u00A0yy");

        assertEquals(4, Bytes.stripxStart(bytes, 2, bytes.length() - 2));
        assertEquals(7, Bytes.stripxEnd(bytes, 2, bytes.length() - 2));
    }

    @Test
    public void stripxStartAndStripxEndShouldTrimUtf8NarrowNoBreakSpace()
    {
        ByteString bytes = bytes("xx\u202Fabc\u202Fyy");

        assertEquals(5, Bytes.stripxStart(bytes, 2, bytes.length() - 2));
        assertEquals(8, Bytes.stripxEnd(bytes, 2, bytes.length() - 2));
    }

    @Test
    public void stripxShouldTrimCommonUtf8IngestionCharacters()
    {
        ByteString bytes = bytes("xx\uFEFF\u200B\u200C\u200D\uFFFDabc\uFFFD\u200D\u200C\u200B\uFEFFyy");

        assertEquals(17, Bytes.stripxStart(bytes, 2, bytes.length() - 2));
        assertEquals(20, Bytes.stripxEnd(bytes, 2, bytes.length() - 2));
    }

    @Test
    public void stripxShouldReturnOriginalBoundariesWhenNoStripIsNeeded()
    {
        ByteString bytes = bytes("xxabcyy");

        assertEquals(2, Bytes.stripxStart(bytes, 2, 5));
        assertEquals(5, Bytes.stripxEnd(bytes, 2, 5));
    }

    @Test
    public void isStripxEmptyShouldRecogniseEmptySlices()
    {
        assertTrue(Bytes.isStripxEmpty(null));
        assertTrue(Bytes.isStripxEmpty(bytes("")));
        assertTrue(Bytes.isStripxEmpty(bytes(" \t\n")));
        assertTrue(Bytes.isStripxEmpty(bytes("\uFEFF\u200B\u00A0\uFFFD")));
        assertTrue(Bytes.isStripxEmpty(bytes("xx\u202F\u00A0yy"), 2, 7));

        assertFalse(Bytes.isStripxEmpty(bytes("abc")));
        assertFalse(Bytes.isStripxEmpty(bytes(" abc ")));
    }

    private static ByteString bytes(String value)
    {
        return ByteString.encode(value, StandardCharsets.UTF_8);
    }

    private static ByteString bytes(int... values)
    {
        ByteString.Builder builder = new ByteString.Builder(values.length);
        for (int value : values)
        {
            builder.append((byte) value);
        }
        return builder.build();
    }
}
