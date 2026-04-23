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

import app.babylon.lang.ArgumentCheck;

final class RowProjectedStripped implements RowProjected
{
    private final int[] projectedIndexes;
    private final int[] starts;
    private final int[] lengths;
    private Row source;

    public RowProjectedStripped(int[] projectedIndexes)
    {
        this.projectedIndexes = ArgumentCheck.nonNull(projectedIndexes, "projectedIndexes must not be null");
        this.starts = new int[projectedIndexes.length];
        this.lengths = new int[projectedIndexes.length];
    }

    @Override
    public RowProjectedStripped with(Row source)
    {
        this.source = ArgumentCheck.nonNull(source, "source must not be null");
        int sourceFieldCount = source.size();
        int sourceEnd = source.length();
        for (int i = 0; i < this.projectedIndexes.length; ++i)
        {
            int sourceIndex = this.projectedIndexes[i];
            if (sourceIndex >= sourceFieldCount)
            {
                this.starts[i] = sourceEnd;
                this.lengths[i] = 0;
                continue;
            }
            int start = source.start(sourceIndex);
            int end = start + source.length(sourceIndex);
            while (start < end && Character.isWhitespace(source.charAt(start)))
            {
                ++start;
            }
            while (end > start && Character.isWhitespace(source.charAt(end - 1)))
            {
                --end;
            }
            this.starts[i] = start;
            this.lengths[i] = end - start;
        }
        return this;
    }

    @Override
    public int size()
    {
        return this.projectedIndexes.length;
    }

    @Override
    public boolean isEmpty()
    {
        for (int i = 0; i < size(); ++i)
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
        return this.lengths[fieldIndex] > 0;
    }

    @Override
    public int length()
    {
        return source().length();
    }

    @Override
    public char charAt(int index)
    {
        return source().charAt(index);
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
        RowBuffer copy = new RowBuffer();
        for (int i = 0; i < size(); ++i)
        {
            int start = this.starts[i];
            int length = this.lengths[i];
            for (int j = 0; j < length; ++j)
            {
                copy.append(charAt(start + j));
            }
            copy.finishField();
        }
        return copy;
    }

    private Row source()
    {
        return ArgumentCheck.nonNull(this.source, "source row must be set before use");
    }
}
