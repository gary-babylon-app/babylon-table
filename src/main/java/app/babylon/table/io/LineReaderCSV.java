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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

final class LineReaderCSV implements LineReader
{
    private static final char QUOTE = '"';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final BufferedCharReader reader;
    private final ReadSettingsCSV settings;
    private final RowBuffer current;

    protected LineReaderCSV(BufferedInputStream instream, ReadSettingsCSV options, Charset charset, int bomLength)
    {
        this.settings = app.babylon.lang.ArgumentCheck.nonNull(options, "options must not be null");
        this.reader = createReader(instream, charset, bomLength);
        this.current = new RowBuffer();
    }

    @Override
    public boolean next() throws IOException
    {
        return readRowParsed(this.current);
    }

    @Override
    public Row current()
    {
        return this.current;
    }

    @Override
    public ReadSettingsCSV getSettings()
    {
        return this.settings;
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
            throw new RuntimeException("Failed to skip CSV BOM bytes.", e);
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

    private boolean readRowParsed(RowBuffer output) throws IOException
    {
        final char separator = this.settings.getSeparator();
        output.clear();
        boolean inQuotes = false;
        boolean anyCharRead = false;
        boolean anyNonRowTerminator = false;
        boolean fieldHasContent = false;
        int completedFieldCount = 0;

        while (true)
        {
            if (inQuotes)
            {
                int quoteIndex = this.reader.next(QUOTE);
                if (quoteIndex == -1)
                {
                    throw new IOException("Unterminated quoted field at EOF.");
                }

                int position = this.reader.position();
                if (quoteIndex > position)
                {
                    output.append(this.reader.buffer(), position, quoteIndex - position);
                    this.reader.advance(quoteIndex - position);
                    anyCharRead = true;
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                    continue;
                }
            } else
            {
                int specialIndex = this.reader.nextSpecial(separator, QUOTE);
                if (specialIndex == -1)
                {
                    if (!anyCharRead)
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
                    anyCharRead = true;
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                    continue;
                }
            }

            int c = this.reader.read();
            if (c == -1)
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
            char ch = (char) c;

            if (inQuotes)
            {
                if (ch == QUOTE)
                {
                    int next = this.reader.peek();
                    if (next == QUOTE)
                    {
                        this.reader.read();
                        output.append(QUOTE);
                        fieldHasContent = true;
                        anyNonRowTerminator = true;
                    } else
                    {
                        inQuotes = false;
                    }
                } else
                {
                    output.append(ch);
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                }
                continue;
            }

            if (ch == QUOTE)
            {
                if (!fieldHasContent)
                {
                    inQuotes = true;
                } else
                {
                    output.append(ch);
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                }
                continue;
            }

            if (ch == separator)
            {
                capture(output);
                fieldHasContent = false;
                ++completedFieldCount;
                anyNonRowTerminator = true;
                continue;
            }

            if (ch == LF || ch == CR)
            {
                if (ch == CR && this.reader.peek() == LF)
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

            output.append(ch);
            fieldHasContent = true;
            anyNonRowTerminator = true;
        }
    }

    private void capture(RowBuffer output)
    {
        output.finishField();
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
