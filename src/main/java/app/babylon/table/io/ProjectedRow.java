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

final class ProjectedRow implements RowProjected
{
    private final int[] projectedIndexes;
    private Row source;

    public ProjectedRow(int[] projectedIndexes)
    {
        this.projectedIndexes = app.babylon.lang.ArgumentCheck.nonNull(projectedIndexes,
                "projectedIndexes must not be null");
    }

    @Override
    public ProjectedRow with(Row source)
    {
        this.source = app.babylon.lang.ArgumentCheck.nonNull(source, "source must not be null");
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
        int sourceIndex = sourceIndex(fieldIndex);
        if (sourceIndex >= source().fieldCount())
        {
            return source().end();
        }
        return source().start(sourceIndex);
    }

    @Override
    public int length(int fieldIndex)
    {
        int sourceIndex = sourceIndex(fieldIndex);
        if (sourceIndex >= source().fieldCount())
        {
            return 0;
        }
        return source().length(sourceIndex);
    }

    @Override
    public Row copy()
    {
        RowBuffer copy = new RowBuffer();
        char[] chars = chars();
        for (int i = 0; i < fieldCount(); ++i)
        {
            int start = start(i);
            int length = length(i);
            for (int j = 0; j < length; ++j)
            {
                copy.append(chars[start + j]);
            }
            copy.finishField();
        }
        return copy;
    }

    private int sourceIndex(int fieldIndex)
    {
        return this.projectedIndexes[fieldIndex];
    }

    private Row source()
    {
        return app.babylon.lang.ArgumentCheck.nonNull(this.source, "source row must be set before use");
    }
}
