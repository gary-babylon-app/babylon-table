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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.babylon.lang.ArgumentCheck;

final class FixedWidthLineReader implements LineReader
{
    private final BufferedReader reader;
    private final int[] fixedWidths;
    private final Charset charset;
    private ByteStringSlices current;

    FixedWidthLineReader(BufferedReader reader, int[] fixedWidths)
    {
        this(reader, fixedWidths, StandardCharsets.UTF_8);
    }

    FixedWidthLineReader(BufferedReader reader, int[] fixedWidths, Charset charset)
    {
        if (fixedWidths == null || fixedWidths.length == 0)
        {
            throw new IllegalArgumentException("fixedWidths must not be empty");
        }
        this.reader = ArgumentCheck.nonNull(reader, "reader must not be null");
        this.fixedWidths = Arrays.copyOf(fixedWidths, fixedWidths.length);
        this.charset = ArgumentCheck.nonNull(charset, "charset must not be null");
        this.current = null;
    }

    @Override
    public boolean next() throws IOException
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

    @Override
    public ByteStringSlices current()
    {
        return ArgumentCheck.nonNull(this.current, "current row is not available until next() succeeds");
    }

    @Override
    public void close() throws IOException
    {
        this.reader.close();
    }

    private ByteStringSlices parse(String line)
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
        return builder.build().toByteStringSlices(this.charset);
    }
}
