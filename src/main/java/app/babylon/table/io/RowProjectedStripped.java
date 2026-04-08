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

import java.util.Objects;

final class RowProjectedStripped implements RowProjected
{
    private final int[] projectedIndexes;
    private final int[] starts;
    private final int[] lengths;
    private Row source;

    public RowProjectedStripped(int[] projectedIndexes)
    {
        this.projectedIndexes = Objects.requireNonNull(projectedIndexes, "projectedIndexes must not be null");
        this.starts = new int[projectedIndexes.length];
        this.lengths = new int[projectedIndexes.length];
    }

    @Override
    public RowProjectedStripped with(Row source)
    {
        this.source = Objects.requireNonNull(source, "source must not be null");
        char[] chars = source.chars();
        int sourceFieldCount = source.fieldCount();
        int sourceEnd = source.end();
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
            while (start < end && Character.isWhitespace(chars[start]))
            {
                ++start;
            }
            while (end > start && Character.isWhitespace(chars[end - 1]))
            {
                --end;
            }
            this.starts[i] = start;
            this.lengths[i] = end - start;
        }
        return this;
    }

    @Override
    public int fieldCount()
    {
        return this.projectedIndexes.length;
    }

    @Override
    public char[] chars()
    {
        return source().chars();
    }

    @Override
    public int end()
    {
        return source().end();
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
    public Row copy()
    {
        RowBuffer copy = new RowBuffer();
        char[] chars = chars();
        for (int i = 0; i < fieldCount(); ++i)
        {
            int start = this.starts[i];
            int length = this.lengths[i];
            for (int j = 0; j < length; ++j)
            {
                copy.append(chars[start + j]);
            }
            copy.finishField();
        }
        return copy;
    }

    private Row source()
    {
        return Objects.requireNonNull(this.source, "source row must be set before use");
    }
}
