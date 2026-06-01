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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RowStreamBuffered implements RowStreamMarkable
{
    private final RowCursor rowCursor;
    private final List<RowValues> cachedRows;
    private RowValues current;
    private boolean recording;
    private int dataStartIndex;
    private int replayIndex;

    public RowStreamBuffered(RowCursor rowCursor)
    {
        this.rowCursor = ArgumentCheck.nonNull(rowCursor, "rowCursor must not be null");
        this.cachedRows = new ArrayList<>();
        this.current = null;
        this.recording = true;
        this.dataStartIndex = 0;
        this.replayIndex = -1;
    }

    @Override
    public void mark(int rowIndex)
    {
        if (rowIndex < 0 || rowIndex >= this.cachedRows.size())
        {
            throw new IllegalArgumentException("Header row index out of range: " + rowIndex);
        }
        this.dataStartIndex = rowIndex + 1;
    }

    @Override
    public void reset()
    {
        this.recording = false;
        this.replayIndex = this.dataStartIndex;
    }

    @Override
    public boolean next() throws IOException
    {
        if (this.replayIndex >= 0 && this.replayIndex < this.cachedRows.size())
        {
            this.current = this.cachedRows.get(this.replayIndex++);
            return true;
        }
        this.replayIndex = -1;
        boolean hasRow = this.rowCursor.next();
        if (hasRow && this.recording)
        {
            this.cachedRows.add(this.rowCursor.current());
        }
        this.current = hasRow ? this.rowCursor.current() : null;
        return hasRow;
    }

    @Override
    public RowValues current()
    {
        return ArgumentCheck.nonNull(this.current, "current row is not available until next() succeeds");
    }

}
