/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.sorting;

/**
 * Compares two primitive int values without boxing.
 */
@FunctionalInterface
public interface ComparatorInt
{
    /**
     * Compares two primitive ints.
     *
     * @param a
     *            first value
     * @param b
     *            second value
     * @return comparison result
     */
    int compare(int a, int b);
}
