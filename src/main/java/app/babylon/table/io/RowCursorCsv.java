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
import java.util.LinkedHashMap;
import java.util.Map;

import app.babylon.io.StreamSourceProbe;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;

/**
 * Supplies rows from an open CSV input stream.
 * <p>
 * Instances are created by a configured {@link RowSourceCsv} and own the CSV
 * reader resources for the lifetime of iteration.
 */
public final class RowCursorCsv implements RowCursor
{
    private static final Charset LEGACY_CSV_FALLBACK = Charset.forName("windows-1252");

    private final Map<ColumnName, Column.Type> explicitColumnTypes;
    private final HeaderStrategy headerStrategy;
    private final boolean stripping;
    private final char separator;
    private final char quote;
    private final int[] fixedWidths;
    private final Charset charset;
    private final boolean autoDetectEncoding;
    private final LineReader lineReader;
    private final RowStreamMarkable rowStream;
    private final RowProjected projectedRow;
    private final ColumnDefinition[] columnDefinitions;

    RowCursorCsv(InputStream inputStream, Builder builder)
    {
        BufferedInputStream bufferedInputStream = toBufferedStream(ArgumentCheck.nonNull(inputStream));
        this.explicitColumnTypes = new LinkedHashMap<>(ArgumentCheck.nonNull(builder).explicitColumnTypes);
        this.headerStrategy = ArgumentCheck.nonNull(builder.headerStrategy);
        this.stripping = builder.stripping;
        this.separator = builder.separator;
        this.quote = builder.quote;
        this.fixedWidths = builder.fixedWidths == null
                ? null
                : Arrays.copyOf(builder.fixedWidths, builder.fixedWidths.length);
        this.charset = ArgumentCheck.nonNull(builder.charset);
        this.autoDetectEncoding = builder.autoDetectEncoding;

        try
        {
            StreamSourceProbe probe = StreamSourceProbe.of(bufferedInputStream, "stream.csv");
            if (probe.isXls() || probe.isXlsx() || probe.isPdf() || probe.isZip())
            {
                throw new IllegalArgumentException("Input stream does not appear to contain CSV text.");
            }

            int bomLength = resolveBomLength(probe);
            CsvFormat format = this.autoDetectEncoding
                    ? CsvFormatProbe.detect(bufferedInputStream, "stream.csv", LEGACY_CSV_FALLBACK, this.separator,
                            this.quote)
                    : new CsvFormat(resolveCharset(probe), this.separator, this.quote, 1.0d);
            BufferedCharReader reader = createBufferedCharReader(bufferedInputStream, format.charset(), bomLength);
            this.lineReader = isFixedWidths()
                    ? new LineReaderCSVFixedWidth(reader, toReaderOptions(format))
                    : new LineReaderCSV(reader, toReaderOptions(format));
            this.rowStream = new RowStreamBuffered(this.lineReader);

            HeaderDetection headerDetection = this.headerStrategy.detect(this.rowStream, null);
            this.projectedRow = createProjectedRow(headerDetection);
            this.columnDefinitions = toColumnDefinitions(headerDetection.getSelectedHeaders());
            this.rowStream.reset();
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

    @Override
    public ColumnDefinition[] columns()
    {
        return Arrays.copyOf(this.columnDefinitions, this.columnDefinitions.length);
    }

    @Override
    public boolean next()
    {
        try
        {
            return this.rowStream.next();
        }
        catch (IOException e)
        {
            throw new TableException("Failed to read CSV row.", e);
        }
    }

    @Override
    public Row current()
    {
        Row row = this.rowStream.current();
        return this.projectedRow == null ? row : this.projectedRow.with(row);
    }

    @Override
    public void close() throws IOException
    {
        this.lineReader.close();
    }

    private TabularRowReaderCsv toReaderOptions(CsvFormat format)
    {
        TabularRowReaderCsv options = new TabularRowReaderCsv();
        options.withHeaderStrategy(this.headerStrategy);
        options.withStripping(this.stripping);
        options.withSeparator(format.separator());
        options.withQuote(format.quote());
        options.withFixedWidths(this.fixedWidths);
        options.withCharset(format.charset());
        options.withAutoDetectEncoding(this.autoDetectEncoding);
        return options;
    }

    private ColumnDefinition[] toColumnDefinitions(String[] headers)
    {
        ColumnDefinition[] definitions = new ColumnDefinition[headers.length];
        for (int i = 0; i < headers.length; ++i)
        {
            ColumnName columnName = ColumnName.of(headers[i]);
            Column.Type explicitType = this.explicitColumnTypes.get(columnName);
            definitions[i] = new ColumnDefinition(columnName, explicitType);
        }
        return definitions;
    }

    private RowProjected createProjectedRow(HeaderDetection headerDetection)
    {
        return this.stripping
                ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                : new RowProjectedDefault(headerDetection.getSelectedPositions());
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

    public static final class Builder
    {
        private final Map<ColumnName, Column.Type> explicitColumnTypes;
        private HeaderStrategy headerStrategy;
        private boolean stripping;
        private char separator;
        private char quote;
        private int[] fixedWidths;
        private Charset charset;
        private boolean autoDetectEncoding;

        private Builder()
        {
            this.explicitColumnTypes = new LinkedHashMap<>();
            this.headerStrategy = new HeaderStrategyAuto(HeaderStrategy.DEFAULT_SCAN_LIMIT);
            this.stripping = true;
            this.separator = ',';
            this.quote = '"';
            this.fixedWidths = null;
            this.charset = StandardCharsets.UTF_8;
            this.autoDetectEncoding = true;
        }

        public Builder withHeaderStrategy(HeaderStrategy headerStrategy)
        {
            this.headerStrategy = ArgumentCheck.nonNull(headerStrategy);
            return this;
        }

        public Builder withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Builder withStripping(boolean stripping)
        {
            this.stripping = stripping;
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

        /**
         * Specifies a source-side column type for this CSV cursor.
         * <p>
         * This type becomes part of the cursor schema returned by
         * {@link RowCursor#columns()} and can therefore influence which builder the row
         * consumer chooses before any rows are read.
         * <p>
         * For categorical text columns, this is typically only worth doing when the
         * direct parser is materially better than first building the string dictionary.
         *
         * @param columnName
         *            the source column name
         * @param columnType
         *            the source-side column type
         * @return this builder
         */
        public Builder withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.explicitColumnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
            return this;
        }

        /**
         * Specifies source-side column types to expose through
         * {@link RowCursor#columns()}.
         * <p>
         * These types are consumed before rows are read, so they can select a more
         * direct builder and bypass intermediate string materialization when the type
         * has an efficient slice-based parser.
         * <p>
         * In normal categorical-text cases, it is often still preferable to leave the
         * source as {@code STRING} and let the row consumer build the natural string
         * dictionary.
         *
         * @param columnTypes
         *            source-side column types keyed by column name
         * @return this builder
         */
        public Builder withColumnTypes(Map<ColumnName, Column.Type> columnTypes)
        {
            if (columnTypes != null)
            {
                for (Map.Entry<ColumnName, Column.Type> entry : columnTypes.entrySet())
                {
                    withColumnType(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        Builder copy()
        {
            Builder copy = new Builder();
            copy.headerStrategy = this.headerStrategy;
            copy.stripping = this.stripping;
            copy.separator = this.separator;
            copy.quote = this.quote;
            copy.fixedWidths = this.fixedWidths == null
                    ? null
                    : Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
            copy.charset = this.charset;
            copy.autoDetectEncoding = this.autoDetectEncoding;
            copy.explicitColumnTypes.putAll(this.explicitColumnTypes);
            return copy;
        }

        public RowCursorCsv build(InputStream inputStream)
        {
            return new RowCursorCsv(inputStream, this);
        }
    }
}
