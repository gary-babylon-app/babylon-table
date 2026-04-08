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

public class ColumnLongConstant implements ColumnLong
{
    private final ColumnName name;
    private final long value;
    private final int size;
    private final boolean isSet;

    ColumnLongConstant(ColumnName name, long value, int size)
    {
        this(name, value, size, true);
    }

    ColumnLongConstant(ColumnName name, long value, int size, boolean isSet)
    {
        this.name = Objects.requireNonNull(name);
        this.value = value;
        this.size = size;
        this.isSet = isSet;
    }

    @Override
    public long get(int i)
    {
        return value;
    }

    @Override
    public boolean isSet(int i)
    {
        return this.isSet;
    }

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    public long getValue()
    {
        return value;
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public Column view(ViewIndex rowIndex)
    {
        Objects.requireNonNull(rowIndex);
        if (rowIndex.isAllSet())
        {
            return new ColumnLongConstant(this.name, this.value, rowIndex.size(), this.isSet);
        }
        ColumnLong.Builder builder = ColumnLong.builder(this.name);
        for (int i = 0; i < rowIndex.size(); ++i)
        {
            if (rowIndex.isSet(i) && this.isSet)
            {
                builder.add(this.value);
            } else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    @Override
    public long[] toArray(long[] x)
    {
        if (x == null || x.length < size)
        {
            x = new long[size];
        }
        for (int i = 0; i < size; ++i)
        {
            x[i] = value;
        }
        return x;
    }

    @Override
    public boolean isConstant()
    {
        return true;
    }

    @Override
    public boolean isAllSet()
    {
        return this.isSet;
    }

    @Override
    public boolean isNoneSet()
    {
        return !this.isSet;
    }
}
