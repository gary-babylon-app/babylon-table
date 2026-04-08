/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.grouping;

import java.util.Arrays;

final class GroupCategoryCode
{
    private final int[] values;

    private GroupCategoryCode(int[] values)
    {
        this.values = Arrays.copyOf(values, values.length);
    }

    static GroupCategoryCode of(int value0)
    {
        return new GroupCategoryCode(new int[]
        {value0});
    }

    static GroupCategoryCode of(int value0, int value1)
    {
        return new GroupCategoryCode(new int[]
        {value0, value1});
    }

    static GroupCategoryCode of(int value0, int value1, int value2)
    {
        return new GroupCategoryCode(new int[]
        {value0, value1, value2});
    }

    static GroupCategoryCode of(int[] values)
    {
        return new GroupCategoryCode(app.babylon.lang.ArgumentCheck.nonNull(values));
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(this.values);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof GroupCategoryCode that)
        {
            return Arrays.equals(this.values, that.values);
        }
        return false;
    }

    @Override
    public String toString()
    {
        return Arrays.toString(this.values);
    }
}
