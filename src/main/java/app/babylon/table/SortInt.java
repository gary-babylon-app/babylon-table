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

import java.util.Objects;

import java.util.Arrays;


public final class SortInt
{
    private SortInt()
    {
    }

    public static void stableSort(int[] values, ComparatorInt comparator)
    {
        Objects.requireNonNull(values);
        Objects.requireNonNull(comparator);
        if (values.length < 2)
        {
            return;
        }
        int[] temp = Arrays.copyOf(values, values.length);
        mergeSort(values, temp, 0, values.length, comparator);
    }

    private static void mergeSort(int[] values, int[] temp, int from, int to, ComparatorInt comparator)
    {
        int length = to - from;
        if (length < 2)
        {
            return;
        }
        int mid = from + (length >>> 1);
        mergeSort(values, temp, from, mid, comparator);
        mergeSort(values, temp, mid, to, comparator);

        if (comparator.compare(values[mid - 1], values[mid]) <= 0)
        {
            return;
        }

        System.arraycopy(values, from, temp, from, length);

        int left = from;
        int right = mid;
        int out = from;
        while (left < mid && right < to)
        {
            if (comparator.compare(temp[left], temp[right]) <= 0)
            {
                values[out++] = temp[left++];
            }
            else
            {
                values[out++] = temp[right++];
            }
        }
        while (left < mid)
        {
            values[out++] = temp[left++];
        }
        while (right < to)
        {
            values[out++] = temp[right++];
        }
    }
}
