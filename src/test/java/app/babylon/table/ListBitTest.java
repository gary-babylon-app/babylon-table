/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ListBitTest
{
    @Test
    public void builderShouldAddAndReadValues()
    {
        ListBit.Builder builder = ListBit.builder();
        builder.add(true).add(false).add(true);

        assertEquals(3, builder.size());
        assertTrue(builder.get(0));
        assertFalse(builder.get(1));
        assertTrue(builder.get(2));
    }

    @Test
    public void getShouldRejectOutOfRangeAccess()
    {
        ListBit.Builder builder = ListBit.builder();
        builder.add(true).add(false);

        ListBit list = builder.build();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @Test
    public void buildShouldCreateImmutableListAndCloseBuilder()
    {
        ListBit.Builder builder = ListBit.builder();
        builder.add(true).add(false).add(true);

        ListBit list = builder.build();
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));

        assertThrows(IllegalStateException.class, () -> builder.add(true));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void copyShouldCloneListBitValues()
    {
        ListBit.Builder builder = ListBit.builder();
        builder.add(true).add(false).add(true).add(false);
        ListBit original = builder.build();

        ListBit copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.size(), copy.size());
        for (int i = 0; i < original.size(); ++i)
        {
            assertEquals(original.get(i), copy.get(i));
        }
    }
}
