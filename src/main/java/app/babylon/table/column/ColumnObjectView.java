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

import java.util.ArrayList;
import java.util.Collection;

import app.babylon.table.ViewIndex;

abstract class ColumnObjectView<T> implements ColumnObject<T>
{
    private final ColumnObject<T> original;
    private final ViewIndex rowIndex;
    private final boolean isConstant;
    private final boolean isAllSet;
    private final boolean isNoneSet;

    ColumnObjectView(ColumnObject<T> original, ViewIndex rowIndex)
    {
        this.rowIndex = ArgumentCheck.nonNull(rowIndex);
        this.original = ArgumentCheck.nonNull(original);
        if (this.original.isConstant())
        {
            this.isConstant = true;
        }
        else
        {
            boolean b = true;
            for (int i = 1; i < rowIndex.size(); ++i)
            {
                if (b)
                {
                    boolean currentSet = rowIndex.isSet(i) && original.isSet(rowIndex.get(i));
                    boolean previousSet = rowIndex.isSet(i - 1) && original.isSet(rowIndex.get(i - 1));
                    if (currentSet != previousSet)
                    {
                        b = false;
                        break;
                    }
                    if (currentSet)
                    {
                        T current = original.get(rowIndex.get(i));
                        T previous = original.get(rowIndex.get(i - 1));
                        b = (previous == current || (current != null && current.equals(previous)));
                    }
                    if (!b)
                    {
                        break;
                    }
                }
            }
            this.isConstant = b;
        }
        if (this.original.isNoneSet() || this.rowIndex.size() == 0)
        {
            this.isAllSet = false;
            this.isNoneSet = true;
        }
        else if (this.original.isAllSet() && this.rowIndex.isAllSet())
        {
            this.isAllSet = true;
            this.isNoneSet = false;
        }
        else
        {
            boolean anySet = false;
            boolean anyUnset = false;
            for (int i = 0; i < this.rowIndex.size(); ++i)
            {
                boolean set = this.rowIndex.isSet(i) && this.original.isSet(this.rowIndex.get(i));
                anySet |= set;
                anyUnset |= !set;
                if (anySet && anyUnset)
                {
                    break;
                }
            }
            this.isAllSet = !anyUnset;
            this.isNoneSet = !anySet;
        }
    }

    @Override
    public boolean isConstant()
    {
        return this.isConstant;
    }
    @Override
    public int size()
    {
        return this.rowIndex.size();
    }

    @Override
    public ColumnName getName()
    {
        return original.getName();
    }

    @Override
    public String toString(int i)
    {
        return isSet(i) ? original.toString(this.rowIndex.get(i)) : "";
    }

    @Override
    public T get(int i)
    {
        return isSet(i) ? original.get(this.rowIndex.get(i)) : null;
    }

    protected ColumnObject<T> getOriginal()
    {
        return original;
    }

    protected int mapRowIndex(int i)
    {
        return this.rowIndex.get(i);
    }

    public void add(T x)
    {
        throw new RuntimeException("Cannot add to column view of " + original.getName());
    }

    public void addNull()
    {
        throw new RuntimeException("Cannot add to column view of " + original.getName());
    }

    @Override
    public boolean isSet(int i)
    {
        return this.rowIndex.isSet(i) && this.original.isSet(this.rowIndex.get(i));
    }

    @Override
    public boolean isAllSet()
    {
        return this.isAllSet;
    }

    @Override
    public boolean isNoneSet()
    {
        return this.isNoneSet;
    }
    @Override
    public Collection<T> getAll(Collection<T> x)
    {
        if (x == null)
        {
            x = new ArrayList<>();
        }
        for (int i = 0; i < this.size(); ++i)
        {
            if (isSet(i))
            {
                x.add(get(i));
            }
        }
        return x;
    }

    @Override
    public String toString()
    {
        return Columns.toString(this);
    }

}
