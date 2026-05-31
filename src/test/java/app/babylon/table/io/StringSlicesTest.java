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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class StringSlicesTest
{
    @Test
    void shouldSelectAndStripFields()
    {
        StringSlices source = slices(" \tAlpha ", "\u00A0Beta\u202F", "Gamma");

        StringSlices selected = source.select(new int[]
        {1, 0}, true);

        assertEquals(2, selected.size());
        assertSame(source.getString(), selected.getString());
        assertArrayEquals(new String[]
        {"Beta", "Alpha"}, values(selected));
        assertTrue(selected.isSet(0));
        assertTrue(selected.isSet(1));
        assertFalse(selected.isEmpty());
    }

    @Test
    void shouldTreatFullyStrippedFieldsAsUnset()
    {
        StringSlices source = slices("Alpha", "\uFEFF\u200B\u00A0\uFFFD");

        StringSlices selected = source.select(new int[]
        {1}, true);

        assertEquals(1, selected.size());
        assertFalse(selected.isSet(0));
        assertTrue(selected.isEmpty());
        assertNull(selected.getString(0));
    }

    @Test
    void shouldSelectCharacterIndexesAgainstSharedBackingString()
    {
        StringSlices source = slices(" Alpha ", " Beta ", "Gamma");

        StringSlices selected = source.select(new int[]
        {1, 0}, true);

        assertSame(source.getString(), selected.getString());
        assertArrayEquals(new String[]
        {"Beta", "Alpha"}, values(selected));
        assertEquals(source.start(1) + 1, selected.start(0));
        assertEquals(source.end(1) - 1, selected.end(0));
        assertEquals(source.start(0) + 1, selected.start(1));
        assertEquals(source.end(0) - 1, selected.end(1));
        assertEquals('B', selected.charAt(selected.start(0)));
        assertEquals('a', selected.charAt(selected.end(1) - 1));
    }

    @Test
    void copyShouldMaterialiseSelectedFieldsOnly()
    {
        StringSlices source = slices(" Alpha ", " Beta ");
        StringSlices selected = source.select(new int[]
        {1, 0}, true);

        StringSlices copy = selected.copy();

        assertArrayEquals(new String[]
        {"Beta", "Alpha"}, values(copy));
        assertEquals(9, copy.length());
    }

    @Test
    void shouldParseDecimalFromBackingString()
    {
        StringSlices source = slices("  123.4500  ");
        StringSlices selected = source.select(new int[]
        {0}, true);

        assertEquals(new BigDecimal("123.45"), selected.parseDecimal(selected.start(0), selected.end(0)));
    }

    private static StringSlices slices(String... values)
    {
        StringSlices.Builder builder = new StringSlices.Builder();
        for (String value : values)
        {
            builder.append(value);
            builder.finishField();
        }
        return builder.build();
    }

    private static String[] values(StringSlices row)
    {
        String[] values = new String[row.size()];
        for (int i = 0; i < row.size(); ++i)
        {
            values[i] = row.getString(i);
        }
        return values;
    }
}
