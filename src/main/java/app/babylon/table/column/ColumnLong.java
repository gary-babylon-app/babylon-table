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

import app.babylon.table.selection.Selection;
import java.util.function.LongPredicate;

/**
 * A column of nullable long values with efficient primitive access and
 * predicate-based row selection.
 */
public interface ColumnLong extends Column
{
    /**
     * Column type descriptor for primitive long columns.
     */
    public static final Type TYPE = ColumnTypes.LONG;

    /**
     * Builder for nullable long columns.
     */
    public static interface Builder extends Column.Builder
    {
        /**
         * Appends a long value.
         *
         * @param x
         *            the value to append
         * @return this builder
         */
        Builder add(long x);

        default Builder add(CharSequence chars, int start, int length)
        {
            if (chars == null || length == 0)
            {
                return addNull();
            }
            try
            {
                return add(TYPE.getParser().parseLong(chars, start, length));
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
        ColumnLong build();
    }

    /**
     * Creates a long column builder for the supplied column name.
     *
     * @param name
     *            the column name
     * @return a new long column builder
     */
    public static Builder builder(ColumnName name)
    {
        return new ColumnLongBuilderArray(name);
    }

    /**
     * Creates a long column builder with an initial capacity hint.
     *
     * @param name
     *            the column name
     * @param initialSize
     *            the expected number of rows
     * @return a new long column builder
     */
    public static Builder builder(ColumnName name, int initialSize)
    {
        return new ColumnLongBuilderArray(name, initialSize);
    }

    @Override
    default public Type getType()
    {
        return TYPE;
    }

    /**
     * Returns the long value at the supplied row.
     *
     * @param i
     *            the zero-based row index
     * @return the long value
     */
    public long get(int i);

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
        return isSet(i)
                ? new ColumnLongConstant(getName(), get(i), 1, true)
                : ColumnLongConstant.createNull(getName(), 1);
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
    default Selection select(LongPredicate predicate)
    {
        LongPredicate p = predicate;
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

    /**
     * Indicates whether the column represents the same value for every row.
     *
     * @return {@code true} when the column is constant
     */
    public boolean isConstant();
}
