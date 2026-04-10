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

class ColumnDoubleConstant implements ColumnDouble
{
    private final ColumnName name;
    private final double value;
    private final int size;
    private final boolean isSet;

    ColumnDoubleConstant(ColumnName name, double value, int size)
    {
        this(name, value, size, true);
    }

    ColumnDoubleConstant(ColumnName name, double value, int size, boolean isSet)
    {
        this.name = ArgumentCheck.nonNull(name);
        this.value = value;
        this.size = size;
        this.isSet = isSet;
    }

    @Override
    public double get(int i)
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

    public double getValue()
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
        ArgumentCheck.nonNull(rowIndex);
        if (rowIndex.isAllSet())
        {
            return new ColumnDoubleConstant(this.name, this.value, rowIndex.size(), this.isSet);
        }
        ColumnDouble.Builder builder = ColumnDouble.builder(this.name);
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
    public double[] toArray(double[] x)
    {
        if (x == null || x.length < size)
        {
            x = new double[size];
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
