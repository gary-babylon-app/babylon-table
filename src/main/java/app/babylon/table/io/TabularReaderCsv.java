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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.io.DataSource;
import app.babylon.io.DataSourceProbe;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.TableName;
import app.babylon.table.column.ColumnName;

public class TabularReaderCsv<T> extends TabularReaderCommon<T, TabularReaderCsv<T>>
{
    private HeaderStrategy headerStrategy;
    private boolean stripping;
    private char separator;
    private int[] fixedWidths;
    private Charset charset;
    private boolean autoDetectEncoding;
    private TableName tableName;
    private ColumnName resourceName;

    public TabularReaderCsv()
    {
        this.headerStrategy = new HeaderStrategyAuto(Csv.DEFAULT_HEADER_SCAN_LIMIT);
        this.stripping = true;
        this.separator = ',';
        this.fixedWidths = null;
        this.charset = StandardCharsets.UTF_8;
        this.autoDetectEncoding = true;
        this.tableName = null;
        this.resourceName = null;
    }

    public TabularReaderCsv<T> withHeaderStrategy(HeaderStrategy headerStrategy)
    {
        this.headerStrategy = ArgumentCheck.nonNull(headerStrategy);
        return this;
    }

    public TabularReaderCsv<T> withStripping(boolean stripping)
    {
        this.stripping = stripping;
        return this;
    }

    public TabularReaderCsv<T> withSeparator(char separator)
    {
        this.separator = separator;
        return this;
    }

    public TabularReaderCsv<T> withFixedWidths(int[] fixedWidths)
    {
        this.fixedWidths = fixedWidths == null ? null : fixedWidths.clone();
        return this;
    }

    public TabularReaderCsv<T> withCharset(Charset charset)
    {
        this.charset = ArgumentCheck.nonNull(charset);
        return this;
    }

    public TabularReaderCsv<T> withAutoDetectEncoding(boolean autoDetectEncoding)
    {
        this.autoDetectEncoding = autoDetectEncoding;
        return this;
    }

    public TabularReaderCsv<T> withTableName(TableName tableName)
    {
        this.tableName = ArgumentCheck.nonNull(tableName);
        return this;
    }

    public TabularReaderCsv<T> withIncludeResourceName(ColumnName resourceName)
    {
        this.resourceName = ArgumentCheck.nonNull(resourceName);
        return this;
    }

    public HeaderStrategy getHeaderStrategy()
    {
        return this.headerStrategy;
    }

    public boolean isStripping()
    {
        return this.stripping;
    }

    public char getSeparator()
    {
        return this.separator;
    }

    public int[] getFixedWidths()
    {
        return this.fixedWidths == null ? null : Arrays.copyOf(fixedWidths, fixedWidths.length);
    }

    public Charset getCharset()
    {
        return this.charset;
    }

    public boolean isAutoDetectEncoding()
    {
        return this.autoDetectEncoding;
    }

    public TableName getTableName()
    {
        return this.tableName;
    }

    public ColumnName getResourceName()
    {
        return this.resourceName;
    }

    @Override
    public TabularReadResult<T> read(DataSource dataSource)
    {
        DataSource checkedDataSource = ArgumentCheck.nonNull(dataSource);
        RowConsumer<T> rowConsumer = rowConsumer();
        if (rowConsumer instanceof RowConsumerTableCreator)
        {
            ((RowConsumerTableCreator) rowConsumer).setSourceName(checkedDataSource.getName());
        }
        try (LineReader lineReader = createLineReader(checkedDataSource))
        {
            RowStreamMarkable parsedRowStream = new RowStreamBuffered(lineReader);
            HeaderDetection headerDetection = getHeaderStrategy().detect(parsedRowStream, selectedColumns());

            final ColumnName[] projectedColumnNames = createProjectedColumnNames(headerDetection);

            rowConsumer.start(projectedColumnNames);

            Predicate<Row> boundRowFilter = getBoundRowFilter(projectedColumnNames);

            RowProjected projectedRow = createRowProjected(headerDetection);

            parsedRowStream.reset();

            while (parsedRowStream.next())
            {
                Row row = projectedRow.with(parsedRowStream.current());
                if (boundRowFilter != null && !boundRowFilter.test(row))
                {
                    continue;
                }
                rowConsumer.accept(row);
            }
            return TabularReadResult.success(rowConsumer.build());

        } catch (TableException e)
        {
            return TabularReadResult
                    .exception("Failed to read CSV tabular data from '" + checkedDataSource.getName() + "'.", e);
        } catch (IOException e)
        {
            return TabularReadResult.exception(
                    "Failed to read CSV tabular data from '" + checkedDataSource.getName() + "'.", new TableException(
                            "Failed to read table from data source '" + checkedDataSource.getName() + "'.", e));
        } catch (RuntimeException e)
        {
            return TabularReadResult
                    .exception("Failed to read CSV tabular data from '" + checkedDataSource.getName() + "'.", e);
        }
    }

    private Predicate<Row> getBoundRowFilter(final ColumnName[] projectedColumnNames)
    {
        Predicate<Row> boundRowFilter = getRowFilter() == null ? null : getRowFilter().bind(projectedColumnNames);
        return boundRowFilter;
    }

    @Override
    protected TabularReaderCsv<T> self()
    {
        return this;
    }

    private Csv.ReadSettings createCsvReadSettings()
    {
        Csv.ReadSettings csvReadSettings = new Csv.ReadSettings().withStripping(this.stripping)
                .withSeparator(this.separator).withFixedWidths(this.fixedWidths).withCharset(this.charset)
                .withAutoDetectEncoding(this.autoDetectEncoding);
        if (this.tableName != null)
        {
            csvReadSettings.withTableName(this.tableName);
        }
        if (this.resourceName != null)
        {
            csvReadSettings.withIncludeResourceName(this.resourceName);
        }
        for (ColumnName selectedColumn : getSelectedColumns())
        {
            csvReadSettings.withSelectedColumn(selectedColumn);
        }
        for (Map.Entry<ColumnName, ColumnName> entry : getColumnRenames().entrySet())
        {
            csvReadSettings.withColumnRename(entry.getKey(), entry.getValue());
        }
        return csvReadSettings;
    }

    private LineReader createLineReader(DataSource dataSource) throws IOException
    {
        BufferedInputStream bufferedStream = LineReaderFactoryCSV.toBufferedStream(dataSource.openStream());
        DataSourceProbe probe = DataSourceProbe.of(bufferedStream, dataSource.getName());
        if (probe.isXlsx() || probe.isXls() || probe.isPdf() || probe.isZip())
        {
            throw new IllegalArgumentException();
        }
        Charset resolvedCharset = resolveCharset(probe);
        int bomLength = resolveBomLength(probe);
        BufferedCharReader reader = createBufferedCharReader(bufferedStream, resolvedCharset, bomLength);
        if (isFixedWidths())
        {
            return new LineReaderCSVFixedWidth(reader, this);
        }
        return new LineReaderCSV(reader, this);
    }

    private RowProjected createRowProjected(HeaderDetection headerDetection)
    {
        return this.stripping
                ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                : new ProjectedRow(headerDetection.getSelectedPositions());
    }
    private ColumnName[] createProjectedColumnNames(HeaderDetection headerDetection)
    {
        String[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnName[] columnNames = new ColumnName[selectedHeaders.length];
        for (int i = 0; i < selectedHeaders.length; ++i)
        {
            columnNames[i] = getRenameColumnName(selectedHeaders[i]);
        }
        return columnNames;
    }
    private ColumnName getRenameColumnName(String original)
    {
        if (original == null)
        {
            return null;
        }
        ColumnName originalColumnName = ColumnName.of(original);
        ColumnName renamed = getColumnRenames().get(originalColumnName);
        return renamed == null ? originalColumnName : renamed;
    }

    private Set<ColumnName> selectedColumns()
    {
        return new LinkedHashSet<>(getSelectedColumns());
    }

    private boolean isFixedWidths()
    {
        return this.fixedWidths != null && this.fixedWidths.length > 0;
    }

    private Charset resolveCharset(DataSourceProbe probe)
    {
        if (probe == null)
        {
            return java.nio.charset.StandardCharsets.UTF_8;
        }
        if (this.autoDetectEncoding)
        {
            Charset detected = probe.detectedCharset();
            return detected == null ? java.nio.charset.StandardCharsets.UTF_8 : detected;
        }
        return this.charset == null ? java.nio.charset.StandardCharsets.UTF_8 : this.charset;
    }

    private int resolveBomLength(DataSourceProbe probe)
    {
        if (probe == null)
        {
            return 0;
        }
        return this.autoDetectEncoding ? probe.bomLengthBytes() : 0;
    }

    private BufferedCharReader createBufferedCharReader(InputStream instream, Charset charset, int bomLength)
    {
        try
        {
            skipBytes(instream, bomLength);
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

    @SuppressWarnings("unchecked")
    private RowConsumer<T> rowConsumer()
    {
        RowConsumer<T> rowConsumer = getRowConsumer();
        if (rowConsumer != null)
        {
            return rowConsumer;
        }
        return (RowConsumer<T>) RowConsumerTableCreator.create(createCsvReadSettings());
    }
}
