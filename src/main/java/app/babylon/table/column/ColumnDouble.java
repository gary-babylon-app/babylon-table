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

import java.util.function.DoublePredicate;

import app.babylon.table.selection.Selection;
import app.babylon.table.selection.RowPredicate;

/**
 * A column of nullable double values with efficient primitive access and
 * predicate-based row selection.
 */
public interface ColumnDouble extends Column
{
    /**
     * Column type descriptor for primitive double columns.
     */
    public static final Type TYPE = ColumnTypes.DOUBLE;

    /**
     * Builder for nullable double columns.
     */
    public static interface Builder extends Column.Builder
    {
        /**
         * Appends a double value.
         *
         * @param x
         *            the value to append
         * @return this builder
         */
        Builder add(double x);

        /**
         * Parses and appends a double value, falling back to null when parsing fails.
         *
         * @param x
         *            the text to parse
         * @return this builder
         */
        default Builder add(CharSequence x)
        {
            if (x == null)
            {
                return addNull();
            }
            try
            {
                return add(TYPE.getParser().parseDouble(x));
            }
            catch (RuntimeException e)
            {
                return addNull();
            }
        }

        /**
         * Parses and appends a double value from a character buffer, falling back to
         * null when parsing fails.
         *
         * @param chars
         *            the source characters
         * @param start
         *            the start offset
         * @param length
         *            the number of characters to parse
         * @return this builder
         */
        default Builder add(CharSequence chars, int start, int length)
        {
            if (chars == null || length == 0)
            {
                return addNull();
            }
            try
            {
                return add(TYPE.getParser().parseDouble(chars, start, length));
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
        ColumnDouble build();
    }

    /**
     * Creates a double column builder for the supplied column name.
     *
     * @param name
     *            the column name
     * @return a new double column builder
     */
    public static Builder builder(ColumnName name)
    {
        return new ColumnDoubleBuilderArray(name);
    }

    @Override
    default public Type getType()
    {
        return TYPE;
    }

    /**
     * Returns the double value at the supplied row.
     *
     * @param i
     *            the zero-based row index
     * @return the double value
     */
    public double get(int i);

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
    public double[] toArray(double[] x);

    /**
     * Returns the maximum set value in the column.
     *
     * @return maximum set value
     */
    default double max()
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
        double max = 0.0;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                double value = get(i);
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
    default double min()
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
        double min = 0.0;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                double value = get(i);
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
            double a = get(i);
            double b = get(j);

            return Double.compare(a, b);
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
                ? new ColumnDoubleConstant(getName(), get(i), 1, true)
                : ColumnDoubleConstant.createNull(getName(), 1);
    }

    @Override
    default public String toString(int i)
    {
        return isSet(i) ? Double.toString(get(i)) : "";
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
    default public ColumnDouble copy(ColumnName x)
    {
        Builder newBuilder = ColumnDouble.builder(x);
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
    default Selection select(DoublePredicate predicate)
    {
        DoublePredicate p = predicate;
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
        double[] parsed = new double[supplied.length];
        for (int i = 0; i < supplied.length; ++i)
        {
            parsed[i] = TYPE.getParser().parseDouble(supplied[i]);
        }
        return predicate(operator, parsed);
    }

    default RowPredicate predicate(Operator operator, double... values)
    {
        double[] supplied = values == null ? new double[0] : java.util.Arrays.copyOf(values, values.length);
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

    private static boolean test(double rowValue, Operator operator, double[] values)
    {
        return switch (operator)
        {
            case EQUAL -> Double.compare(rowValue, values[0]) == 0;
            case NOT_EQUAL -> Double.compare(rowValue, values[0]) != 0;
            case GREATER_THAN -> rowValue > values[0];
            case GREATER_THAN_OR_EQUAL -> rowValue >= values[0];
            case LESS_THAN -> rowValue < values[0];
            case LESS_THAN_OR_EQUAL -> rowValue <= values[0];
            case IN -> contains(rowValue, values);
            case NOT_IN -> !contains(rowValue, values);
        };
    }

    private static boolean contains(double rowValue, double[] values)
    {
        for (double value : values)
        {
            if (Double.compare(rowValue, value) == 0)
            {
                return true;
            }
        }
        return false;
    }

}
