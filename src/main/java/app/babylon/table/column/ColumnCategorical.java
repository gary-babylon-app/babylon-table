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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ViewIndex;
import app.babylon.table.column.type.TypeParser;
import app.babylon.table.selection.Selection;

/**
 * An object column optimised for repeated values by storing dictionary-encoded
 * categories.
 *
 * @param <T>
 *            the runtime value type stored in the column
 */
public interface ColumnCategorical<T> extends ColumnObject<T>
{

    /**
     * Creates a constant categorical column using an explicit column type.
     *
     * @param <T>
     *            the value type
     * @param name
     *            the column name
     * @param value
     *            the repeated value, or {@code null}
     * @param size
     *            the number of rows
     * @param type
     *            the column type
     * @return a constant categorical column
     */
    public static <T> ColumnCategorical<T> constant(ColumnName name, T value, int size, Type type)
    {
        return new ColumnCategoricalConstant<T>(name, value, size, type);
    }

    /**
     * Builder for dictionary-encoded categorical columns.
     *
     * @param <T>
     *            the value type
     */
    public static interface Builder<T> extends ColumnObject.Builder<T>
    {
        @Override
        Builder<T> add(T x);

        @Override
        Builder<T> addNull();

        @Override
        ColumnCategorical<T> build();

        /**
         * Builds the categorical column by parsing each distinct source value through
         * the supplied target type parser.
         *
         * This is the natural path for building a {@code ColumnCategorical<S>} from a
         * string-like categorical builder when the target parser may still need to
         * construct a {@link String}. The categorical dictionary can be parsed once
         * during materialization, rather than first building an immutable
         * {@code ColumnCategorical<String>} and then transforming that immutable column
         * afterward.
         *
         * This is therefore preferred over {@link #build() build()} followed by
         * {@link ColumnCategorical#transform(Transformer)} when the target values come
         * from parsing the source categorical strings, because the parse work can be
         * done during the build and the intermediate immutable string categorical
         * column can be avoided.
         *
         * If the supplied target type is the same as the builder type, this behaves
         * like {@link #build()} and returns the categorical column directly.
         *
         * @param <S>
         *            the transformed value type
         * @param transformedType
         *            the target column type
         * @return an immutable categorical column of the transformed type
         */
        default <S> ColumnCategorical<S> build(Column.Type transformedType)
        {
            Column.Type targetType = ArgumentCheck.nonNull(transformedType);
            if (targetType.equals(getType()))
            {
                @SuppressWarnings("unchecked")
                ColumnCategorical<S> built = (ColumnCategorical<S>) build();
                return built;
            }
            Class<?> valueClass = getType().getValueClass();
            if (!CharSequence.class.isAssignableFrom(valueClass))
            {
                throw new IllegalStateException(
                        "Categorical parsed build requires CharSequence values, not " + valueClass.getName());
            }
            @SuppressWarnings("unchecked")
            TypeParser<S> parser = (TypeParser<S>) targetType.getParser();
            @SuppressWarnings("unchecked")
            Builder<CharSequence> source = (Builder<CharSequence>) this;
            ColumnCategorical<CharSequence> built = source.build();
            return built.transform(Transformer.of(parser::parse, targetType));
        }
    }

    public static Builder<String> builder(ColumnName name)
    {
        return builder(name, ColumnTypes.STRING);
    }

    public static <T> Builder<T> builder(ColumnName name, Column.Type type)
    {
        Column.Type columnType = ArgumentCheck.nonNull(type);
        if (columnType.isPrimitive())
        {
            throw new IllegalArgumentException(
                    "Categorical builder requires non-primitive type: " + columnType.getValueClass().getName());
        }
        return new ColumnCategoricalBuilderDictionary<T>(name, columnType);
    }

    @Override
    public T get(int i);

    /**
     * Returns the dictionary code stored at the supplied row.
     *
     * @param i
     *            the zero-based row index
     * @return the category code for the row
     */
    public int getCategoryCode(int i);

    /**
     * Resolves a dictionary code to its categorical value.
     *
     * @param categoryCode
     *            the dictionary code
     * @return the value for that code, or {@code null} for the null category
     */
    public T getCategoryValue(int categoryCode);

    /**
     * Returns the distinct category codes used by set values in the column.
     *
     * The null/unset category uses the internal code {@code 0}, but that code is
     * not exposed by this method. Only codes associated with non-null categorical
     * values are returned.
     *
     * @param x
     *            the destination array, or {@code null}
     * @return an array of non-zero category codes for set values
     */
    public int[] getCategoryCodes(int[] x);

    /**
     * Appends the distinct categorical values to the supplied collection.
     *
     * @param x
     *            the destination collection, or {@code null}
     * @return a collection containing the categorical values
     */
    public Collection<T> getCategoricalValues(Collection<T> x);

    @Override
    default Set<T> getUniques(Set<T> x)
    {
        if (x == null)
        {
            x = new LinkedHashSet<T>();
        }
        return (Set<T>) getCategoricalValues(x);
    }

    @Override
    public ColumnCategorical<T> view(ViewIndex rowIndex);

    @Override
    default T max()
    {
        if (isEmpty())
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

        int[] categoryCodes = getCategoryCodes(null);
        T max = null;
        for (int categoryCode : categoryCodes)
        {
            T value = getCategoryValue(categoryCode);
            if (max == null)
            {
                max = value;
            }
            else
            {
                @SuppressWarnings("unchecked")
                Comparable<T> comparable = (Comparable<T>) max;
                if (comparable.compareTo(value) < 0)
                {
                    max = value;
                }
            }
        }
        return max;
    }

    @Override
    default T min()
    {
        if (isEmpty())
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

        int[] categoryCodes = getCategoryCodes(null);
        T min = null;
        for (int categoryCode : categoryCodes)
        {
            T value = getCategoryValue(categoryCode);
            if (min == null)
            {
                min = value;
            }
            else
            {
                @SuppressWarnings("unchecked")
                Comparable<T> comparable = (Comparable<T>) min;
                if (comparable.compareTo(value) > 0)
                {
                    min = value;
                }
            }
        }
        return min;
    }

    @Override
    default String toString(int i)
    {
        return isSet(i) ? get(i).toString() : "";
    }

    @Override
    default Column getAsColumn(int i)
    {
        T t = get(i);
        return ColumnCategorical.constant(getName(), t, 1, getType());
    }

    @Override
    default ColumnCategorical<T> copy(ColumnName x)
    {
        @SuppressWarnings("unchecked")
        Builder<T> newBuilder = ColumnCategorical.builder(x, getType());
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

    @Override
    default int compare(int i, int j)
    {
        T a = get(i);
        T b = get(j);

        if (a == b)
        {
            return 0;
        }

        if (a == null)
        {
            return -1;
        }

        if (b == null)
        {
            return 1;
        }

        if (a instanceof Comparable)
        {
            @SuppressWarnings("unchecked")
            Comparable<T> ac = (Comparable<T>) a;
            return ac.compareTo(b);
        }
        throw new RuntimeException("Column values are not Comparable: " + a.getClass().getName());
    }

    @Override
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
     * Transforms this categorical column into another categorical column.
     *
     * @param <S>
     *            the transformed value type
     * @param transformer
     *            the transformation to apply
     * @return the transformed categorical column
     */
    @Override
    <S> ColumnCategorical<S> transform(Transformer<T, S> transformer);
}
