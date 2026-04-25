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

import java.util.Objects;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ViewIndex;
import app.babylon.table.column.type.TypeParser;

class ColumnObjectArray<T> implements ColumnObject<T>
{
    private final Object[] values;
    private final ColumnName name;
    private final Column.Type type;
    private final boolean isConstant;
    private final boolean isAllSet;
    private final boolean isNoneSet;
    private final int size;

    ColumnObjectArray(ColumnObjectBuilderArray<T> builder)
    {
        this.name = ArgumentCheck.nonNull(builder.getName());
        this.type = ArgumentCheck.nonNull(builder.getType());
        this.size = ArgumentCheck.nonNegative(builder.activeSize());
        this.isConstant = builder.activeIsConstant();
        this.isAllSet = builder.activeIsAllSet();
        this.isNoneSet = builder.activeIsNoneSet();
        this.values = ArgumentCheck.nonNull(builder.detachValues());
        if (this.size > this.values.length)
        {
            throw new IllegalArgumentException("Size exceeds values length.");
        }
    }

    ColumnObjectArray(ColumnObjectBuilderArray<?> builder, Column.Type transformedType)
    {
        this.name = ArgumentCheck.nonNull(builder.getName());
        this.type = ArgumentCheck.nonNull(transformedType);
        this.size = ArgumentCheck.nonNegative(builder.activeSize());
        @SuppressWarnings("unchecked")
        TypeParser<T> parser = (TypeParser<T>) this.type.getParser();
        this.values = ArgumentCheck.nonNull(builder.detachValues());

        boolean transformedConstant = true;
        boolean hasAnySet = false;
        boolean hasAnyUnset = false;
        T previousValue = null;
        boolean previousAssigned = false;

        for (int i = 0; i < this.size; ++i)
        {
            CharSequence sourceValue = (CharSequence) this.values[i];
            T transformed = sourceValue == null ? null : parser.parse(sourceValue);
            this.values[i] = transformed;
            if (transformed == null)
            {
                hasAnyUnset = true;
            }
            else
            {
                hasAnySet = true;
            }
            if (transformedConstant && previousAssigned)
            {
                transformedConstant = Objects.equals(previousValue, transformed);
            }
            previousValue = transformed;
            previousAssigned = true;
        }
        if (this.size > this.values.length)
        {
            throw new IllegalArgumentException("Size exceeds values length.");
        }
        this.isConstant = transformedConstant;
        this.isAllSet = !hasAnyUnset;
        this.isNoneSet = !hasAnySet;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int i)
    {
        return (T) this.values[i];
    }

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    @Override
    public Column.Type getType()
    {
        return this.type;
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public boolean isSet(int i)
    {
        return get(i) != null;
    }

    @Override
    public boolean isAllSet()
    {
        return this.isAllSet;
    }

    @Override
    public boolean isNoneSet()
    {
        return this.isNoneSet;
    }

    @Override
    public String toString()
    {
        return Columns.toString(this);
    }

    @Override
    public String toString(int i)
    {
        T value = get(i);
        return value == null ? "" : value.toString();
    }

    @Override
    public boolean isConstant()
    {
        return this.isConstant;
    }

    @Override
    public int compare(int i, int j)
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
        if (a instanceof Comparable<?> comparable)
        {
            @SuppressWarnings("unchecked")
            Comparable<T> typedComparable = (Comparable<T>) comparable;
            return typedComparable.compareTo(b);
        }
        throw new RuntimeException("Column values are not Comparable: " + a.getClass().getName());
    }

    @Override
    public ColumnObject<T> view(ViewIndex rowIndex)
    {
        return new ColumnObjectArrayView<>(this, rowIndex);
    }

    @Override
    public ColumnCategorical<T> selectRow(int i)
    {
        return ColumnCategorical.constant(getName(), get(i), 1, this.type);
    }

    @Override
    public ColumnObject<T> copy(ColumnName x)
    {
        ColumnObject.Builder<T> copyBuilder = new ColumnObjectBuilderArray<>(x, this.type);
        for (int i = 0; i < size(); ++i)
        {
            copyBuilder.add(get(i));
        }
        return copyBuilder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        return ColumnObject.equals(this, obj);
    }

    private static final class ColumnObjectArrayView<T> extends ColumnObjectView<T>
    {
        private final Column.Type type;

        private ColumnObjectArrayView(ColumnObject<T> original, ViewIndex rowIndex)
        {
            super(original, rowIndex);
            this.type = original.getType();
        }

        @Override
        public Column.Type getType()
        {
            return this.type;
        }

        @Override
        public int compare(int i, int j)
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
            if (a instanceof Comparable<?> comparable)
            {
                @SuppressWarnings("unchecked")
                Comparable<T> typedComparable = (Comparable<T>) comparable;
                return typedComparable.compareTo(b);
            }
            throw new RuntimeException("Column values are not Comparable: " + a.getClass().getName());
        }

        @Override
        public ColumnObject<T> view(ViewIndex rowIndex)
        {
            return new ColumnObjectArrayView<>(this, rowIndex);
        }

        @Override
        public ColumnCategorical<T> selectRow(int i)
        {
            return ColumnCategorical.constant(getName(), get(i), 1, getType());
        }

        @Override
        public ColumnObject<T> copy(ColumnName x)
        {
            ColumnObject.Builder<T> copyBuilder = new ColumnObjectBuilderArray<>(x, getType());
            for (int i = 0; i < size(); ++i)
            {
                copyBuilder.add(get(i));
            }
            return copyBuilder.build();
        }
    }
}
