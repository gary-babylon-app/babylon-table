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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SortIntTest
{
    @Test
    public void stableSortShouldMatchJavaListSortNaturalOrder()
    {
        int[] source = new int[]{5, -1, 3, 3, 0, -7, 9, -1, 4, 4, 2, -7};
        int[] actual = Arrays.copyOf(source, source.length);
        SortInt.stableSort(actual, Integer::compare);

        List<Integer> expectedList = new ArrayList<>();
        for (int value : source)
        {
            expectedList.add(Integer.valueOf(value));
        }
        expectedList.sort(Integer::compare);

        int[] expected = new int[expectedList.size()];
        for (int i = 0; i < expected.length; ++i)
        {
            expected[i] = expectedList.get(i).intValue();
        }

        assertArrayEquals(expected, actual);
    }

    @Test
    public void stableSortShouldMatchJavaListSortReverseOrder()
    {
        int[] source = new int[]{5, -1, 3, 3, 0, -7, 9, -1, 4, 4, 2, -7};
        int[] actual = Arrays.copyOf(source, source.length);
        SortInt.stableSort(actual, (a, b) -> Integer.compare(b, a));

        List<Integer> expectedList = new ArrayList<>();
        for (int value : source)
        {
            expectedList.add(Integer.valueOf(value));
        }
        expectedList.sort((a, b) -> Integer.compare(b.intValue(), a.intValue()));

        int[] expected = new int[expectedList.size()];
        for (int i = 0; i < expected.length; ++i)
        {
            expected[i] = expectedList.get(i).intValue();
        }

        assertArrayEquals(expected, actual);
    }

    @Test
    public void stableSortShouldPreserveOrderForEqualKeys()
    {
        int[] source = new int[]{2, 4, 1, 3, 6, 8, 5, 7};
        int[] actual = Arrays.copyOf(source, source.length);

        ComparatorInt parityComparator = (a, b) -> Integer.compare(a & 1, b & 1);
        SortInt.stableSort(actual, parityComparator);

        assertArrayEquals(new int[]{2, 4, 6, 8, 1, 3, 5, 7}, actual);
    }
}
