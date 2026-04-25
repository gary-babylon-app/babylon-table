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

import java.util.ArrayList;
import java.util.Collection;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ViewIndex;
class ColumnCategoricalConstant<T> implements ColumnCategorical<T>
{
    private final ColumnName columnName;
    private final T value;
    private final int size;
    private final Type type;
    private final boolean isAllSet;
    private final boolean isNoneSet;

    public ColumnCategoricalConstant(ColumnName colName, T value, int size, Column.Type type)
    {
        this.columnName = ArgumentCheck.nonNull(colName);
        this.value = value;
        this.size = size;
        this.type = ArgumentCheck.nonNull(type);
        this.isAllSet = value != null;
        this.isNoneSet = value == null;
    }

    @Override
    public ColumnCategorical<T> copy(ColumnName newCopyName)
    {
        ColumnCategorical<T> newCopy = new ColumnCategoricalConstant<T>(newCopyName, getValue(), size(), this.type);
        return newCopy;
    }

    public T getValue()
    {
        return value;
    }

    @Override
    public ColumnName getName()
    {
        return columnName;
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public Column.Type getType()
    {
        return this.type;
    }

    @Override
    public ColumnCategorical<T> view(ViewIndex rowIndex)
    {
        ArgumentCheck.nonNull(rowIndex);
        if (rowIndex.isAllSet())
        {
            return ColumnCategorical.constant(getName(), getValue(), rowIndex.size(), getType());
        }
        ColumnCategorical.Builder<T> builder = ColumnCategorical.builder(getName(), getType());
        for (int i = 0; i < rowIndex.size(); ++i)
        {
            if (rowIndex.isSet(i) && isSet(0))
            {
                builder.add(getValue());
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    public ColumnCategorical<T> immutable()
    {
        return this;
    }

    @Override
    public boolean equals(Object obj)
    {
        return ColumnObject.equals(this, obj);
    }

    @Override
    public int getCategoryCode(int i)
    {
        return isSet(i) ? 1 : 0;
    }

    @Override
    public T getCategoryValue(int categoryCode)
    {
        if (categoryCode == 0)
        {
            return null;
        }
        return this.value;
    }

    @Override
    public int[] getCategoryCodes(int[] x)
    {
        boolean hasValue = this.size > 0 && isSet(0);
        int requiredSize = hasValue ? 1 : 0;
        if (x == null || x.length != requiredSize)
        {
            x = new int[requiredSize];
        }
        if (hasValue)
        {
            x[0] = 1;
        }
        return x;
    }

    @Override
    public Collection<T> getCategoricalValues(Collection<T> x)
    {
        if (x == null)
        {
            x = new ArrayList<T>();
        }
        if (this.size > 0 && isSet(0))
        {
            x.add(getValue());
        }
        return x;
    }

    @Override
    public boolean isConstant()
    {
        return true;
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
    public T get(int i)
    {
        return this.value;
    }

    @Override
    public <S> ColumnCategorical<S> transform(Transformer<T, S> transformer)
    {
        Transformer<T, S> xform = ArgumentCheck.nonNull(transformer);
        ColumnName transformedName = xform.columnName() == null ? getName() : xform.columnName();
        S transformedValue = isSet(0) ? xform.apply(this.value) : null;
        return ColumnCategorical.constant(transformedName, transformedValue, this.size, xform.type());
    }
}
