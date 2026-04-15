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

final class LineReaderCSV implements LineReader
{
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final BufferedCharReader reader;
    private final char separator;
    private final char quote;
    private final RowBuffer current;

    protected LineReaderCSV(BufferedCharReader reader, TabularRowReaderCsv options)
    {
        TabularRowReaderCsv checkedOptions = ArgumentCheck.nonNull(options, "options must not be null");
        this.separator = checkedOptions.getSeparator();
        this.quote = checkedOptions.getQuote();
        this.reader = ArgumentCheck.nonNull(reader, "reader must not be null");
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

    private boolean readRowParsed(RowBuffer output) throws IOException
    {
        final char separator = this.separator;
        final char quote = this.quote;
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
                    anyCharRead = true;
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
                if (ch == quote)
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
                    output.append(ch);
                    fieldHasContent = true;
                    anyNonRowTerminator = true;
                }
                continue;
            }

            if (ch == quote)
            {
                if (!fieldHasContent)
                {
                    inQuotes = true;
                }
                else
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
