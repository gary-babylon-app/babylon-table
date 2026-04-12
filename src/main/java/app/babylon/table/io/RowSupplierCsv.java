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

import app.babylon.io.DataSourceProbe;
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
public final class RowSupplierCsv implements RowSupplier
{
    private final Map<ColumnName, Column.Type> explicitColumnTypes;
    private final HeaderStrategy headerStrategy;
    private final char separator;
    private final int[] fixedWidths;
    private final Charset charset;
    private final boolean autoDetectEncoding;
    private final LineReader lineReader;
    private final RowStreamMarkable rowStream;
    private final ColumnDefinition[] columnDefinitions;

    RowSupplierCsv(InputStream inputStream, HeaderStrategy headerStrategy, char separator, int[] fixedWidths,
            Charset charset, boolean autoDetectEncoding, Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        BufferedInputStream bufferedInputStream = toBufferedStream(ArgumentCheck.nonNull(inputStream));
        this.explicitColumnTypes = new LinkedHashMap<>(ArgumentCheck.nonNull(explicitColumnTypes));
        this.headerStrategy = ArgumentCheck.nonNull(headerStrategy);
        this.separator = separator;
        this.fixedWidths = fixedWidths == null ? null : Arrays.copyOf(fixedWidths, fixedWidths.length);
        this.charset = ArgumentCheck.nonNull(charset);
        this.autoDetectEncoding = autoDetectEncoding;

        try
        {
            DataSourceProbe probe = DataSourceProbe.of(bufferedInputStream, "stream.csv");
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
        return this.rowStream.current();
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

    private boolean isFixedWidths()
    {
        return this.fixedWidths != null && this.fixedWidths.length > 0;
    }

    private Charset resolveCharset(DataSourceProbe probe)
    {
        if (this.autoDetectEncoding)
        {
            Charset detected = probe.detectedCharset();
            return detected == null ? StandardCharsets.UTF_8 : detected;
        }
        return this.charset;
    }

    private int resolveBomLength(DataSourceProbe probe)
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

        public RowSupplierCsv build(InputStream inputStream)
        {
            return new RowSupplierCsv(inputStream, this.headerStrategy, this.separator, this.fixedWidths, this.charset,
                    this.autoDetectEncoding, this.explicitColumnTypes);
        }
    }
}
