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

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ViewIndex;

class ColumnBooleanConstant implements ColumnBoolean
{
    private final ColumnName name;
    private final boolean value;
    private final int size;
    private final boolean isSet;

    ColumnBooleanConstant(ColumnName name, boolean value, int size)
    {
        this(name, value, size, true);
    }

    ColumnBooleanConstant(ColumnName name, boolean value, int size, boolean isSet)
    {
        this.name = ArgumentCheck.nonNull(name);
        this.value = value;
        this.size = size;
        this.isSet = isSet;
    }

    public static ColumnBooleanConstant of(ColumnName name, boolean value, int size)
    {
        return new ColumnBooleanConstant(name, value, size);
    }

    public static ColumnBooleanConstant createNull(ColumnName name, int size)
    {
        return new ColumnBooleanConstant(name, false, size, false);
    }

    @Override
    public boolean get(int i)
    {
        return this.value;
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

    public int getSize()
    {
        return this.size;
    }

    public boolean getValue()
    {
        return this.value;
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public Column view(ViewIndex rowIndex)
    {
        ArgumentCheck.nonNull(rowIndex);
        if (rowIndex.isAllSet())
        {
            return this.isSet
                    ? new ColumnBooleanConstant(this.name, this.value, rowIndex.size(), true)
                    : createNull(this.name, rowIndex.size());
        }
        ColumnBoolean.Builder builder = ColumnBoolean.builder(this.name);
        for (int i = 0; i < rowIndex.size(); ++i)
        {
            if (rowIndex.isSet(i) && this.isSet)
            {
                builder.add(this.value);
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    @Override
    public boolean[] toArray(boolean[] x)
    {
        if (x == null || x.length < this.size)
        {
            x = new boolean[this.size];
        }
        for (int i = 0; i < this.size; ++i)
        {
            x[i] = this.value;
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
