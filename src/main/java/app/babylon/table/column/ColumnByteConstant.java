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

class ColumnByteConstant implements ColumnByte
{
    private final ColumnName name;
    private final byte value;
    private final int size;
    private final boolean isSet;

    ColumnByteConstant(ColumnName name, byte value, int size)
    {
        this(name, value, size, true);
    }

    ColumnByteConstant(ColumnName name, byte value, int size, boolean isSet)
    {
        this.name = ArgumentCheck.nonNull(name);
        this.value = value;
        this.size = size;
        this.isSet = isSet;
    }

    public static ColumnByteConstant createNull(ColumnName name, int size)
    {
        return new ColumnByteConstant(name, Byte.MAX_VALUE, size, false);
    }

    public byte getValue()
    {
        return this.value;
    }

    public int getSize()
    {
        return this.size;
    }

    @Override
    public Type getType()
    {
        return ColumnByte.TYPE;
    }

    @Override
    public byte get(int i)
    {
        return this.value;
    }

    @Override
    public byte[] toArray(byte[] x)
    {
        if (x == null || x.length < this.size)
        {
            x = new byte[this.size];
        }
        for (int i = 0; i < this.size; ++i)
        {
            x[i] = this.value;
        }
        return x;
    }

    @Override
    public boolean isSet(int i)
    {
        return this.isSet;
    }

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public boolean isAllSet()
    {
        return this.isSet;
    }

    @Override
    public boolean isNoneSet()
    {
        return !this.isSet;
    }

    @Override
    public ColumnByte copy(ColumnName x)
    {
        return this.isSet ? new ColumnByteConstant(x, this.value, this.size, true) : createNull(x, this.size);
    }

    @Override
    public Column view(ViewIndex rowIndex)
    {
        ArgumentCheck.nonNull(rowIndex);
        if (rowIndex.isAllSet())
        {
            return this.isSet
                    ? new ColumnByteConstant(this.name, this.value, rowIndex.size(), true)
                    : createNull(this.name, rowIndex.size());
        }
        ColumnByte.Builder builder = ColumnByte.builder(this.name);
        for (int i = 0; i < rowIndex.size(); ++i)
        {
            if (rowIndex.isSet(i) && this.isSet)
            {
                builder.add(this.value);
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    @Override
    public boolean isXls()
    {
        return false;
    }

    @Override
    public boolean isXlsx()
    {
        return false;
    }
}
