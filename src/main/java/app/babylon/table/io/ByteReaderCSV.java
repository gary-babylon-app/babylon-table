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
import java.io.InputStream;

import app.babylon.lang.ArgumentCheck;

final class ByteReaderCSV implements Closeable
{
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final InputStream inputStream;
    private final byte[] buffer;
    private int position;
    private int limit;

    ByteReaderCSV(InputStream inputStream)
    {
        this(inputStream, DEFAULT_BUFFER_SIZE);
    }

    ByteReaderCSV(InputStream inputStream, int bufferSize)
    {
        this.inputStream = ArgumentCheck.nonNull(inputStream, "inputStream must not be null");
        this.buffer = new byte[Math.max(1, bufferSize)];
        this.position = 0;
        this.limit = 0;
    }

    int read() throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        return Byte.toUnsignedInt(this.buffer[this.position++]);
    }

    int peek() throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        return Byte.toUnsignedInt(this.buffer[this.position]);
    }

    byte[] buffer()
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

    int nextSpecial(int separator, int quote) throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        for (int i = this.position; i < this.limit; ++i)
        {
            int value = Byte.toUnsignedInt(this.buffer[i]);
            if (value == separator || value == quote || value == '\r' || value == '\n')
            {
                return i;
            }
        }
        return this.limit;
    }

    int next(int value) throws IOException
    {
        if (!ensureAvailable())
        {
            return -1;
        }
        for (int i = this.position; i < this.limit; ++i)
        {
            if (Byte.toUnsignedInt(this.buffer[i]) == value)
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
        this.limit = this.inputStream.read(this.buffer, 0, this.buffer.length);
        this.position = 0;
        return this.limit > 0;
    }

    @Override
    public void close() throws IOException
    {
        this.inputStream.close();
    }
}
