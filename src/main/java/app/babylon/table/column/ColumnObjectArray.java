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

import app.babylon.table.ViewIndex;

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
        this(builder.getName(), builder.getType(), builder.activeSize(), builder.activeIsConstant(),
                builder.activeIsAllSet(), builder.activeIsNoneSet(), builder.detachValues());
    }

    ColumnObjectArray(ColumnName name, Column.Type type, int size, boolean isConstant, boolean isAllSet,
            boolean isNoneSet, Object[] values)
    {
        this.name = ArgumentCheck.nonNull(name);
        this.type = ArgumentCheck.nonNull(type);
        this.values = ArgumentCheck.nonNull(values);
        this.size = ArgumentCheck.nonNegative(size);
        if (size > values.length)
        {
            throw new IllegalArgumentException("Size exceeds values length.");
        }
        this.isConstant = isConstant;
        this.isAllSet = isAllSet;
        this.isNoneSet = isNoneSet;
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
    public Column getAsColumn(int i)
    {
        return ColumnCategorical.constant(getName(), get(i), 1, this.type);
    }

    @Override
    public Column copy(ColumnName x)
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
        public Column getAsColumn(int i)
        {
            return ColumnCategorical.constant(getName(), get(i), 1, getType());
        }

        @Override
        public Column copy(ColumnName x)
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
