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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

final class BufferedCharReader implements Closeable
{
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final Reader reader;
    private final char[] buffer;
    private int position;
    private int limit;

    BufferedCharReader(Reader reader)
    {
        this(reader, DEFAULT_BUFFER_SIZE);
    }

    BufferedCharReader(Reader reader, int bufferSize)
    {
        this.reader = Objects.requireNonNull(reader, "reader must not be null");
        this.buffer = new char[Math.max(1, bufferSize)];
        this.position = 0;
        this.limit = 0;
    }

    int read() throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        return this.buffer[this.position++];
    }

    int peek() throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        return this.buffer[this.position];
    }

    char[] buffer()
    {
        return this.buffer;
    }

    int position()
    {
        return this.position;
    }

    void advance(int count)
    {
        this.position += count;
    }

    int nextSpecial(char separator, char quote) throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        for (int i = this.position; i < this.limit; ++i)
        {
            char ch = this.buffer[i];
            if (ch == separator || ch == quote || ch == '\r' || ch == '\n')
            {
                return i;
            }
        }
        return this.limit;
    }

    int next(char value) throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        for (int i = this.position; i < this.limit; ++i)
        {
            if (this.buffer[i] == value)
            {
                return i;
            }
        }
        return this.limit;
    }

    private boolean ensureAvailable() throws IOException
    {
        if (this.position < this.limit)
        {
            return true;
        }
        this.limit = this.reader.read(this.buffer, 0, this.buffer.length);
        this.position = 0;
        return this.limit > 0;
    }

    @Override
    public void close() throws IOException
    {
        this.reader.close();
    }
}
