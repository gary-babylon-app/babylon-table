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
import java.io.PushbackReader;
import java.io.Reader;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;

final class RowCursorCharCSVStateMachine implements RowCursor
{
    private static final int CR = '\r';
    private static final int LF = '\n';

    private final PushbackReader reader;
    private final char separator;
    private final char quote;
    private final StringSlices.Builder builder;
    private StringSlices current;

    RowCursorCharCSVStateMachine(Reader reader, char separator, char quote)
    {
        this.reader = new PushbackReader(ArgumentCheck.nonNull(reader, "reader must not be null"), 1);
        this.separator = separator;
        this.quote = quote;
        this.builder = new StringSlices.Builder();
        this.current = null;
    }

    @Override
    public boolean next()
    {
        try
        {
            boolean hasRow = readRowParsed(this.builder);
            this.current = hasRow ? this.builder.build() : null;
            return hasRow;
        }
        catch (IOException e)
        {
            throw new TableException("Failed to read CSV row.", e);
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

    private boolean readRowParsed(StringSlices.Builder output) throws IOException
    {
        output.clear();
        boolean inQuotes = false;
        boolean anyCharRead = false;
        boolean anyNonRowTerminator = false;
        boolean fieldHasContent = false;
        int completedFieldCount = 0;

        while (true)
        {
            int value = this.reader.read();
            if (value == -1)
            {
                if (!anyCharRead)
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

            anyCharRead = true;

            if (inQuotes)
            {
                if (value == this.quote)
                {
                    int next = this.reader.read();
                    if (next == this.quote)
                    {
                        output.append((char) this.quote);
                        fieldHasContent = true;
                        anyNonRowTerminator = true;
                    }
                    else
                    {
                        if (next != -1)
                        {
                            this.reader.unread(next);
                        }
                        inQuotes = false;
                    }
                }
                else
                {
                    output.append((char) value);
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                }
                continue;
            }

            if (value == this.quote)
            {
                if (!fieldHasContent)
                {
                    inQuotes = true;
                }
                else
                {
                    output.append((char) value);
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                }
                continue;
            }

            if (value == this.separator)
            {
                capture(output);
                fieldHasContent = false;
                ++completedFieldCount;
                anyNonRowTerminator = true;
                continue;
            }

            if (value == LF || value == CR)
            {
                if (value == CR)
                {
                    int next = this.reader.read();
                    if (next != LF && next != -1)
                    {
                        this.reader.unread(next);
                    }
                }

                if (!anyNonRowTerminator && completedFieldCount == 0 && !fieldHasContent)
                {
                    output.clear();
                    return true;
                }
                capture(output);
                return true;
            }

            output.append((char) value);
            fieldHasContent = true;
            anyNonRowTerminator = true;
        }
    }

    private static void capture(StringSlices.Builder output)
    {
        output.finishField();
    }
}
