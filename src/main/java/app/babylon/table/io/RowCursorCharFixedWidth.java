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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;

final class RowCursorCharFixedWidth implements RowCursor
{
    private final BufferedReader reader;
    private final int[] fixedWidths;
    private StringSlices current;

    RowCursorCharFixedWidth(BufferedReader reader, int[] fixedWidths)
    {
        if (fixedWidths == null || fixedWidths.length == 0)
        {
            throw new IllegalArgumentException("fixedWidths must not be empty");
        }
        this.reader = ArgumentCheck.nonNull(reader, "reader must not be null");
        this.fixedWidths = Arrays.copyOf(fixedWidths, fixedWidths.length);
        this.current = null;
    }

    @Override
    public boolean next()
    {
        try
        {
            String line = this.reader.readLine();
            if (line == null)
            {
                this.current = null;
                return false;
            }
            this.current = parse(line);
            return true;
        }
        catch (IOException e)
        {
            throw new TableException("Failed to read fixed-width row.", e);
        }
    }

    @Override
    public RowValues current()
    {
        return ArgumentCheck.nonNull(this.current, "current row is not available until next() succeeds");
    }

    @Override
    public void close() throws IOException
    {
        this.reader.close();
    }

    private StringSlices parse(String line)
    {
        StringSlices.Builder builder = new StringSlices.Builder(line.length(), this.fixedWidths.length);
        int start = 0;
        for (int width : this.fixedWidths)
        {
            int end = Math.min(line.length(), start + width);
            if (start < end)
            {
                builder.append(line, start, end);
            }
            builder.finishField();
            start += width;
        }
        return builder.build();
    }
}
