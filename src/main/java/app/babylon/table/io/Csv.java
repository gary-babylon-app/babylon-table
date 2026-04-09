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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.io.DataSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableException;
import app.babylon.table.TableName;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class Csv
{
    static final int DEFAULT_HEADER_SCAN_LIMIT = 25;

    public static class ReadSettings
    {
        Map<ColumnName, ColumnName> renameHeaders;
        Set<ColumnName> selectedColumns;
        LineReaderFactory lineReaderFactory;
        TableName tableName;
        ColumnName resourceName;
        HeaderStrategy headerStrategy;
        boolean stripping;
        char separator;
        Map<ColumnName, Predicate<String>> rowIncludeFilters;
        Map<ColumnName, Predicate<String>> rowExcludeFilters;
        int[] fixedWidths;
        Charset charset;
        boolean autoDetectEncoding;

        public ReadSettings()
        {
            this.renameHeaders = new HashMap<>();
            this.selectedColumns = new LinkedHashSet<>();
            this.lineReaderFactory = null;
            this.tableName = null;
            this.resourceName = null;
            this.headerStrategy = new HeaderStrategyAuto(Csv.DEFAULT_HEADER_SCAN_LIMIT);
            this.stripping = true;
            this.separator = ',';
            this.rowIncludeFilters = new HashMap<>();
            this.rowExcludeFilters = new HashMap<>();
            this.fixedWidths = null;
            this.charset = null;
            this.autoDetectEncoding = true;
        }

        public ReadSettings withCharset(Charset charset)
        {
            this.charset = charset;
            return this;
        }

        public ReadSettings withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public ReadSettings withAutoDetectEncoding(boolean autoDetectEncoding)
        {
            this.autoDetectEncoding = autoDetectEncoding;
            return this;
        }

        public ReadSettings withRowIncludeFilter(ColumnName x, Predicate<String> filter)
        {
            if (filter != null)
            {
                this.rowIncludeFilters.put(x, filter);
            }
            return this;
        }

        public ReadSettings withRowExcludeFilter(ColumnName x, Predicate<String> filter)
        {
            if (filter != null)
            {
                this.rowExcludeFilters.put(x, filter);
            }
            return this;
        }

        public ReadSettings withFixedWidths(int[] fixedWiths)
        {
            if (fixedWiths == null)
            {
                this.fixedWidths = null;
                return this;
            }
            this.fixedWidths = Arrays.copyOf(fixedWiths, fixedWiths.length);
            return this;
        }

        public ReadSettings withIncludeResourceName(ColumnName x)
        {
            this.resourceName = x;
            return this;
        }

        public ReadSettings withStripping(boolean stripping)
        {
            this.stripping = stripping;
            return this;
        }

        public ReadSettings withTableName(TableName tableName)
        {
            this.tableName = tableName;
            return this;
        }

        public ReadSettings withHeaderStrategy(HeaderStrategy headerStrategy)
        {
            ArgumentCheck.nonNull(headerStrategy);
            this.headerStrategy = headerStrategy;
            return this;
        }

        public ReadSettings withLineReaderFactory(LineReaderFactory lineReaderFactory)
        {
            this.lineReaderFactory = lineReaderFactory;
            return this;
        }

        public ReadSettings withSelectedColumn(ColumnName x)
        {
            this.selectedColumns.add(x);
            return this;
        }

        public ReadSettings withSelectedColumns(ColumnName... x)
        {
            if (x != null)
            {
                this.selectedColumns.addAll(Arrays.asList(x));
            }
            return this;
        }

        public ReadSettings withColumnRename(ColumnName original, ColumnName newName)
        {
            ArgumentCheck.nonNull(original);
            ArgumentCheck.nonNull(newName);
            if (this.renameHeaders.containsKey(original))
            {
                throw new RuntimeException("Rename failed, column " + original + " already renamed");
            }
            this.renameHeaders.put(original, newName);
            return this;
        }

        public HeaderStrategy getHeaderStrategy()
        {
            return this.headerStrategy;
        }

        public LineReaderFactory getLineReaderFactory()
        {
            return this.lineReaderFactory;
        }

        public boolean includeResourceName()
        {
            return this.resourceName != null;
        }

        public ColumnName getResourceName()
        {
            return this.resourceName;
        }

        public boolean isStripping()
        {
            return this.stripping;
        }

        public TableName getTableName()
        {
            return this.tableName;
        }

        public ColumnName getRenameColumnName(String original)
        {
            if (Strings.isEmpty(original))
            {
                return null;
            }
            return getRenameColumnName(ColumnName.of(original));
        }

        public ColumnName getRenameColumnName(ColumnName original)
        {
            ColumnName r = this.renameHeaders.get(original);
            if (r == null)
            {
                return original;
            }
            return r;
        }

        public Collection<ColumnName> getSelectedColumns(Collection<ColumnName> x)
        {
            if (x == null)
            {
                x = new ArrayList<>();
            }
            x.addAll(this.selectedColumns);
            return x;
        }

        public Charset getCharset()
        {
            return this.charset;
        }

        public boolean hasCharset()
        {
            return this.charset != null;
        }

        public boolean isAutoDetectEncoding()
        {
            return this.autoDetectEncoding;
        }

        public boolean isFixedWidths()
        {
            return !Is.empty(this.fixedWidths);
        }

        public int[] getFixedWidths()
        {
            if (this.fixedWidths == null)
            {
                return null;
            }
            return Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
        }

        public char getSeparator()
        {
            return this.separator;
        }

        public boolean hasRowIncludeFilters()
        {
            return this.rowIncludeFilters.size() > 0;
        }

        public boolean hasRowExcludeFilters()
        {
            return this.rowExcludeFilters.size() > 0;
        }

        public Predicate<String> getRowIncludeFilter(ColumnName x)
        {
            return this.rowIncludeFilters.get(x);
        }

        public Predicate<String> getRowExcludeFilter(ColumnName x)
        {
            return this.rowExcludeFilters.get(x);
        }

    }

    public static TableColumnar read(DataSource ds, ReadSettings options)
    {
        HeaderStrategy headerStrategy = options == null ? null : options.getHeaderStrategy();
        return read(ds, options, headerStrategy, RowConsumerTableCreator.factory());
    }

    public static TableColumnar read(DataSource ds, ReadSettings options, HeaderStrategy headerStrategy)
    {
        return read(ds, options, headerStrategy, RowConsumerTableCreator.factory());
    }

    public static <T> T read(DataSource ds, ReadSettings options, RowConsumerFactory<T> rowConsumerFactory)
    {
        HeaderStrategy headerStrategy = options == null ? null : options.getHeaderStrategy();
        return read(ds, options, headerStrategy, rowConsumerFactory);
    }

    public static <T> T read(DataSource ds, ReadSettings options, HeaderStrategy headerStrategy,
            RowConsumerFactory<T> rowConsumerFactory)
    {
        if (options == null)
        {
            options = new ReadSettings();
        }
        if (headerStrategy == null)
        {
            headerStrategy = new HeaderStrategyAuto();
        }
        if (rowConsumerFactory == null)
        {
            throw new IllegalArgumentException("rowConsumerFactory must not be null");
        }
        LineReaderFactory lineReaderFactory = options.getLineReaderFactory();
        if (lineReaderFactory == null)
        {
            lineReaderFactory = new LineReaderFactoryCSV();
        }
        try (LineReader lineReader = lineReaderFactory.create(ds, options))
        {
            RowStreamMarkable parsedRowStream = new RowStreamBuffered(lineReader);

            HeaderDetection headerDetection = headerStrategy.detect(parsedRowStream, options);

            RowConsumerResult<T> rowConsumer = rowConsumerFactory.create(options, headerDetection);
            RowProjected projectedRow = createRowProjected(options, headerDetection);

            parsedRowStream.reset();
            while (parsedRowStream.next())
            {
                rowConsumer.accept(projectedRow.with(parsedRowStream.current()));
            }
            return rowConsumer.buildResult(ds);
        } catch (TableException e)
        {
            throw e;
        } catch (IOException e)
        {
            throw new TableException("Failed to read table from data source '" + ds.getName() + "'.", e);
        }
    }

    private static RowProjected createRowProjected(ReadSettings options, HeaderDetection headerDetection)
    {
        RowProjected projectedRow = options.isStripping()
                ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                : new ProjectedRow(headerDetection.getSelectedPositions());
        return projectedRow;
    }
}
