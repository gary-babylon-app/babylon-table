/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ByteStringSlicesTest
{
    @Test
    void shouldSelectAndStripFields()
    {
        ByteStringSlices source = slices(" \tAlpha ", "\u00A0Beta\u202F", "Gamma");

        ByteStringSlices selected = source.select(new int[]
        {1, 0}, true);

        assertEquals(2, selected.size());
        assertSame(source.getByteString(), selected.getByteString());
        assertArrayEquals(new String[]
        {"Beta", "Alpha"}, values(selected));
        assertTrue(selected.isSet(0));
        assertTrue(selected.isSet(1));
        assertFalse(selected.isEmpty());
    }

    @Test
    void shouldTreatFullyStrippedFieldsAsUnset()
    {
        ByteStringSlices source = slices("Alpha", "\uFEFF\u200B\u00A0\uFFFD");

        ByteStringSlices selected = source.select(new int[]
        {1}, true);

        assertEquals(1, selected.size());
        assertFalse(selected.isSet(0));
        assertTrue(selected.isEmpty());
        assertNull(selected.decode(0));
    }

    @Test
    void shouldSelectByteIndexesAgainstSharedBackingBytes()
    {
        ByteStringSlices source = slices(" Alpha ", " Beta ", "Gamma");

        ByteStringSlices selected = source.select(new int[]
        {1, 0}, true);

        assertSame(source.getByteString(), selected.getByteString());
        assertArrayEquals(new String[]
        {"Beta", "Alpha"}, values(selected));
        assertEquals(source.start(1) + 1, selected.start(0));
        assertEquals(source.end(1) - 1, selected.end(0));
        assertEquals(source.start(0) + 1, selected.start(1));
        assertEquals(source.end(0) - 1, selected.end(1));
        assertEquals((byte) 'B', selected.byteAt(selected.start(0)));
        assertEquals((byte) 'a', selected.byteAt(selected.end(1) - 1));
    }

    @Test
    void shouldSelectFieldsWithoutCopyingBackingBytes()
    {
        ByteStringSlices source = slices("A", "B", "C");

        ByteStringSlices selected = source.select(new int[]
        {2, 0});

        assertSame(source.getByteString(), selected.getByteString());
        assertArrayEquals(new String[]
        {"C", "A"}, values(selected));
    }

    @Test
    void shouldCompareAndHashBySelectedFieldBytes()
    {
        ByteStringSlices key1 = slices("A", "BC", "tail").select(new int[]
        {0, 1});
        ByteStringSlices key2 = slices("A", "BC", "other").select(new int[]
        {0, 1});
        ByteStringSlices differentBoundary = slices("AB", "C").select(new int[]
        {0, 1});
        ByteStringSlices differentValue = slices("A", "BD").select(new int[]
        {0, 1});

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, differentBoundary);
        assertNotEquals(key1, differentValue);
        assertTrue(key1.compareTo(differentValue) < 0);
        assertTrue(differentValue.compareTo(key1) > 0);
    }

    private static ByteStringSlices slices(String... values)
    {
        ByteStringSlices.Builder builder = new ByteStringSlices.Builder();
        for (String value : values)
        {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            builder.append(bytes, 0, bytes.length);
            builder.finishField();
        }
        return builder.build();
    }

    private static String[] values(ByteStringSlices row)
    {
        String[] values = new String[row.size()];
        for (int i = 0; i < row.size(); ++i)
        {
            values[i] = row.decode(i);
        }
        return values;
    }
}
