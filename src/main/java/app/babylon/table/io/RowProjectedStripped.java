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
    private final int[] ends;
    private Row source;

    public RowProjectedStripped(int[] projectedIndexes)
    {
        this.projectedIndexes = ArgumentCheck.nonNull(projectedIndexes, "projectedIndexes must not be null");
        this.starts = new int[projectedIndexes.length];
        this.ends = new int[projectedIndexes.length];
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
                this.ends[i] = sourceEnd;
                continue;
            }
            int start = source.start(sourceIndex);
            int end = source.end(sourceIndex);
            while (start < end && Character.isWhitespace(source.charAt(start)))
            {
                ++start;
            }
            while (end > start && Character.isWhitespace(source.charAt(end - 1)))
            {
                --end;
            }
            this.starts[i] = start;
            this.ends[i] = end;
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
        return end(fieldIndex) > start(fieldIndex);
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
    public int end(int fieldIndex)
    {
        return this.ends[fieldIndex];
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
            int end = this.ends[i];
            for (int j = start; j < end; ++j)
            {
                copy.append(charAt(j));
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
