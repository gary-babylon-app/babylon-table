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

import java.util.Arrays;
import java.util.function.Predicate;

import app.babylon.table.ViewIndex;
import app.babylon.table.column.type.TypeParser;
import app.babylon.table.selection.RowPredicate;
import app.babylon.table.selection.Selection;
import app.babylon.text.Sentence.ParseMode;

/**
 * A nullable primitive boolean column backed by compact bit lists for both
 * values and null-state.
 */
public interface ColumnBoolean extends Column
{
    /**
     * Column type descriptor for primitive boolean columns.
     */
    public static final Type TYPE = ColumnTypes.BOOLEAN;

    /**
     * Builder for nullable boolean columns.
     */
    public static interface Builder extends Column.Builder
    {
        /**
         * Appends a boolean value.
         *
         * @param x
         *            the value to append
         * @return this builder
         */
        Builder add(boolean x);

        @Override
        default Builder add(CharSequence chars, int start, int length)
        {
            return add(ParseMode.EXACT, chars, start, length);
        }

        @Override
        default Builder add(ParseMode parseMode, CharSequence chars, int start, int length)
        {
            if (chars == null || length == 0)
            {
                return addNull();
            }
            TypeParser<Boolean> parser = parser();
            if (parseMode == null || parseMode == ParseMode.EXACT)
            {
                try
                {
                    return add(parser.parseBoolean(chars, start, length));
                }
                catch (IllegalArgumentException e)
                {
                    return addNull();
                }
            }
            Boolean value = (parseMode == null ? ParseMode.EXACT : parseMode).apply(parser, chars, start, length);
            return value == null ? addNull() : add(value.booleanValue());
        }

        @SuppressWarnings("unchecked")
        private static TypeParser<Boolean> parser()
        {
            return (TypeParser<Boolean>) TYPE.getParser();
        }

        /**
         * Appends an unset row.
         *
         * @return this builder
         */
        Builder addNull();

        @Override
        ColumnBoolean build();
    }

    /**
     * Creates a boolean column builder for the supplied column name.
     *
     * @param name
     *            the column name
     * @return a new boolean column builder
     */
    public static Builder builder(ColumnName name)
    {
        return new ColumnBooleanBuilderBitSet(name);
    }

    @Override
    default public Type getType()
    {
        return TYPE;
    }

    /**
     * Returns the boolean value at the supplied row.
     *
     * @param i
     *            the zero-based row index
     * @return the boolean value
     */
    public boolean get(int i);

    /**
     * Copies the values into the provided array, allocating a new array when
     * necessary.
     *
     * @param x
     *            the destination array, or {@code null}
     * @return an array containing the column values
     */
    public boolean[] toArray(boolean[] x);

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
            return Boolean.compare(get(i), get(j));
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
                ? new ColumnBooleanConstant(getName(), get(i), 1, true)
                : ColumnBooleanConstant.createNull(getName(), 1);
    }

    @Override
    default public String toString(int i)
    {
        return isSet(i) ? Boolean.toString(get(i)) : "";
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
    default public ColumnBoolean copy(ColumnName x)
    {
        Builder newBuilder = ColumnBoolean.builder(x);
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
    default Selection select(Predicate<Boolean> predicate)
    {
        Predicate<Boolean> p = predicate;
        Selection selection = new Selection(this.getName() + " filtered.");
        for (int i = 0; i < this.size(); ++i)
        {
            selection.add(isSet(i) && p.test(Boolean.valueOf(get(i))));
        }
        return selection;
    }

    @Override
    default RowPredicate predicate(Operator operator, CharSequence... values)
    {
        CharSequence[] supplied = values == null ? new CharSequence[0] : values;
        boolean[] parsed = new boolean[supplied.length];
        for (int i = 0; i < supplied.length; ++i)
        {
            Boolean value = Builder.parser().parse(supplied[i]);
            if (value == null)
            {
                throw new IllegalArgumentException("Could not parse '" + supplied[i] + "' as " + getType() + ".");
            }
            parsed[i] = value.booleanValue();
        }
        return predicate(operator, parsed);
    }

    default RowPredicate predicate(Operator operator, boolean... values)
    {
        boolean[] supplied = values == null ? new boolean[0] : Arrays.copyOf(values, values.length);
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

    private static boolean test(boolean rowValue, Operator operator, boolean[] values)
    {
        return switch (operator)
        {
            case EQUAL -> rowValue == values[0];
            case NOT_EQUAL -> rowValue != values[0];
            case GREATER_THAN -> Boolean.compare(rowValue, values[0]) > 0;
            case GREATER_THAN_OR_EQUAL -> Boolean.compare(rowValue, values[0]) >= 0;
            case LESS_THAN -> Boolean.compare(rowValue, values[0]) < 0;
            case LESS_THAN_OR_EQUAL -> Boolean.compare(rowValue, values[0]) <= 0;
            case IN -> contains(rowValue, values);
            case NOT_IN -> !contains(rowValue, values);
        };
    }

    private static boolean contains(boolean rowValue, boolean[] values)
    {
        for (boolean value : values)
        {
            if (rowValue == value)
            {
                return true;
            }
        }
        return false;
    }

}
