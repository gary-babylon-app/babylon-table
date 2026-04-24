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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.babylon.io.StreamSourceProbe;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;

/**
 * Supplies rows from an open CSV input stream.
 * <p>
 * Instances are created by a configured {@link RowSourceCsv} and own the CSV
 * reader resources for the lifetime of iteration.
 */
public final class RowCursorCsv extends RowCursorLineReaderCommon
{
    private static final Charset LEGACY_CSV_FALLBACK = Charset.forName("windows-1252");

    private final HeaderStrategy headerStrategy;
    private final boolean stripping;
    private final char separator;
    private final char quote;
    private final int[] fixedWidths;
    private final Charset charset;
    private final boolean autoDetectEncoding;

    RowCursorCsv(InputStream inputStream, Builder builder)
    {
        super(createLineReader(ArgumentCheck.nonNull(inputStream), builder), builder);
        this.headerStrategy = ArgumentCheck.nonNull(builder.headerStrategy);
        this.stripping = builder.stripping;
        this.separator = builder.separator;
        this.quote = builder.quote;
        this.fixedWidths = builder.fixedWidths == null
                ? null
                : Arrays.copyOf(builder.fixedWidths, builder.fixedWidths.length);
        this.charset = ArgumentCheck.nonNull(builder.charset);
        this.autoDetectEncoding = builder.autoDetectEncoding;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public HeaderStrategy getHeaderStrategy()
    {
        return this.headerStrategy;
    }

    public char getSeparator()
    {
        return this.separator;
    }

    public boolean isStripping()
    {
        return this.stripping;
    }

    public char getQuote()
    {
        return this.quote;
    }

    public int[] getFixedWidths()
    {
        return this.fixedWidths == null ? null : Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
    }

    public Charset getCharset()
    {
        return this.charset;
    }

    public boolean isAutoDetectEncoding()
    {
        return this.autoDetectEncoding;
    }

    private static LineReader createLineReader(InputStream inputStream, Builder builder)
    {
        BufferedInputStream bufferedInputStream = toBufferedStream(inputStream);
        try
        {
            StreamSourceProbe probe = StreamSourceProbe.of(bufferedInputStream, "stream.csv");
            if (probe.isXls() || probe.isXlsx() || probe.isPdf() || probe.isZip())
            {
                throw new IllegalArgumentException("Input stream does not appear to contain CSV text.");
            }

            int bomLength = builder.resolveBomLength(probe);
            CsvFormat format = builder.autoDetectEncoding
                    ? CsvFormatProbe.detect(bufferedInputStream, "stream.csv", LEGACY_CSV_FALLBACK, builder.separator,
                            builder.quote)
                    : new CsvFormat(builder.resolveCharset(probe), builder.separator, builder.quote, 1.0d);
            BufferedCharReader reader = createBufferedCharReader(bufferedInputStream, format.charset(), bomLength);
            return builder.isFixedWidths()
                    ? new LineReaderCSVFixedWidth(reader, toReaderOptions(builder, format))
                    : new LineReaderCSV(reader, toReaderOptions(builder, format));
        }
        catch (IOException | RuntimeException e)
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
    }

    private static TabularRowReaderCsv toReaderOptions(Builder builder, CsvFormat format)
    {
        TabularRowReaderCsv options = new TabularRowReaderCsv();
        options.withHeaderStrategy(builder.headerStrategy);
        options.withStripping(builder.stripping);
        options.withSeparator(format.separator());
        options.withQuote(format.quote());
        options.withFixedWidths(builder.fixedWidths);
        options.withCharset(format.charset());
        options.withAutoDetectEncoding(builder.autoDetectEncoding);
        return options;
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

    public static final class Builder extends RowCursorLineReaderCommon.BuilderBase<Builder>
    {
        private char separator;
        private char quote;
        private int[] fixedWidths;
        private Charset charset;
        private boolean autoDetectEncoding;

        private Builder()
        {
            super();
            this.separator = ',';
            this.quote = '"';
            this.fixedWidths = null;
            this.charset = StandardCharsets.UTF_8;
            this.autoDetectEncoding = true;
        }

        public Builder withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Builder withQuote(char quote)
        {
            this.quote = quote;
            return this;
        }

        public Builder withFixedWidths(int[] fixedWidths)
        {
            this.fixedWidths = fixedWidths == null ? null : Arrays.copyOf(fixedWidths, fixedWidths.length);
            return this;
        }

        public Builder withCharset(Charset charset)
        {
            this.charset = ArgumentCheck.nonNull(charset);
            return this;
        }

        public Builder withAutoDetectEncoding(boolean autoDetectEncoding)
        {
            this.autoDetectEncoding = autoDetectEncoding;
            return this;
        }

        Builder copy()
        {
            Builder copy = new Builder();
            copyCommonTo(copy);
            copy.separator = this.separator;
            copy.quote = this.quote;
            copy.fixedWidths = this.fixedWidths == null
                    ? null
                    : Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
            copy.charset = this.charset;
            copy.autoDetectEncoding = this.autoDetectEncoding;
            return copy;
        }

        private boolean isFixedWidths()
        {
            return this.fixedWidths != null && this.fixedWidths.length > 0;
        }

        private Charset resolveCharset(StreamSourceProbe probe)
        {
            if (this.autoDetectEncoding)
            {
                return probe.getCharset(LEGACY_CSV_FALLBACK);
            }
            return this.charset;
        }

        private int resolveBomLength(StreamSourceProbe probe)
        {
            return this.autoDetectEncoding ? probe.bomLengthBytes() : 0;
        }

        @Override
        protected Builder self()
        {
            return this;
        }

        public RowCursorCsv build(InputStream inputStream)
        {
            return new RowCursorCsv(inputStream, this);
        }
    }
}
