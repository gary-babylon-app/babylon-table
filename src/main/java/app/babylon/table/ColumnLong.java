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

import java.util.function.LongPredicate;

/**
 * A column of nullable long values with efficient primitive access and
 * predicate-based row selection.
 */
public interface ColumnLong extends Column
{
    public static final Type TYPE = PrimitiveColumnType.LONG;

    public static interface Builder extends ColumnBuilder
    {
        Builder add(long x);

        Builder addNull();

        @Override
        ColumnLong build();
    }

    public static Builder builder(ColumnName name)
    {
        return new ColumnLongBuilderArray(name);
    }

    public static Builder builder(ColumnName name, int initialSize)
    {
        return new ColumnLongBuilderArray(name, initialSize);
    }

    @Override
    default public Type getType()
    {
        return TYPE;
    }

    public long get(int i);

    @Override
    public boolean isSet(int i);

    public long[] toArray(long[] x);

    @Override
    default int compare(int i, int j)
    {
        if (i == j)
        {
            return 0;
        }
        boolean aSet = isSet(i);
        boolean bSet = isSet(j);
        if (aSet && bSet)
        {
            long a = get(i);
            long b = get(j);

            return Long.compare(a, b);
        }
        if (!aSet && !bSet)
        {
            return 0;
        }
        return aSet ? 1 : -1;
    }

    @Override
    default public Column getAsColumn(int i)
    {
        return new ColumnLongConstant(getName(), get(i), 1, isSet(i));
    }

    @Override
    default public String toString(int i)
    {
        return isSet(i) ? Long.toString(get(i)) : "";
    }

    @Override
    default public ColumnLong copy(ColumnName x)
    {
        Builder newBuilder = ColumnLong.builder(x);
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                newBuilder.add(get(i));
            } else
            {
                newBuilder.addNull();
            }
        }
        return newBuilder.build();
    }

    default Selection select(LongPredicate predicate)
    {
        LongPredicate p = predicate;
        Selection selection = new Selection(this.getName() + " filtered.");
        for (int i = 0; i < this.size(); ++i)
        {
            if (isSet(i))
            {
                selection.add(p.test(get(i)));
            } else
            {
                selection.add(false);
            }
        }
        return selection;
    }

    public boolean isConstant();
}
