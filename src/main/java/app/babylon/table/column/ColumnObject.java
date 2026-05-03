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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ToStringSettings;
import app.babylon.table.ViewIndex;
import app.babylon.table.column.type.TypeParser;
import app.babylon.table.column.type.TypeWriter;
import app.babylon.table.selection.RowPredicate;
import app.babylon.table.selection.Selection;

/**
 * A column that stores object values, including strings, dates, decimals, and
 * other reference types.
 *
 * @param <T>
 *            the runtime value type stored in the column
 */
public interface ColumnObject<T> extends Column
{
    /**
     * Controls which physical representation should back an object column.
     */
    public enum Mode
    {
        /** Let the builder choose the most suitable representation. */
        AUTO,
        /** Use dictionary encoding when building the column. */
        CATEGORICAL,
        /** Use direct array storage when building the column. */
        ARRAY;

        /**
         * Parses a mode name, defaulting to {@link #AUTO} when the text is blank.
         *
         * @param s
         *            the text to parse
         * @return the resolved mode
         */
        public static Mode parse(String s)
        {
            if (s == null)
            {
                return AUTO;
            }
            return valueOf(s.trim().toUpperCase());
        }
    }

    /**
     * Creates an object column builder using the default storage mode.
     *
     * @param <T>
     *            the value type
     * @param name
     *            the column name
     * @param type
     *            the column type
     * @return a builder for the supplied object type
     */
    public static <T> Builder<T> builder(ColumnName name, Column.Type type)
    {
        return builder(name, type, Mode.AUTO);
    }

    /**
     * Creates an object column builder using the requested storage mode.
     *
     * @param <T>
     *            the value type
     * @param name
     *            the column name
     * @param type
     *            the column type
     * @param mode
     *            the preferred storage strategy
     * @return a builder for the supplied object type
     */
    public static <T> Builder<T> builder(ColumnName name, Column.Type type, Mode mode)
    {
        Column.Type columnType = ArgumentCheck.nonNull(type);
        if (columnType.isPrimitive())
        {
            throw new IllegalArgumentException(
                    "Object builder requires non-primitive type: " + columnType.getValueClass().getName());
        }
        Mode resolvedMode = mode == null ? Mode.AUTO : mode;
        return switch (resolvedMode)
        {
            case AUTO -> new ColumnObjectBuilderComposite<>(name, columnType);
            case ARRAY -> new ColumnObjectBuilderArray<>(name, columnType);
            case CATEGORICAL -> (Builder<T>) ColumnCategorical.<T>builder(name, columnType);
        };
    }

    /**
     * Creates a {@link BigDecimal} column builder.
     *
     * @param name
     *            the column name
     * @return a decimal column builder
     */
    public static Builder<BigDecimal> builderDecimal(ColumnName name)
    {
        return builder(name, ColumnTypes.DECIMAL);
    }

    /**
     * Creates a {@link String} column builder.
     *
     * @param name
     *            the column name
     * @return a string column builder
     */
    public static Builder<String> builder(ColumnName name)
    {
        return builder(name, ColumnTypes.STRING);
    }

    /**
     * Creates a copy of this object column with the supplied column name.
     *
     * @param x
     *            the name to assign to the copy
     * @return an object column containing the same values under the new name
     */
    @Override
    public ColumnObject<T> copy(ColumnName x);

    /**
     * Returns a single-row object column containing the value from the supplied
     * row.
     *
     * @param i
     *            the row to extract
     * @return a single-row object column with the same column name
     */
    @Override
    public ColumnObject<T> selectRow(int i);

    /**
     * Builder for nullable object columns.
     *
     * @param <T>
     *            the value type produced by the builder
     */
    public static interface Builder<T> extends Column.Builder
    {
        @Override
        public ColumnName getName();

        /**
         * Returns the object column type produced by this builder.
         *
         * @return builder type
         */
        public Column.Type getType();

        /**
         * Appends a value to the column.
         *
         * @param x
         *            the value to append, which may be {@code null}
         * @return this builder
         */
        public Builder<T> add(T x);

        @Override
        default Builder<T> add(CharSequence chars, int offset, int length)
        {
            if (chars == null || length == 0)
            {
                return addNull();
            }
            @SuppressWarnings("unchecked")
            TypeParser<T> parser = (TypeParser<T>) getType().getParser();
            T value = parser.parse(chars, offset, length);
            if (value == null)
            {
                addNull();
            }
            else
            {
                add(value);
            }
            return this;
        }

        @Override
        public ColumnObject<T> build();

        /**
         * Builds a column by parsing this builder's source values as the supplied
         * target type.
         *
         * The returned column follows {@code transformedType}, not necessarily this
         * object builder's interface. For example, a string-backed object builder may
         * produce a primitive column when the target type is primitive. If the supplied
         * target type is the same as the builder type, this behaves like
         * {@link #build()} and returns the object column directly.
         *
         * @param transformedType
         *            the target column type
         * @return an immutable column whose logical type is {@code transformedType}
         */
        Column buildAs(Column.Type transformedType);

        /**
         * Appends an unset row.
         *
         * @return this builder
         */
        public default Builder<T> addNull()
        {
            return add(null);
        }

        /**
         * Returns the number of rows currently accumulated by the builder.
         *
         * @return the builder size
         */
        public int size();
    }

    @Override
    public ColumnObject<T> view(ViewIndex rowIndex);

    /**
     * Returns the value at the supplied row.
     *
     * @param i
     *            the zero-based row index
     * @return the row value, or {@code null} when unset
     */
    public T get(int i);

    @Override
    default public boolean isSet(int i)
    {
        return get(i) != null;
    }

    /**
     * Returns the first row value.
     *
     * @return the first row value
     */
    default public T first()
    {
        return get(0);
    }

    /**
     * Returns the last row value.
     *
     * @return the last row value
     */
    default public T last()
    {
        return get(size() - 1);
    }

    /**
     * Returns the maximum set value in the column.
     *
     * @return maximum set value
     */
    default public T max()
    {
        if (size() == 0 || isNoneSet())
        {
            throw new RuntimeException("Can not compute max on column with no values. " + getName());
        }
        if (isConstant())
        {
            return get(0);
        }

        Class<?> valueClass = getType().getValueClass();
        if (!Comparable.class.isAssignableFrom(valueClass))
        {
            throw new RuntimeException("Column values are not Comparable: " + valueClass.getName());
        }

        int maxIndex = -1;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                if (maxIndex < 0 || compare(maxIndex, i) < 0)
                {
                    maxIndex = i;
                }
            }
        }
        return maxIndex < 0 ? null : get(maxIndex);
    }

    /**
     * Returns the minimum set value in the column.
     *
     * @return minimum set value
     */
    default public T min()
    {
        if (size() == 0 || isNoneSet())
        {
            throw new RuntimeException("Can not compute min on column with no values. " + getName());
        }
        if (isConstant())
        {
            return get(0);
        }

        Class<?> valueClass = getType().getValueClass();
        if (!Comparable.class.isAssignableFrom(valueClass))
        {
            throw new RuntimeException("Column values are not Comparable: " + valueClass.getName());
        }

        int minIndex = -1;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                if (minIndex < 0 || compare(minIndex, i) > 0)
                {
                    minIndex = i;
                }
            }
        }
        return minIndex < 0 ? null : get(minIndex);
    }

    /**
     * Appends all set values to the supplied collection.
     *
     * @param x
     *            the destination collection, or {@code null}
     * @return a collection containing all set values in row order
     */
    default public Collection<T> getAll(Collection<T> x)
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

    /**
     * Collects the distinct set values from the column.
     *
     * @param x
     *            the destination set, or {@code null}
     * @return a set containing the unique values
     */
    default public Set<T> getUniques(Set<T> x)
    {
        if (x == null)
        {
            x = new LinkedHashSet<T>();
        }
        return (Set<T>) getAll(x);
    }

    /**
     * Selects rows whose values satisfy the supplied predicate.
     *
     * @param f
     *            the predicate to test against each set value
     * @return a selection containing the predicate result for each row
     */
    default public Selection select(Predicate<T> f)
    {
        Selection selection = new Selection(this.getName() + " filtered.");
        for (int i = 0; i < this.size(); ++i)
        {
            if (isSet(i))
            {
                T t = this.get(i);
                selection.add(f.test(t));
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
        Operator resolvedOperator = ArgumentCheck.nonNull(operator);
        Object[] parsedValues = parsePredicateValues(resolvedOperator, values);
        return row -> isSet(row) && test(get(row), resolvedOperator, parsedValues);
    }

    /**
     * Creates a row predicate from already-typed comparison values.
     * <p>
     * This avoids the text parser used by
     * {@link #predicate(Operator, CharSequence...)} for object types such as
     * {@link BigDecimal}, {@link LocalDate}, or {@link java.util.Currency}. For
     * {@code ColumnObject<String>}, calls with string literals naturally bind to
     * the {@code CharSequence...} overload because {@link String} is a
     * {@link CharSequence}; that is equivalent for string columns because parsing a
     * string value as a string returns the same value.
     *
     * @param operator
     *            comparison operator
     * @param values
     *            already-typed comparison values
     * @return predicate evaluated by row index
     */
    @SuppressWarnings("unchecked")
    default RowPredicate predicate(Operator operator, T... values)
    {
        Operator resolvedOperator = ArgumentCheck.nonNull(operator);
        Object[] supplied = values == null ? new Object[0] : values.clone();
        requireValueCount(resolvedOperator, supplied.length);
        return row -> isSet(row) && test(get(row), resolvedOperator, supplied);
    }

    /**
     * Transforms this column into another object column using the supplied
     * transformer.
     *
     * @param <S>
     *            the transformed value type
     * @param transformer
     *            the row transformation to apply
     * @return a transformed column
     */
    default <S> ColumnObject<S> transform(Transformer<T, S> transformer)
    {
        Transformer<T, S> xform = ArgumentCheck.nonNull(transformer);

        Column.Type targetType = xform.type();
        ColumnName transformedName = xform.columnName() == null ? getName() : xform.columnName();
        ColumnObject.Builder<S> transformed = ColumnObject.builder(transformedName, targetType);
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                S value = xform.apply(get(i));
                transformed.add(value);
            }
            else
            {
                transformed.addNull();
            }
        }
        return transformed.build();
    }

    default public String toString(int i, ToStringSettings settings)
    {
        StringBuilder out = new StringBuilder();
        appendTo(i, out, settings);
        return out.toString();
    }

    @Override
    default public void appendTo(int i, StringBuilder out, ToStringSettings settings)
    {
        if (!isSet(i))
        {
            return;
        }

        T value = get(i);
        if (value == null)
        {
            return;
        }

        Optional<TypeWriter<?>> settingsWriter = settings == null
                ? Optional.empty()
                : settings.getTypeWriter(getType());
        if (settingsWriter.isPresent())
        {
            write(value, settingsWriter.get(), out);
            return;
        }

        Class<?> valueClass = getType().getValueClass();
        if (BigDecimal.class.equals(valueClass))
        {
            BigDecimal decimal = (BigDecimal) value;
            if (settings != null && settings.isStripTrailingZeros())
            {
                decimal = decimal.stripTrailingZeros();
            }
            out.append(decimal.toPlainString());
            return;
        }

        if (LocalDate.class.equals(valueClass))
        {
            LocalDate date = (LocalDate) value;
            out.append(date.format(settings == null
                    ? ToStringSettings.standard().getDateFormatter(null)
                    : settings.getDateFormatter(null)));
            return;
        }

        Optional<TypeWriter<?>> typeWriter = getType().getWriter();
        if (typeWriter.isPresent())
        {
            write(value, typeWriter.get(), out);
            return;
        }
        out.append(value.toString());
    }

    @SuppressWarnings("unchecked")
    private static <T> void write(T value, TypeWriter<?> typeWriter, StringBuilder out)
    {
        ((TypeWriter<T>) typeWriter).write(value, out);
    }

    private Object[] parsePredicateValues(Operator operator, CharSequence... values)
    {
        CharSequence[] supplied = values == null ? new CharSequence[0] : values;
        requireValueCount(operator, supplied.length);
        Object[] parsed = new Object[supplied.length];
        TypeParser<?> parser = getType().getParser();
        for (int i = 0; i < supplied.length; ++i)
        {
            parsed[i] = parser.parse(supplied[i]);
            if (parsed[i] == null)
            {
                throw new IllegalArgumentException("Could not parse '" + supplied[i] + "' as " + getType() + ".");
            }
        }
        return parsed;
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

    @SuppressWarnings(
    {"rawtypes", "unchecked"})
    private static boolean test(Object rowValue, Operator operator, Object[] values)
    {
        return switch (operator)
        {
            case EQUAL -> compare(rowValue, values[0]) == 0;
            case NOT_EQUAL -> compare(rowValue, values[0]) != 0;
            case GREATER_THAN -> compare(rowValue, values[0]) > 0;
            case GREATER_THAN_OR_EQUAL -> compare(rowValue, values[0]) >= 0;
            case LESS_THAN -> compare(rowValue, values[0]) < 0;
            case LESS_THAN_OR_EQUAL -> compare(rowValue, values[0]) <= 0;
            case IN -> contains(rowValue, values);
            case NOT_IN -> !contains(rowValue, values);
        };
    }

    @SuppressWarnings("unchecked")
    private static int compare(Object left, Object right)
    {
        if (left == right)
        {
            return 0;
        }
        if (left == null)
        {
            return -1;
        }
        if (right == null)
        {
            return 1;
        }
        if (left instanceof Comparable comparable)
        {
            return comparable.compareTo(right);
        }
        throw new IllegalArgumentException("Column values are not Comparable: " + left.getClass().getName());
    }

    private static boolean contains(Object rowValue, Object[] values)
    {
        for (Object value : values)
        {
            if (compare(rowValue, value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares a column object with another object using column name, size, and
     * row-by-row value equality.
     *
     * @param <T>
     *            the column value type
     * @param a
     *            the column to compare
     * @param obj
     *            the comparison target
     * @return {@code true} when the objects represent the same column contents
     */
    public static <T> boolean equals(ColumnObject<T> a, Object obj)
    {
        if (a == obj)
        {
            return true;
        }

        if (obj instanceof ColumnObject cd)
        {
            boolean h = a.getName().equals(cd.getName()) && a.size() == cd.size();
            if (h)
            {
                for (int i = 0; i < a.size(); ++i)
                {
                    if (a.isSet(i) != cd.isSet(i))
                    {
                        return false;
                    }

                    if (a.isSet(i) && cd.isSet(i))
                    {
                        T x = a.get(i);
                        Object y = cd.get(i);
                        if (!x.equals(y))
                        {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

}
