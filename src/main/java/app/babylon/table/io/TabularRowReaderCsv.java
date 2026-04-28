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
import java.util.function.Predicate;

import app.babylon.io.StreamSource;
import app.babylon.io.StreamSourceProbe;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.column.ColumnName;

public class TabularRowReaderCsv extends TabularRowReaderCommon<TabularRowReaderCsv>
{
    private static final Charset LEGACY_CSV_FALLBACK = Charset.forName("windows-1252");

    private HeaderStrategy headerStrategy;
    private boolean stripping;
    private char separator;
    private char quote;
    private int[] fixedWidths;
    private Charset charset;
    private boolean autoDetectEncoding;

    public TabularRowReaderCsv()
    {
        this.headerStrategy = new HeaderStrategyAuto(HeaderStrategy.DEFAULT_SCAN_LIMIT);
        this.stripping = true;
        this.separator = ',';
        this.quote = '"';
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

    public TabularRowReaderCsv withQuote(char quote)
    {
        this.quote = quote;
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

    public char getQuote()
    {
        return this.quote;
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
    public TabularRowReader.Result read(StreamSource streamSource, RowConsumer rowConsumer)
    {
        StreamSource checkedStreamSource = ArgumentCheck.nonNull(streamSource);
        RowConsumer checkedRowConsumer = ArgumentCheck.nonNull(rowConsumer);
        try (LineReader lineReader = createLineReader(checkedStreamSource))
        {
            RowStreamMarkable parsedRowStream = new RowStreamBuffered(lineReader);
            HeaderDetection headerDetection = getHeaderStrategy().detect(parsedRowStream, getSelectedColumns(null));

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
                    .exception("Failed to read CSV tabular data from '" + checkedStreamSource.getName() + "'.", e);
        }
        catch (IOException e)
        {
            return TabularRowReader.Result.exception(
                    "Failed to read CSV tabular data from '" + checkedStreamSource.getName() + "'.", new TableException(
                            "Failed to read table from stream source '" + checkedStreamSource.getName() + "'.", e));
        }
        catch (RuntimeException e)
        {
            return TabularRowReader.Result
                    .exception("Failed to read CSV tabular data from '" + checkedStreamSource.getName() + "'.", e);
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

    private LineReader createLineReader(StreamSource streamSource) throws IOException
    {
        BufferedInputStream bufferedStream = toBufferedStream(streamSource.openStream());
        StreamSourceProbe probe = StreamSourceProbe.of(bufferedStream, streamSource.getName());
        if (probe.isXlsx() || probe.isXls() || probe.isPdf() || probe.isZip())
        {
            throw new IllegalArgumentException();
        }
        CsvFormat format = this.autoDetectEncoding
                ? CsvFormatProbe.detect(bufferedStream, streamSource.getName(), LEGACY_CSV_FALLBACK, this.separator,
                        this.quote)
                : new CsvFormat(resolveCharset(probe), this.separator, this.quote, 1.0d);
        int bomLength = resolveBomLength(probe);
        BufferedCharReader reader = createBufferedCharReader(bufferedStream, format.charset(), bomLength);
        TabularRowReaderCsv effectiveOptions = copy();
        effectiveOptions.withSeparator(format.separator());
        effectiveOptions.withQuote(format.quote());
        if (isFixedWidths())
        {
            return new LineReaderCSVFixedWidth(reader, effectiveOptions);
        }
        return new LineReaderCSV(reader, effectiveOptions);
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
                : new RowProjectedDefault(headerDetection.getSelectedPositions());
    }

    private ColumnName[] createProjectedColumnNames(HeaderDetection headerDetection)
    {
        ColumnName[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnName[] columnNames = new ColumnName[selectedHeaders.length];
        for (int i = 0; i < selectedHeaders.length; ++i)
        {
            columnNames[i] = getRenameColumnName(selectedHeaders[i]);
        }
        return columnNames;
    }

    private ColumnName getRenameColumnName(ColumnName original)
    {
        if (original == null)
        {
            return null;
        }
        ColumnName renamed = getColumnReName(original);
        return renamed;
    }

    private boolean isFixedWidths()
    {
        return this.fixedWidths != null && this.fixedWidths.length > 0;
    }

    private Charset resolveCharset(StreamSourceProbe probe)
    {
        if (probe == null)
        {
            return LEGACY_CSV_FALLBACK;
        }
        if (this.autoDetectEncoding)
        {
            // Excel-style CSV exports are a common legacy case here. ISO-8859-1 is the
            // other
            // plausible default to consider if caller expectations skew more toward
            // database exports.
            return probe.getCharset(LEGACY_CSV_FALLBACK);
        }
        return this.charset == null ? LEGACY_CSV_FALLBACK : this.charset;
    }

    private TabularRowReaderCsv copy()
    {
        TabularRowReaderCsv copy = new TabularRowReaderCsv();
        copy.headerStrategy = this.headerStrategy;
        copy.stripping = this.stripping;
        copy.separator = this.separator;
        copy.quote = this.quote;
        copy.fixedWidths = this.fixedWidths == null ? null : Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
        copy.charset = this.charset;
        copy.autoDetectEncoding = this.autoDetectEncoding;
        return copy;
    }

    private int resolveBomLength(StreamSourceProbe probe)
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
