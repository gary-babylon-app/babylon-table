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

import app.babylon.table.ViewIndex;

class ColumnCategoricalView<T> extends ColumnObjectView<T> implements ColumnCategorical<T>
{
    private final int[] categoryCodes;

    ColumnCategoricalView(ColumnCategorical<T> original, ViewIndex rowIndex)
    {
        super(original, rowIndex);
        boolean[] isUsed = new boolean[maximumCode(original.getCategoryCodes(null)) + 1];
        for (int i = 0; i < rowIndex.size(); ++i)
        {
            if (rowIndex.isSet(i) && original.isSet(rowIndex.get(i)))
            {
                int catCode = original.getCategoryCode(rowIndex.get(i));
                isUsed[catCode] = true;
            }
        }
        this.categoryCodes = categoryCodesOf(isUsed);
    }

    @Override
    protected ColumnCategorical<T> getOriginal()
    {
        return (ColumnCategorical<T>) super.getOriginal();
    }

    @Override
    public Collection<T> getCategoricalValues(Collection<T> x)
    {
        if (x == null)
        {
            x = new ArrayList<T>();
        }

        for (int code : this.categoryCodes)
        {
            x.add(getOriginal().getCategoryValue(code));
        }
        return x;
    }

    @Override
    public int[] getCategoryCodes(int[] x)
    {
        if (x == null || x.length != this.categoryCodes.length)
        {
            x = new int[this.categoryCodes.length];
        }
        System.arraycopy(this.categoryCodes, 0, x, 0, this.categoryCodes.length);
        return x;
    }

    @Override
    public ColumnCategoricalView<T> view(ViewIndex rowIndex)
    {
        return new ColumnCategoricalView<T>(this, rowIndex);
    }

    @Override
    public boolean equals(Object obj)
    {
        return ColumnObject.equals(this, obj);
    }

    @Override
    public T getCategoryValue(int categoryCode)
    {
        return getOriginal().getCategoryValue(categoryCode);
    }

    @Override
    public int getCategoryCode(int i)
    {
        return isSet(i) ? getOriginal().getCategoryCode(mapRowIndex(i)) : 0;
    }

    @Override
    public Column.Type getType()
    {
        return getOriginal().getType();
    }

    @Override
    public <S> ColumnCategorical<S> transform(Transformer<T, S> transformer)
    {
        return ColumnCategoricalBuilderDictionary.transformPreservingCodes(this, transformer);
    }

    private static int maximumCode(int[] categoryCodes)
    {
        int max = 0;
        for (int code : categoryCodes)
        {
            max = Math.max(max, code);
        }
        return max;
    }

    private static int[] categoryCodesOf(boolean[] isUsed)
    {
        int n = 0;
        for (int i = 1; i < isUsed.length; ++i)
        {
            if (isUsed[i])
            {
                ++n;
            }
        }
        int[] x = new int[n];
        int j = 0;
        for (int i = 1; i < isUsed.length; ++i)
        {
            if (isUsed[i])
            {
                x[j] = i;
                ++j;
            }
        }
        return x;
    }

}
