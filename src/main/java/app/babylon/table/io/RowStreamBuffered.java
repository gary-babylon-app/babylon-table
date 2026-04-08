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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class RowStreamBuffered implements RowStreamMarkable
{
    private final LineReader lineReader;
    private final List<Row> cachedRows;
    private Row current;
    private boolean recording;
    private int dataStartIndex;
    private int replayIndex;

    RowStreamBuffered(LineReader lineReader)
    {
        this.lineReader = app.babylon.lang.ArgumentCheck.nonNull(lineReader, "lineReader must not be null");
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
        boolean hasRow = this.lineReader.next();
        if (hasRow && this.recording)
        {
            this.cachedRows.add(this.lineReader.current().copy());
        }
        this.current = hasRow ? this.lineReader.current() : null;
        return hasRow;
    }

    @Override
    public Row current()
    {
        return app.babylon.lang.ArgumentCheck.nonNull(this.current,
                "current row is not available until next() succeeds");
    }
}
