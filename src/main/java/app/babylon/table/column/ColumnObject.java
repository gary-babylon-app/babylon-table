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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.table.ToStringSettings;
import app.babylon.table.ViewIndex;
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
        AUTO, CATEGORICAL, ARRAY;

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
     * @param clazz
     *            the runtime value class
     * @return a builder for the supplied object type
     */
    public static <T> Builder<T> builder(ColumnName name, Class<T> clazz)
    {
        return builder(name, clazz, Mode.AUTO);
    }

    /**
     * Creates an object column builder using the requested storage mode.
     *
     * @param <T>
     *            the value type
     * @param name
     *            the column name
     * @param clazz
     *            the runtime value class
     * @param mode
     *            the preferred storage strategy
     * @return a builder for the supplied object type
     */
    public static <T> Builder<T> builder(ColumnName name, Class<T> clazz, Mode mode)
    {
        Class<T> valueClass = ArgumentCheck.nonNull(clazz);
        if (valueClass.isPrimitive())
        {
            throw new IllegalArgumentException("Object builder requires non-primitive class: " + valueClass.getName());
        }
        Mode resolvedMode = mode == null ? Mode.AUTO : mode;
        return switch (resolvedMode)
        {
            case AUTO -> new ColumnObjectBuilderComposite<>(name, clazz);
            case ARRAY -> new ColumnObjectBuilderArray<>(name, clazz);
            case CATEGORICAL -> (Builder<T>) ColumnCategorical.builder(name, clazz);
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
        return builder(name, BigDecimal.class);
    }

    /**
     * Builder for nullable object columns.
     *
     * @param <T>
     *            the value type produced by the builder
     */
    public static interface Builder<T> extends ColumnBuilder
    {
        @Override
        public ColumnName getName();

        /**
         * Appends a value to the column.
         *
         * @param x
         *            the value to append, which may be {@code null}
         * @return this builder
         */
        public Builder<T> add(T x);

        @Override
        public ColumnObject<T> build();

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
         * Returns the first value currently added to the builder.
         *
         * @return the first value
         */
        public T first();

        /**
         * Returns the most recently added value.
         *
         * @return the last value
         */
        public T last();

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

    /**
     * Indicates whether the column represents the same value for every row.
     *
     * @return {@code true} when the column is constant
     */
    public boolean isConstant();

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

        Class<S> valueClass = xform.valueClass();
        ColumnName transformedName = xform.columnName() == null ? getName() : xform.columnName();
        ColumnObject.Builder<S> transformed = ColumnObject.builder(transformedName, valueClass);
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
        if (!isSet(i))
        {
            return "";
        }

        if (this instanceof ColumnObject<?> co)
        {
            Class<?> valueClass = getType().getValueClass();
            if (BigDecimal.class.equals(valueClass))
            {
                BigDecimal value = (BigDecimal) co.get(i);
                if (value == null)
                {
                    return "";
                }
                if (settings != null && settings.isStripTrailingZeros())
                {
                    value = value.stripTrailingZeros();
                }
                return value.toPlainString();
            }

            if (LocalDate.class.equals(valueClass))
            {
                LocalDate value = (LocalDate) co.get(i);
                if (value == null)
                {
                    return "";
                }
                return value.format(settings == null
                        ? ToStringSettings.standard().getDateFormatter(null)
                        : settings.getDateFormatter(null));
            }
        }
        return toString(i);
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
