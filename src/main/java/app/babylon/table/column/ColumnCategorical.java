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

import app.babylon.table.ViewIndex;
import app.babylon.table.selection.Selection;

/**
 * An object column optimised for repeated values by storing dictionary-encoded
 * categories.
 */
public interface ColumnCategorical<T> extends ColumnObject<T>
{

    public static <T> ColumnCategorical<T> constant(ColumnName name, T value, int size, Class<T> valueClass)
    {
        return new ColumnCategoricalConstant<T>(name, value, size, valueClass);
    }

    public static <T> ColumnCategorical<T> constant(ColumnName name, T value, int size, Type type)
    {
        return new ColumnCategoricalConstant<T>(name, value, size, type);
    }

    public static interface Builder<T> extends ColumnObject.Builder<T>
    {
        @Override
        Builder<T> add(T x);

        @Override
        Builder<T> addNull();

        @Override
        ColumnCategorical<T> build();
    }

    public static <T> Builder<T> builder(ColumnName name, Class<T> clazz)
    {
        Class<T> valueClass = app.babylon.lang.ArgumentCheck.nonNull(clazz);
        if (valueClass.isPrimitive())
        {
            throw new IllegalArgumentException(
                    "Categorical builder requires non-primitive class: " + valueClass.getName());
        }
        Column.Type type = Column.Type.of(valueClass);
        return new ColumnCategoricalBuilderDictionary<T>(name, type);
    }

    @Override
    public T get(int i);

    public int getCategoryCode(int i);

    public T getCategoryValue(int categoryCode);

    public int[] getCategoryCodes(int[] x);

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
        Class<T> valueClass = (Class<T>) getType().getValueClass();
        Builder<T> newBuilder = ColumnCategorical.builder(x, valueClass);
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

    @Override
    default int compare(int i, int j)
    {
        T a = get(i);
        T b = get(j);

        if (a == b)
        {
            return 0;
        }

        if (a instanceof Comparable)
        {
            @SuppressWarnings("unchecked")
            Comparable<T> ac = (Comparable<T>) a;
            return ac.compareTo(b);
        }

        if (a != null)
        {
            if (b != null)
            {
                return a.toString().compareTo(b.toString());
            } else
            {
                return 1;
            }
        } else
        {
            if (b != null)
            {
                return -1;
            }
        }

        throw new IllegalStateException("Unexpected state");
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
            } else
            {
                selection.add(false);
            }
        }
        return selection;
    }

    @Override
    <S> ColumnCategorical<S> transform(Transformer<T, S> transformer);
}
