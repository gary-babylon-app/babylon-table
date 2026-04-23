/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import java.util.Arrays;

final class RowFixedWidth implements Row
{
    private char[] chars;
    private final int[] widths;
    private final int[] starts;
    private final int[] lengths;
    private int end;

    RowFixedWidth(int[] widths)
    {
        this.widths = Arrays.copyOf(widths, widths.length);
        this.starts = new int[widths.length];
        this.lengths = new int[widths.length];
        this.chars = new char[256];
        this.end = 0;
    }

    RowFixedWidth clear()
    {
        this.end = 0;
        return this;
    }

    RowFixedWidth append(char[] source, int offset, int length)
    {
        if (length <= 0)
        {
            return this;
        }
        ensureCharCapacity(this.end + length);
        System.arraycopy(source, offset, this.chars, this.end, length);
        this.end += length;
        return this;
    }

    RowFixedWidth finish()
    {
        int start = 0;
        for (int i = 0; i < this.widths.length; ++i)
        {
            this.starts[i] = start;
            int actualLength = Math.max(0, Math.min(this.widths[i], this.end - start));
            this.lengths[i] = actualLength;
            start += this.widths[i];
        }
        return this;
    }

    @Override
    public int size()
    {
        return this.widths.length;
    }

    @Override
    public boolean isEmpty()
    {
        for (int i = 0; i < this.widths.length; ++i)
        {
            if (isSet(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSet(int fieldIndex)
    {
        return length(fieldIndex) > 0;
    }

    @Override
    public int length()
    {
        return this.end;
    }

    @Override
    public char charAt(int index)
    {
        if (index < 0 || index >= this.end)
        {
            throw new IndexOutOfBoundsException();
        }
        return this.chars[index];
    }

    @Override
    public int start(int fieldIndex)
    {
        return this.starts[fieldIndex];
    }

    @Override
    public int length(int fieldIndex)
    {
        return this.lengths[fieldIndex];
    }

    @Override
    public RowKey keyOf(int[] positions)
    {
        return RowKey.of(this, positions);
    }

    @Override
    public Row copy()
    {
        RowBuffer copy = new RowBuffer(this.end, this.widths.length);
        for (int i = 0; i < this.widths.length; ++i)
        {
            copy.append(this.chars, this.starts[i], this.lengths[i]);
            copy.finishField();
        }
        return copy;
    }

    private void ensureCharCapacity(int minCapacity)
    {
        if (minCapacity <= this.chars.length)
        {
            return;
        }
        this.chars = Arrays.copyOf(this.chars, Math.max(minCapacity, this.chars.length * 2));
    }
}
