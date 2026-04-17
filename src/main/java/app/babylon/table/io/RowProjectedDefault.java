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

final class RowProjectedDefault implements RowProjected
{
    private final int[] projectedIndexes;
    private Row source;

    public RowProjectedDefault(int[] projectedIndexes)
    {
        this.projectedIndexes = ArgumentCheck.nonNull(projectedIndexes, "projectedIndexes must not be null");
    }

    @Override
    public RowProjectedDefault with(Row source)
    {
        this.source = ArgumentCheck.nonNull(source, "source must not be null");
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
        int sourceIndex = sourceIndex(fieldIndex);
        return sourceIndex < source().size() && source().isSet(sourceIndex);
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
        int sourceIndex = sourceIndex(fieldIndex);
        if (sourceIndex >= source().size())
        {
            return source().length();
        }
        return source().start(sourceIndex);
    }

    @Override
    public int length(int fieldIndex)
    {
        int sourceIndex = sourceIndex(fieldIndex);
        if (sourceIndex >= source().size())
        {
            return 0;
        }
        return source().length(sourceIndex);
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
            int start = start(i);
            int length = length(i);
            for (int j = 0; j < length; ++j)
            {
                copy.append(charAt(start + j));
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
        return ArgumentCheck.nonNull(this.source, "source row must be set before use");
    }
}
