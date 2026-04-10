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

import java.util.Objects;

final class ColumnObjectBuilderArray<T> implements ColumnObject.Builder<T>
{
    private Object[] values;
    private int size;
    private final ColumnName name;
    private final Column.Type type;
    private boolean isConstant;
    private boolean hasAnySet;
    private boolean hasAnyUnset;
    private boolean built;

    ColumnObjectBuilderArray(ColumnName name, Class<T> valueClass)
    {
        this(name, Column.Type.of(ArgumentCheck.nonNull(valueClass)));
    }

    ColumnObjectBuilderArray(ColumnName name, Column.Type type)
    {
        this.name = ArgumentCheck.nonNull(name);
        this.type = ArgumentCheck.nonNull(type);
        this.size = 0;
        this.values = new Object[16];
        this.isConstant = true;
        this.hasAnySet = false;
        this.hasAnyUnset = false;
        this.built = false;
    }

    @SuppressWarnings("unchecked")
    T get(int i)
    {
        ensureActive();
        return (T) this.values[i];
    }

    @Override
    public T last()
    {
        return get(size() - 1);
    }

    @Override
    public T first()
    {
        return get(0);
    }

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    Column.Type getType()
    {
        return this.type;
    }

    @Override
    public int size()
    {
        ensureActive();
        return this.size;
    }

    @Override
    public ColumnObject.Builder<T> add(T value)
    {
        ensureActive();
        if (this.size == this.values.length)
        {
            Object[] grown = new Object[2 * this.size];
            System.arraycopy(this.values, 0, grown, 0, this.size);
            this.values = grown;
        }
        if (this.isConstant && this.size > 0)
        {
            this.isConstant = Objects.equals(value, this.values[this.size - 1]);
        }
        if (value == null)
        {
            this.hasAnyUnset = true;
        }
        else
        {
            this.hasAnySet = true;
        }
        this.values[this.size] = value;
        ++this.size;
        return this;
    }

    boolean activeIsAllSet()
    {
        ensureActive();
        return !this.hasAnyUnset;
    }

    boolean activeIsNoneSet()
    {
        ensureActive();
        return !this.hasAnySet;
    }

    @Override
    public ColumnObject<T> build()
    {
        int n = activeSize();
        @SuppressWarnings("unchecked")
        T constantValue = n == 0 ? null : (T) this.values[0];
        @SuppressWarnings("unchecked")
        Class<T> valueClass = (Class<T>) this.type.getValueClass();
        if (n > 0 && activeIsConstant())
        {
            detachValues();
            return ColumnCategorical.constant(getName(), constantValue, n, valueClass);
        }
        return new ColumnObjectArray<>(this);
    }

    @Override
    public String toString()
    {
        ensureActive();
        StringBuilder builder = new StringBuilder();
        builder.append(this.name.getCanonical());
        for (int i = 0; i < Math.max(Math.min(2, this.size - 1), 0); ++i)
        {
            if (i != 0)
            {
                builder.append(", ");
            }
            else
            {
                builder.append("[");
            }
            builder.append(toString(i));
        }
        if (this.size > 1)
        {
            builder.append(", ... ,");
            builder.append(toString(this.size - 1));
            builder.append("]");
        }
        return builder.toString();
    }

    private String toString(int i)
    {
        T value = get(i);
        return value == null ? "" : value.toString();
    }

    boolean activeIsConstant()
    {
        ensureActive();
        return this.isConstant;
    }

    int activeSize()
    {
        ensureActive();
        return this.size;
    }

    Object[] detachValues()
    {
        ensureActive();
        Object[] detached = this.values;
        this.values = null;
        this.size = 0;
        this.built = true;
        return detached;
    }

    private void ensureActive()
    {
        if (this.built)
        {
            throw new IllegalStateException("Builder has already transferred ownership: " + this.name);
        }
    }
}
