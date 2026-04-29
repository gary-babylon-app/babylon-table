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

import java.util.function.IntPredicate;

import app.babylon.table.selection.Selection;

/**
 * A column of nullable int values with efficient primitive access and
 * predicate-based row selection.
 */
public interface ColumnInt extends Column
{
    /**
     * Column type descriptor for primitive int columns.
     */
    public static final Type TYPE = ColumnTypes.INT;

    /**
     * Builder for nullable int columns.
     */
    public static interface Builder extends Column.Builder
    {
        /**
         * Appends an int value.
         *
         * @param x
         *            the value to append
         * @return this builder
         */
        Builder add(int x);

        default Builder add(CharSequence chars, int start, int length)
        {
            if (chars == null || length == 0)
            {
                return addNull();
            }
            try
            {
                return add(TYPE.getParser().parseInt(chars, start, length));
            }
            catch (RuntimeException e)
            {
                return addNull();
            }
        }

        /**
         * Appends an unset row.
         *
         * @return this builder
         */
        Builder addNull();

        @Override
        ColumnInt build();
    }

    /**
     * Creates an int column builder for the supplied column name.
     *
     * @param name
     *            the column name
     * @return a new int column builder
     */
    public static Builder builder(ColumnName name)
    {
        return new ColumnIntBuilderArray(name);
    }

    @Override
    default public Type getType()
    {
        return TYPE;
    }

    /**
     * Returns the int value at the supplied row.
     *
     * @param i
     *            the zero-based row index
     * @return the int value
     */
    public int get(int i);

    @Override
    public boolean isSet(int i);

    /**
     * Copies the values into the provided array, allocating a new array when
     * necessary.
     *
     * @param x
     *            the destination array, or {@code null}
     * @return an array containing the column values
     */
    public int[] toArray(int[] x);

    /**
     * Returns the maximum set value in the column.
     *
     * @return maximum set value
     */
    default int max()
    {
        if (isEmpty())
        {
            throw new RuntimeException("Can not compute max on column with no values. " + getName());
        }
        if (isConstant())
        {
            return get(0);
        }

        boolean found = false;
        int max = 0;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                int value = get(i);
                if (!found || value > max)
                {
                    max = value;
                    found = true;
                }
            }
        }
        if (!found)
        {
            throw new RuntimeException("Can not compute max on column with no values. " + getName());
        }
        return max;
    }

    /**
     * Returns the minimum set value in the column.
     *
     * @return minimum set value
     */
    default int min()
    {
        if (isEmpty())
        {
            throw new RuntimeException("Can not compute min on column with no values. " + getName());
        }
        if (isConstant())
        {
            return get(0);
        }

        boolean found = false;
        int min = 0;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                int value = get(i);
                if (!found || value < min)
                {
                    min = value;
                    found = true;
                }
            }
        }
        if (!found)
        {
            throw new RuntimeException("Can not compute min on column with no values. " + getName());
        }
        return min;
    }

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
    default public Column selectRow(int i)
    {
        return isSet(i)
                ? new ColumnIntConstant(getName(), get(i), 1, true)
                : ColumnIntConstant.createNull(getName(), 1);
    }

    @Override
    default public String toString(int i)
    {
        return isSet(i) ? Integer.toString(get(i)) : "";
    }

    @Override
    default public void appendTo(int i, StringBuilder out, app.babylon.table.ToStringSettings settings)
    {
        if (isSet(i))
        {
            out.append(get(i));
        }
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
            }
            else
            {
                newBuilder.addNull();
            }
        }
        return newBuilder.build();
    }

    /**
     * Selects rows whose values satisfy the supplied predicate.
     *
     * @param predicate
     *            the predicate to test against each set value
     * @return a selection containing the predicate result for each row
     */
    default Selection select(IntPredicate predicate)
    {
        IntPredicate p = predicate;
        Selection selection = new Selection(this.getName() + " filtered.");
        for (int i = 0; i < this.size(); ++i)
        {
            if (isSet(i))
            {
                selection.add(p.test(get(i)));
            }
            else
            {
                selection.add(false);
            }
        }
        return selection;
    }

}
