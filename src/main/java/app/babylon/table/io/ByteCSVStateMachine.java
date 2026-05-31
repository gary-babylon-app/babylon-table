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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import app.babylon.lang.ArgumentCheck;

final class ByteCSVStateMachine implements LineReader
{
    private static final int CR = '\r';
    private static final int LF = '\n';

    private final ByteReaderCSV reader;
    private final int separator;
    private final int quote;
    private final ByteStringSlices.Builder builder;
    private ByteStringSlices current;

    ByteCSVStateMachine(ByteReaderCSV reader, char separator, char quote)
    {
        this(reader, separator, quote, StandardCharsets.UTF_8);
    }

    ByteCSVStateMachine(ByteReaderCSV reader, char separator, char quote, Charset charset)
    {
        Charset resolvedCharset = ArgumentCheck.nonNull(charset, "charset must not be null");
        requireAsciiCompatible(resolvedCharset, separator, quote);
        this.separator = toByteValue(separator, "separator");
        this.quote = toByteValue(quote, "quote");
        this.reader = ArgumentCheck.nonNull(reader, "reader must not be null");
        this.builder = new ByteStringSlices.Builder(resolvedCharset);
        this.current = null;
    }

    @Override
    public boolean next() throws IOException
    {
        boolean hasRow = readRowParsed(this.builder);
        this.current = hasRow ? this.builder.build() : null;
        return hasRow;
    }

    @Override
    public ByteStringSlices current()
    {
        return ArgumentCheck.nonNull(this.current, "current row is not available until next() succeeds");
    }

    private boolean readRowParsed(ByteStringSlices.Builder output) throws IOException
    {
        final int separator = this.separator;
        final int quote = this.quote;
        output.clear();
        boolean inQuotes = false;
        boolean anyByteRead = false;
        boolean anyNonRowTerminator = false;
        boolean fieldHasContent = false;
        int completedFieldCount = 0;

        while (true)
        {
            if (inQuotes)
            {
                int quoteIndex = this.reader.next(quote);
                if (quoteIndex == -1)
                {
                    throw new IOException("Unterminated quoted field at EOF.");
                }

                int position = this.reader.position();
                if (quoteIndex > position)
                {
                    output.append(this.reader.buffer(), position, quoteIndex - position);
                    this.reader.advance(quoteIndex - position);
                    anyByteRead = true;
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                    continue;
                }
            }
            else
            {
                int specialIndex = this.reader.nextSpecial(separator, quote);
                if (specialIndex == -1)
                {
                    if (!anyByteRead)
                    {
                        return false;
                    }
                    if (!anyNonRowTerminator && completedFieldCount == 0 && !fieldHasContent)
                    {
                        output.clear();
                        return false;
                    }
                    capture(output);
                    return true;
                }

                int position = this.reader.position();
                if (specialIndex > position)
                {
                    output.append(this.reader.buffer(), position, specialIndex - position);
                    this.reader.advance(specialIndex - position);
                    anyByteRead = true;
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                    continue;
                }
            }

            int value = this.reader.read();
            if (value == -1)
            {
                if (!anyByteRead)
                {
                    return false;
                }
                if (inQuotes)
                {
                    throw new IOException("Unterminated quoted field at EOF.");
                }
                if (!anyNonRowTerminator && completedFieldCount == 0 && !fieldHasContent)
                {
                    output.clear();
                    return false;
                }
                capture(output);
                return true;
            }

            anyByteRead = true;

            if (inQuotes)
            {
                if (value == quote)
                {
                    int next = this.reader.peek();
                    if (next == quote)
                    {
                        this.reader.read();
                        output.append(quote);
                        fieldHasContent = true;
                        anyNonRowTerminator = true;
                    }
                    else
                    {
                        inQuotes = false;
                    }
                }
                else
                {
                    output.append(value);
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                }
                continue;
            }

            if (value == quote)
            {
                if (!fieldHasContent)
                {
                    inQuotes = true;
                }
                else
                {
                    output.append(value);
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                }
                continue;
            }

            if (value == separator)
            {
                capture(output);
                fieldHasContent = false;
                ++completedFieldCount;
                anyNonRowTerminator = true;
                continue;
            }

            if (value == LF || value == CR)
            {
                if (value == CR && this.reader.peek() == LF)
                {
                    this.reader.read();
                }

                if (!anyNonRowTerminator && completedFieldCount == 0 && !fieldHasContent)
                {
                    output.clear();
                    return true;
                }
                capture(output);
                return true;
            }

            output.append(value);
            fieldHasContent = true;
            anyNonRowTerminator = true;
        }
    }

    private static void capture(ByteStringSlices.Builder output)
    {
        output.finishField();
    }

    static int toByteValue(char value, String name)
    {
        if (value > 0xFF)
        {
            throw new IllegalArgumentException(name + " must fit in one byte.");
        }
        return value;
    }

    static void requireAsciiCompatible(Charset charset, char separator, char quote)
    {
        requireAsciiByte(charset, separator, "separator");
        requireAsciiByte(charset, quote, "quote");
        requireAsciiByte(charset, '\r', "carriage return");
        requireAsciiByte(charset, '\n', "line feed");
    }

    private static void requireAsciiByte(Charset charset, char value, String name)
    {
        if (value > 0x7F)
        {
            throw new IllegalArgumentException(name + " must be ASCII for byte CSV parsing.");
        }
        ByteBuffer encoded = charset.encode(String.valueOf(value));
        if (encoded.remaining() != 1 || Byte.toUnsignedInt(encoded.get(encoded.position())) != value)
        {
            throw new IllegalArgumentException(
                    "Byte CSV reader requires an ASCII-compatible charset for " + name + ": " + charset);
        }
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
