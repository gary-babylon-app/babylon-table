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
import java.util.Arrays;

final class LineReaderCSVFixedWidth implements LineReader
{
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final BufferedCharReader reader;
    private final int[] fixedWidths;
    private final RowFixedWidth current;

    protected LineReaderCSVFixedWidth(BufferedCharReader reader, TabularReaderCsv options)
    {
        ArgumentCheck.nonNull(options, "options must not be null");
        int[] configuredWidths = options.getFixedWidths();
        if (configuredWidths == null || configuredWidths.length == 0)
        {
            throw new IllegalArgumentException("fixedWidths must not be empty");
        }
        this.reader = ArgumentCheck.nonNull(reader, "reader must not be null");
        this.fixedWidths = Arrays.copyOf(configuredWidths, configuredWidths.length);
        this.current = new RowFixedWidth(this.fixedWidths);
    }

    @Override
    public boolean next() throws IOException
    {
        this.current.clear();
        boolean anyCharRead = false;
        while (true)
        {
            int nextRowEnd = this.reader.next(CR);
            int nextLf = this.reader.next(LF);
            int nextTerminator;
            if (nextRowEnd == -1)
            {
                nextTerminator = nextLf;
            }
            else if (nextLf == -1)
            {
                nextTerminator = nextRowEnd;
            }
            else
            {
                nextTerminator = Math.min(nextRowEnd, nextLf);
            }

            if (nextTerminator == -1)
            {
                if (!anyCharRead)
                {
                    return false;
                }
                this.current.finish();
                return true;
            }

            int position = this.reader.position();
            if (nextTerminator > position)
            {
                this.current.append(this.reader.buffer(), position, nextTerminator - position);
                this.reader.advance(nextTerminator - position);
                anyCharRead = true;
            }

            int c = this.reader.read();
            if (c == -1)
            {
                if (!anyCharRead)
                {
                    return false;
                }
                this.current.finish();
                return true;
            }

            char ch = (char) c;
            if (ch == CR && this.reader.peek() == LF)
            {
                this.reader.read();
            }
            this.current.finish();
            return true;
        }
    }

    @Override
    public Row current()
    {
        return this.current;
    }

    @Override
    public void close() throws IOException
    {
        if (this.reader != null)
        {
            this.reader.close();
        }
    }

}
