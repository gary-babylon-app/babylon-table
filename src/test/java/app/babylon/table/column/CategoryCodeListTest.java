/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CategoryCodeListTest
{
    @Test
    public void builderShouldTrackSizeAndValuesBeforeBuild()
    {
        CategoryCodeList.Builder builder = CategoryCodeList.builder();

        builder.add(3).addAll(new int[]
        {9, 11});

        assertEquals(3, builder.size());
        assertEquals(3, builder.get(0));
        assertEquals(9, builder.get(1));
        assertEquals(11, builder.get(2));
    }

    @Test
    public void byteRangeListShouldSupportToArrayAndCopy()
    {
        CategoryCodeList list = CategoryCodeList.builder().add(1).add(255).build();

        int[] target = new int[4];
        int[] values = list.toArray(target);
        CategoryCodeList copy = list.copy();

        assertSame(target, values);
        assertArrayEquals(new int[]
        {1, 255, 0, 0}, values);
        assertEquals(2, copy.size());
        assertEquals(1, copy.get(0));
        assertEquals(255, copy.get(1));
        assertNotSame(list, copy);
        assertArrayEquals(new int[]
        {1, 255}, copy.toArray(null));
    }

    @Test
    public void charRangeListShouldSupportToArrayAndCopy()
    {
        CategoryCodeList list = CategoryCodeList.builder().add(256).add(65535).build();

        int[] target = new int[3];
        int[] values = list.toArray(target);
        CategoryCodeList copy = list.copy();

        assertSame(target, values);
        assertArrayEquals(new int[]
        {256, 65535, 0}, values);
        assertEquals(2, copy.size());
        assertEquals(256, copy.get(0));
        assertEquals(65535, copy.get(1));
        assertNotSame(list, copy);
        assertArrayEquals(new int[]
        {256, 65535}, copy.toArray(null));
    }

    @Test
    public void intRangeListShouldSupportToArrayAndCopy()
    {
        CategoryCodeList list = CategoryCodeList.builder().add(3).add(70000).addAll(new int[]
        {9, 11}).build();

        int[] target = new int[6];
        int[] values = list.toArray(target);
        CategoryCodeList copy = list.copy();

        assertSame(target, values);
        assertArrayEquals(new int[]
        {3, 70000, 9, 11, 0, 0}, values);
        assertEquals(4, copy.size());
        assertEquals(3, copy.get(0));
        assertEquals(70000, copy.get(1));
        assertEquals(11, copy.get(3));
        assertNotSame(list, copy);
        assertArrayEquals(new int[]
        {3, 70000, 9, 11}, copy.toArray(null));
    }

    @Test
    public void toArrayShouldReturnANewArrayWhenTargetIsTooSmall()
    {
        CategoryCodeList byteList = CategoryCodeList.builder().add(1).add(2).build();
        CategoryCodeList charList = CategoryCodeList.builder().add(256).add(257).build();
        CategoryCodeList intList = CategoryCodeList.builder().add(70000).add(70001).build();

        int[] tooSmall = new int[1];

        int[] byteValues = byteList.toArray(tooSmall);
        int[] charValues = charList.toArray(tooSmall);
        int[] intValues = intList.toArray(tooSmall);

        assertNotSame(tooSmall, byteValues);
        assertNotSame(tooSmall, charValues);
        assertNotSame(tooSmall, intValues);
        assertArrayEquals(new int[]
        {1, 2}, byteValues);
        assertArrayEquals(new int[]
        {256, 257}, charValues);
        assertArrayEquals(new int[]
        {70000, 70001}, intValues);
    }

    @Test
    public void toArrayAndCopyShouldBeDefensive()
    {
        CategoryCodeList list = CategoryCodeList.builder().addAll(new int[]
        {1, 2, 3}).build();

        int[] copy = list.toArray(null);
        copy[0] = 99;

        assertEquals(1, list.get(0));
        assertArrayEquals(new int[]
        {1, 2, 3}, list.toArray(null));
        assertArrayEquals(new int[]
        {1, 2, 3}, list.copy().toArray(null));
    }

    @Test
    public void builderShouldAcceptNullAndEmptyArraysInAddAll()
    {
        CategoryCodeList.Builder builder = CategoryCodeList.builder();

        builder.addAll(null).addAll(new int[0]).add(5);

        assertEquals(1, builder.size());
        assertEquals(5, builder.get(0));
    }

    @Test
    public void builderAndListShouldValidateBoundsAndSingleUse()
    {
        assertThrows(IndexOutOfBoundsException.class, () -> CategoryCodeList.builder().get(0));

        CategoryCodeList.Builder builder = CategoryCodeList.builder();
        builder.add(1).add(2);
        CategoryCodeList list = builder.build();

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
        assertThrows(IllegalStateException.class, () -> builder.add(3));
        assertThrows(IllegalStateException.class, () -> builder.addAll(new int[]
        {4}));
        assertThrows(IllegalStateException.class, builder::size);
        assertThrows(IllegalStateException.class, () -> builder.get(-1));
        assertThrows(IllegalStateException.class, () -> builder.get(0));
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(RuntimeException.class, () -> CategoryCodeList.builder().add(-1));
    }
}
