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

    @Test
    public void builderShouldGrowAndSupportCharRangeValues()
    {
        ViewIndex.Builder builder = ViewIndex.builder();
        for (int i = 0; i < 40; ++i)
        {
            builder.add(i);
        }
        builder.addNull();
        builder.add(40000);

        assertEquals(42, builder.size());
        assertTrue(builder.isSet(0));
        assertFalse(builder.isSet(40));
        assertEquals(40000, builder.get(41));

        ViewIndex list = builder.build();
        int[] target = new int[50];
        int[] values = list.toArray(target);

        assertTrue(values == target);
        assertEquals(42, list.size());
        assertFalse(list.isAllSet());
        assertEquals(0, list.get(0));
        assertEquals(39, list.get(39));
        assertFalse(list.isSet(40));
        assertEquals(ViewIndex.CHAR_NULL, list.get(40));
        assertEquals(40000, list.get(41));
        assertEquals(40000, values[41]);

        ViewIndex copy = list.copy();
        assertEquals(list.size(), copy.size());
        assertFalse(copy.isAllSet());
        assertTrue(copy.isSet(39));
        assertFalse(copy.isSet(40));
        assertTrue(copy.isSet(41));
        assertEquals(40000, copy.get(41));
    }

    @Test
    public void builderShouldGrowWellPastInitialCapacityAndSupportIntRangeValues()
    {
        ViewIndex.Builder builder = ViewIndex.builder();
        for (int i = 0; i < 70000; ++i)
        {
            builder.add(i);
        }
        builder.addNull();
        builder.add(70000);

        assertEquals(70002, builder.size());
        assertTrue(builder.isSet(65535));
        assertFalse(builder.isSet(70000));
        assertEquals(70000, builder.get(70001));

        ViewIndex list = builder.build();
        int[] values = list.toArray(null);

        assertEquals(70002, list.size());
        assertFalse(list.isAllSet());
        assertEquals(0, list.get(0));
        assertEquals(65535, list.get(65535));
        assertEquals(69999, list.get(69999));
        assertFalse(list.isSet(70000));
        assertEquals(ViewIndex.INT_NULL, list.get(70000));
        assertEquals(70000, list.get(70001));
        assertEquals(ViewIndex.INT_NULL, values[70000]);
        assertEquals(70000, values[70001]);

        ViewIndex copy = list.copy();
        assertEquals(list.size(), copy.size());
        assertFalse(copy.isAllSet());
        assertTrue(copy.isSet(65535));
        assertFalse(copy.isSet(70000));
        assertTrue(copy.isSet(70001));
        assertEquals(70000, copy.get(70001));
    }

    @Test
    public void copyShouldPreserveAllSetIndexes()
    {
        ViewIndex list = ViewIndex.builder().addAll(new int[]
        {3, 4, 5, 6, 7}).build();

        ViewIndex copy = list.copy();

        assertTrue(list.isAllSet());
        assertTrue(copy.isAllSet());
        assertTrue(copy.isSet(0));
        assertTrue(copy.isSet(4));
        assertEquals(3, copy.get(0));
        assertEquals(7, copy.get(4));
    }
}
