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

import java.util.function.LongPredicate;

import app.babylon.table.selection.Selection;
import app.babylon.table.selection.RowPredicate;

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

    /**
     * Returns the maximum set value in the column.
     *
     * @return maximum set value
     */
    default long max()
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
        long max = 0L;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                long value = get(i);
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
    default long min()
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
        long min = 0L;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                long value = get(i);
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
    default public Column selectRow(int i)
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
    default public void appendTo(int i, StringBuilder out, app.babylon.table.ToStringSettings settings)
    {
        if (isSet(i))
        {
            out.append(get(i));
        }
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

    @Override
    default RowPredicate predicate(Operator operator, CharSequence... values)
    {
        CharSequence[] supplied = values == null ? new CharSequence[0] : values;
        long[] parsed = new long[supplied.length];
        for (int i = 0; i < supplied.length; ++i)
        {
            parsed[i] = TYPE.getParser().parseLong(supplied[i]);
        }
        return predicate(operator, parsed);
    }

    default RowPredicate predicate(Operator operator, long... values)
    {
        long[] supplied = values == null ? new long[0] : java.util.Arrays.copyOf(values, values.length);
        requireValueCount(operator, supplied.length);
        return row -> isSet(row) && test(get(row), operator, supplied);
    }

    private static void requireValueCount(Operator operator, int count)
    {
        if (operator == Operator.IN || operator == Operator.NOT_IN)
        {
            if (count == 0)
            {
                throw new IllegalArgumentException(operator + " requires at least one value.");
            }
            return;
        }
        if (count != 1)
        {
            throw new IllegalArgumentException(operator + " requires exactly one value.");
        }
    }

    private static boolean test(long rowValue, Operator operator, long[] values)
    {
        return switch (operator)
        {
            case EQUAL -> rowValue == values[0];
            case NOT_EQUAL -> rowValue != values[0];
            case GREATER_THAN -> rowValue > values[0];
            case GREATER_THAN_OR_EQUAL -> rowValue >= values[0];
            case LESS_THAN -> rowValue < values[0];
            case LESS_THAN_OR_EQUAL -> rowValue <= values[0];
            case IN -> contains(rowValue, values);
            case NOT_IN -> !contains(rowValue, values);
        };
    }

    private static boolean contains(long rowValue, long[] values)
    {
        for (long value : values)
        {
            if (rowValue == value)
            {
                return true;
            }
        }
        return false;
    }

}
