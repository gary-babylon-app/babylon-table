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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

final class LineReaderCSVFixedWidth implements LineReader
{
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final BufferedCharReader reader;
    private final int[] fixedWidths;
    private final Csv.ReadSettings settings;
    private final RowFixedWidth current;

    protected LineReaderCSVFixedWidth(BufferedInputStream instream, Csv.ReadSettings settings, Charset charset,
            int bomLength)
    {
        ArgumentCheck.nonNull(settings, "settings must not be null");
        int[] configuredWidths = settings.getFixedWidths();
        if (configuredWidths == null || configuredWidths.length == 0)
        {
            throw new IllegalArgumentException("fixedWidths must not be empty");
        }
        this.reader = createReader(instream, charset, bomLength);
        this.fixedWidths = Arrays.copyOf(configuredWidths, configuredWidths.length);
        this.settings = settings;
        this.current = new RowFixedWidth(this.fixedWidths);
    }

    private static BufferedCharReader createReader(InputStream instream, Charset charset, int bomLength)
    {
        try
        {
            if (bomLength > 0)
            {
                skipBytes(instream, bomLength);
            }
        } catch (IOException e)
        {
            throw new RuntimeException("Failed to skip fixed-width BOM bytes.", e);
        }
        return new BufferedCharReader(new InputStreamReader(instream, charset));
    }

    private static void skipBytes(InputStream instream, int count) throws IOException
    {
        int remaining = count;
        while (remaining > 0)
        {
            long skipped = instream.skip(remaining);
            if (skipped > 0)
            {
                remaining -= (int) skipped;
                continue;
            }
            int b = instream.read();
            if (b == -1)
            {
                return;
            }
            --remaining;
        }
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
            } else if (nextLf == -1)
            {
                nextTerminator = nextRowEnd;
            } else
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
    public Csv.ReadSettings getSettings()
    {
        return this.settings;
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
