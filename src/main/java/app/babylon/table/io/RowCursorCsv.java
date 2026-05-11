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

import app.babylon.io.StreamSourceProbe;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;

/**
 * Supplies rows from an open CSV input stream.
 * <p>
 * Instances are created by a configured {@link RowSourceCsv} and own the CSV
 * reader resources for the lifetime of iteration.
 */
final class RowCursorCsv extends RowCursorLineReaderCommon
{
    private static final Charset LEGACY_CSV_FALLBACK = Charset.forName("windows-1252");

    private final ReadOptionsCsv options;

    RowCursorCsv(InputStream inputStream, ReadOptionsCsv options)
    {
        super(createLineReader(ArgumentCheck.nonNull(inputStream), ArgumentCheck.nonNull(options)));
        this.options = options;
    }

    public char getSeparator()
    {
        return this.options.separator();
    }

    public char getQuote()
    {
        return this.options.quote();
    }

    public int[] getFixedWidths()
    {
        return this.options.fixedWidths();
    }

    public Charset getCharset()
    {
        return this.options.charset();
    }

    public boolean isAutoDetectEncoding()
    {
        return this.options.autoDetectEncoding();
    }

    private static LineReader createLineReader(InputStream inputStream, ReadOptionsCsv options)
    {
        BufferedInputStream bufferedInputStream = toBufferedStream(inputStream);
        try
        {
            StreamSourceProbe probe = StreamSourceProbe.of(bufferedInputStream, "stream.csv");
            if (probe.isXls() || probe.isXlsx() || probe.isPdf() || probe.isZip())
            {
                throw new IllegalArgumentException("Input stream does not appear to contain CSV text.");
            }

            int bomLength = options.autoDetectEncoding() ? probe.bomLengthBytes() : 0;
            DetectedCsvFormat detectedFormat = options.autoDetectEncoding()
                    ? CsvFormatProbe.detect(bufferedInputStream, "stream.csv", LEGACY_CSV_FALLBACK, options.separator(),
                            options.quote())
                    : new DetectedCsvFormat(options.charset(), options.separator(), options.quote(), 1.0d);
            BufferedCharReader reader = createBufferedCharReader(bufferedInputStream, detectedFormat.charset(),
                    bomLength);
            return options.isFixedWidths()
                    ? new LineReaderCSVFixedWidth(reader, options.fixedWidths())
                    : new LineReaderCSV(reader, detectedFormat.separator(), detectedFormat.quote());
        }
        catch (IOException | RuntimeException e)
        {
            closeAndThrow(bufferedInputStream, e);
            throw new AssertionError("unreachable");
        }
    }

    private static void closeAndThrow(BufferedInputStream bufferedInputStream, Exception e)
    {
        try
        {
            bufferedInputStream.close();
        }
        catch (IOException closeException)
        {
            e.addSuppressed(closeException);
        }
        if (e instanceof RuntimeException runtimeException)
        {
            throw runtimeException;
        }
        throw new TableException("Failed to prepare CSV row supplier.", e);
    }

    private static BufferedCharReader createBufferedCharReader(InputStream inputStream, Charset charset, int bomLength)
    {
        try
        {
            skipBytes(inputStream, bomLength);
        }
        catch (IOException e)
        {
            throw new TableException("Failed to skip CSV BOM bytes.", e);
        }
        return new BufferedCharReader(new InputStreamReader(inputStream, charset));
    }

    private static BufferedInputStream toBufferedStream(InputStream inputStream)
    {
        if (inputStream instanceof BufferedInputStream bufferedInputStream)
        {
            return bufferedInputStream;
        }
        return new BufferedInputStream(inputStream);
    }

    private static void skipBytes(InputStream inputStream, int count) throws IOException
    {
        int remaining = count;
        while (remaining > 0)
        {
            long skipped = inputStream.skip(remaining);
            if (skipped > 0)
            {
                remaining -= (int) skipped;
                continue;
            }
            int b = inputStream.read();
            if (b == -1)
            {
                return;
            }
            --remaining;
        }
    }

}
