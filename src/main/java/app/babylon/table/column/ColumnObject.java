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
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.table.Selection;
import app.babylon.table.ToStringSettings;
import app.babylon.table.ViewIndex;

/**
 * A column that stores object values, including strings, dates, decimals, and
 * other reference types.
 */
public interface ColumnObject<T> extends Column
{
    public enum Mode
    {
        AUTO, CATEGORICAL, ARRAY;

        public static Mode parse(String s)
        {
            if (s == null)
            {
                return AUTO;
            }
            return valueOf(s.trim().toUpperCase());
        }
    }

    public static <T> Builder<T> builder(ColumnName name, Class<T> clazz)
    {
        return builder(name, clazz, Mode.AUTO);
    }

    public static <T> Builder<T> builder(ColumnName name, Class<T> clazz, Mode mode)
    {
        Class<T> valueClass = Objects.requireNonNull(clazz);
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

    public static Builder<BigDecimal> builderDecimal(ColumnName name)
    {
        return builder(name, BigDecimal.class);
    }

    public static interface Builder<T> extends ColumnBuilder
    {
        @Override
        public ColumnName getName();

        public Builder<T> add(T x);

        @Override
        public ColumnObject<T> build();

        public default Builder<T> addNull()
        {
            return add(null);
        }

        public T first();

        public T last();

        public int size();
    }

    @Override
    public ColumnObject<T> view(ViewIndex rowIndex);

    public T get(int i);

    public boolean isConstant();

    @Override
    default public boolean isSet(int i)
    {
        return get(i) != null;
    }

    default public T first()
    {
        return get(0);
    }

    default public T last()
    {
        return get(size() - 1);
    }

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

    default public Set<T> getUniques(Set<T> x)
    {
        if (x == null)
        {
            x = new LinkedHashSet<T>();
        }
        return (Set<T>) getAll(x);
    }

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

    default <S> ColumnObject<S> transform(Transformer<T, S> transformer)
    {
        Transformer<T, S> xform = Objects.requireNonNull(transformer);

        Class<S> valueClass = xform.valueClass();
        ColumnName transformedName = xform.columnName() == null ? getName() : xform.columnName();
        ColumnObject.Builder<S> transformed = ColumnObject.builder(transformedName, valueClass);
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                S value = xform.apply(get(i));
                transformed.add(value);
            } else
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
