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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ViewIndexTest
{
    @Test
    public void builderShouldSupportNullEntriesAndChooseCompactBacking()
    {
        ViewIndex list = ViewIndex.builder().add(1).addNull().add(254).build();

        assertEquals(3, list.size());
        assertFalse(list.isAllSet());
        assertTrue(list.isSet(0));
        assertFalse(list.isSet(1));
        assertTrue(list.isSet(2));
        assertEquals(1, list.get(0));
        assertEquals(255, list.get(1));
        assertEquals(254, list.get(2));
        assertEquals("ArrayByte", list.getClass().getSimpleName());
    }

    @Test
    public void toArrayAndCopyShouldBeDefensive()
    {
        ViewIndex list = ViewIndex.builder().add(7).addNull().add(42).build();

        int[] copy = list.toArray(null);
        copy[0] = 99;

        assertArrayEquals(new int[]
        {7, ViewIndex.BYTE_NULL, 42}, list.toArray(null));

        ViewIndex copyList = list.copy();
        assertEquals(list.size(), copyList.size());
        assertFalse(copyList.isAllSet());
        assertEquals(list.get(0), copyList.get(0));
        assertEquals(list.get(1), copyList.get(1));
        assertEquals(list.get(2), copyList.get(2));
        assertFalse(copyList.isSet(1));
    }

    @Test
    public void builderShouldValidateRangeAndSingleUse()
    {
        ViewIndex.Builder builder = ViewIndex.builder();
        builder.add(1).addNull();
        ViewIndex list = builder.build();

        assertFalse(list.isAllSet());
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
        assertThrows(IllegalStateException.class, () -> builder.add(3));
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(RuntimeException.class, () -> ViewIndex.builder().add(-1));
    }

    @Test
    public void isAllSetShouldBeTrueWhenAllRowsAreSet()
    {
        ViewIndex list = ViewIndex.builder().add(1).add(2).build();
        assertTrue(list.isAllSet());
    }
}
