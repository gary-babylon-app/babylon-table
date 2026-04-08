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

import app.babylon.table.Selection;
import java.util.function.IntPredicate;

/**
 * A column of nullable int values with efficient primitive access and
 * predicate-based row selection.
 */
public interface ColumnInt extends Column
{
    public static final Type TYPE = PrimitiveColumnType.INT;

    public static interface Builder extends ColumnBuilder
    {
        Builder add(int x);

        Builder addNull();

        @Override
        ColumnInt build();
    }

    public static Builder builder(ColumnName name)
    {
        return new ColumnIntBuilderArray(name);
    }

    @Override
    default public Type getType()
    {
        return TYPE;
    }

    public int get(int i);

    @Override
    public boolean isSet(int i);

    public int[] toArray(int[] x);

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
            int a = get(i);
            int b = get(j);

            return Integer.compare(a, b);
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
        return new ColumnIntConstant(getName(), get(i), 1, isSet(i));
    }

    @Override
    default public String toString(int i)
    {
        return isSet(i) ? Integer.toString(get(i)) : "";
    }

    @Override
    default public ColumnInt copy(ColumnName x)
    {
        Builder newBuilder = ColumnInt.builder(x);
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

    default Selection select(IntPredicate predicate)
    {
        IntPredicate p = predicate;
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
