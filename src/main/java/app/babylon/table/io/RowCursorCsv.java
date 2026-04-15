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
    /**
     * It is difficult to distinguish what encoding to use when UTF8 fails, and no
     * BOM and not UTF16 Common choices are ISO-8869-1 and Windows-1252.
     */
    private static final Charset LEGACY_CSV_FALLBACK = StandardCharsets.ISO_8859_1;
    // private static final Charset LEGACY_CSV_FALLBACK =
    // Charset.forName("windows-1252");

    private final Map<ColumnName, Column.Type> explicitColumnTypes;
    private final HeaderStrategy headerStrategy;
    private final char separator;
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
        this.separator = builder.separator;
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

            Charset resolvedCharset = resolveCharset(probe);
            int bomLength = resolveBomLength(probe);
            BufferedCharReader reader = createBufferedCharReader(bufferedInputStream, resolvedCharset, bomLength);
            this.lineReader = isFixedWidths()
                    ? new LineReaderCSVFixedWidth(reader, toReaderOptions())
                    : new LineReaderCSV(reader, toReaderOptions());
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

    private TabularRowReaderCsv toReaderOptions()
    {
        TabularRowReaderCsv options = new TabularRowReaderCsv();
        options.withHeaderStrategy(this.headerStrategy);
        options.withSeparator(this.separator);
        options.withFixedWidths(this.fixedWidths);
        options.withCharset(this.charset);
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
        return new RowProjectedDefault(headerDetection.getSelectedPositions());
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
        private char separator;
        private int[] fixedWidths;
        private Charset charset;
        private boolean autoDetectEncoding;

        private Builder()
        {
            this.explicitColumnTypes = new LinkedHashMap<>();
            this.headerStrategy = new HeaderStrategyAuto(HeaderStrategy.DEFAULT_SCAN_LIMIT);
            this.separator = ',';
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

        public Builder withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.explicitColumnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
            return this;
        }

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
            copy.separator = this.separator;
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
