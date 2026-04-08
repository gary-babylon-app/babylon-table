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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CategoryCodeListTest
{
    @Test
    public void defaultBuilderShouldBuildIntBackedList()
    {
        CategoryCodeList list = CategoryCodeList.builder().add(3).add(70000).addAll(new int[]
        {9, 11}).build();
        assertEquals(4, list.size());
        assertEquals(3, list.get(0));
        assertEquals(70000, list.get(1));
        assertEquals(11, list.get(3));
        assertEquals("ArrayInt", list.getClass().getSimpleName());
    }

    @Test
    public void builderShouldBuildByteAndCharRangesFromValues()
    {
        CategoryCodeList byteList = CategoryCodeList.builder().add(1).add(255).build();
        CategoryCodeList charList = CategoryCodeList.builder().add(256).add(65535).build();

        assertEquals(2, byteList.size());
        assertEquals(255, byteList.get(1));
        assertEquals(2, charList.size());
        assertEquals(65535, charList.get(1));
        assertEquals("ArrayByte", byteList.getClass().getSimpleName());
        assertEquals("ArrayChar", charList.getClass().getSimpleName());
    }

    @Test
    public void toArrayShouldBeDefensive()
    {
        CategoryCodeList list = CategoryCodeList.builder().addAll(new int[]
        {1, 2, 3}).build();
        int[] copy = list.toArray(null);
        copy[0] = 99;
        assertEquals(1, list.get(0));
        assertArrayEquals(new int[]
        {1, 2, 3}, list.toArray(null));
    }

    @Test
    public void copyShouldBeDefensiveForAllBackings()
    {
        CategoryCodeList byteList = CategoryCodeList.builder().add(1).add(255).build();
        CategoryCodeList charList = CategoryCodeList.builder().add(256).add(65535).build();
        CategoryCodeList intList = CategoryCodeList.builder().add(1).add(70000).build();

        CategoryCodeList byteCopy = byteList.copy();
        CategoryCodeList charCopy = charList.copy();
        CategoryCodeList intCopy = intList.copy();

        assertEquals(byteList.size(), byteCopy.size());
        assertEquals(charList.size(), charCopy.size());
        assertEquals(intList.size(), intCopy.size());
        assertEquals(byteList.get(1), byteCopy.get(1));
        assertEquals(charList.get(1), charCopy.get(1));
        assertEquals(intList.get(1), intCopy.get(1));
        assertEquals("ArrayByte", byteCopy.getClass().getSimpleName());
        assertEquals("ArrayChar", charCopy.getClass().getSimpleName());
        assertEquals("ArrayInt", intCopy.getClass().getSimpleName());
    }

    @Test
    public void builderShouldValidateRangeAndSingleUse()
    {
        CategoryCodeList.Builder builder = CategoryCodeList.builder();
        builder.add(1).add(2);
        CategoryCodeList list = builder.build();

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
        assertThrows(IllegalStateException.class, () -> builder.add(3));
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(RuntimeException.class, () -> CategoryCodeList.builder().add(-1));
    }
}
