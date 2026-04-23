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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RowFixedWidthTest
{
    @Test
    void splitsFieldsByConfiguredWidths()
    {
        RowFixedWidth row = new RowFixedWidth(new int[]
        {4, 3, 2});

        row.append("ABCDXYZ12".toCharArray(), 0, 9).finish();

        assertEquals(3, row.size());
        assertFalse(row.isEmpty());
        assertTrue(row.isSet(0));
        assertTrue(row.isSet(1));
        assertTrue(row.isSet(2));
        assertEquals(0, row.start(0));
        assertEquals(4, row.start(1));
        assertEquals(7, row.start(2));
        assertEquals(4, row.length(0));
        assertEquals(3, row.length(1));
        assertEquals(2, row.length(2));
        assertEquals(9, row.length());
        assertArrayEquals(new String[]
        {"ABCD", "XYZ", "12"}, values(row));
    }

    @Test
    void truncatesMissingFields()
    {
        RowFixedWidth row = new RowFixedWidth(new int[]
        {3, 3, 3});

        row.append("ABCDE".toCharArray(), 0, 5).finish();

        assertArrayEquals(new String[]
        {"ABC", "DE", ""}, values(row));
        assertTrue(row.isSet(0));
        assertTrue(row.isSet(1));
        assertFalse(row.isSet(2));
    }

    @Test
    void clearsExistingContent()
    {
        RowFixedWidth row = new RowFixedWidth(new int[]
        {2, 2});

        row.append("ABCD".toCharArray(), 0, 4).finish();
        row.clear().finish();

        assertEquals(0, row.length());
        assertTrue(row.isEmpty());
        assertFalse(row.isSet(0));
        assertFalse(row.isSet(1));
        assertArrayEquals(new String[]
        {"", ""}, values(row));
    }

    @Test
    void growsCapacityAndCanBuildKeys()
    {
        RowFixedWidth row = new RowFixedWidth(new int[]
        {200, 80});
        char[] chars = repeated('A', 200 + 80);

        row.append(chars, 0, chars.length).finish();

        RowKey key = row.keyOf(new int[]
        {0, 1});

        assertEquals("RowKey2", key.getClass().getSimpleName());
        assertEquals(repeat('A', 200), key.getString(0));
        assertEquals(repeat('A', 80), key.getString(1));
    }

    @Test
    void copiesToRowBuffer()
    {
        RowFixedWidth row = new RowFixedWidth(new int[]
        {2, 3});

        row.append("ABCDE".toCharArray(), 0, 5).finish();

        Row copy = row.copy();

        assertInstanceOf(RowBuffer.class, copy);
        assertArrayEquals(new String[]
        {"AB", "CDE"}, values(copy));
        assertEquals(row.keyOf(new int[]
        {0, 1}), copy.keyOf(new int[]
        {0, 1}));
    }

    private static String[] values(Row row)
    {
        String[] values = new String[row.size()];
        for (int i = 0; i < row.size(); ++i)
        {
            int start = row.start(i);
            values[i] = row.subSequence(start, start + row.length(i)).toString();
        }
        return values;
    }

    private static char[] repeated(char ch, int count)
    {
        char[] chars = new char[count];
        for (int i = 0; i < count; ++i)
        {
            chars[i] = ch;
        }
        return chars;
    }

    private static String repeat(char ch, int count)
    {
        return new String(repeated(ch, count));
    }
}
