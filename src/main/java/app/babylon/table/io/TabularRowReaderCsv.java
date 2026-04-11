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
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.io.DataSource;
import app.babylon.io.DataSourceProbe;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.column.ColumnName;

public class TabularRowReaderCsv extends TabularRowReaderCommon<TabularRowReaderCsv>
{
    private HeaderStrategy headerStrategy;
    private boolean stripping;
    private char separator;
    private int[] fixedWidths;
    private Charset charset;
    private boolean autoDetectEncoding;

    public TabularRowReaderCsv()
    {
        this.headerStrategy = new HeaderStrategyAuto(HeaderStrategy.DEFAULT_SCAN_LIMIT);
        this.stripping = true;
        this.separator = ',';
        this.fixedWidths = null;
        this.charset = StandardCharsets.UTF_8;
        this.autoDetectEncoding = true;
    }

    public TabularRowReaderCsv withHeaderStrategy(HeaderStrategy headerStrategy)
    {
        this.headerStrategy = ArgumentCheck.nonNull(headerStrategy);
        return this;
    }

    public TabularRowReaderCsv withStripping(boolean stripping)
    {
        this.stripping = stripping;
        return this;
    }

    public TabularRowReaderCsv withSeparator(char separator)
    {
        this.separator = separator;
        return this;
    }

    public TabularRowReaderCsv withFixedWidths(int[] fixedWidths)
    {
        this.fixedWidths = fixedWidths == null ? null : Arrays.copyOf(fixedWidths, fixedWidths.length);
        return this;
    }

    public TabularRowReaderCsv withCharset(Charset charset)
    {
        this.charset = ArgumentCheck.nonNull(charset);
        return this;
    }

    public TabularRowReaderCsv withAutoDetectEncoding(boolean autoDetectEncoding)
    {
        this.autoDetectEncoding = autoDetectEncoding;
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

    @Override
    public TabularRowReader.Result read(DataSource dataSource, RowConsumer rowConsumer)
    {
        DataSource checkedDataSource = ArgumentCheck.nonNull(dataSource);
        RowConsumer checkedRowConsumer = ArgumentCheck.nonNull(rowConsumer);
        try (LineReader lineReader = createLineReader(checkedDataSource))
        {
            RowStreamMarkable parsedRowStream = new RowStreamBuffered(lineReader);
            HeaderDetection headerDetection = getHeaderStrategy().detect(parsedRowStream, selectedColumns());

            final ColumnName[] projectedColumnNames = createProjectedColumnNames(headerDetection);
            RowProjected projectedRow = createRowProjected(headerDetection);

            checkedRowConsumer.start(projectedColumnNames);

            Predicate<Row> boundRowFilter = getBoundRowFilter(projectedColumnNames);

            parsedRowStream.reset();

            if (boundRowFilter == null)
            {
                while (parsedRowStream.next())
                {
                    checkedRowConsumer.accept(projectedRow.with(parsedRowStream.current()));
                }
            }
            else
            {
                while (parsedRowStream.next())
                {
                    Row row = projectedRow.with(parsedRowStream.current());
                    if (!boundRowFilter.test(row))
                    {
                        continue;
                    }
                    checkedRowConsumer.accept(row);
                }
            }
            return TabularRowReader.Result.success();

        }
        catch (TableException e)
        {
            return TabularRowReader.Result
                    .exception("Failed to read CSV tabular data from '" + checkedDataSource.getName() + "'.", e);
        }
        catch (IOException e)
        {
            return TabularRowReader.Result.exception(
                    "Failed to read CSV tabular data from '" + checkedDataSource.getName() + "'.", new TableException(
                            "Failed to read table from data source '" + checkedDataSource.getName() + "'.", e));
        }
        catch (RuntimeException e)
        {
            return TabularRowReader.Result
                    .exception("Failed to read CSV tabular data from '" + checkedDataSource.getName() + "'.", e);
        }
    }

    private Predicate<Row> getBoundRowFilter(final ColumnName[] projectedColumnNames)
    {
        Predicate<Row> boundRowFilter = getRowFilter() == null ? null : getRowFilter().bind(projectedColumnNames);
        return boundRowFilter;
    }

    @Override
    protected TabularRowReaderCsv self()
    {
        return this;
    }

    private LineReader createLineReader(DataSource dataSource) throws IOException
    {
        BufferedInputStream bufferedStream = toBufferedStream(dataSource.openStream());
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

    private static BufferedInputStream toBufferedStream(InputStream instream)
    {
        if (instream instanceof BufferedInputStream bufferedInputStream)
        {
            return bufferedInputStream;
        }
        return new BufferedInputStream(instream);
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
        }
        catch (IOException e)
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

}
