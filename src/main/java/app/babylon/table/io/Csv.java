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
        TableName tableName;
        ColumnName resourceName;
        boolean stripping;
        char separator;
        int[] fixedWidths;
        Charset charset;
        boolean autoDetectEncoding;

        public ReadSettings()
        {
            this.renameHeaders = new HashMap<>();
            this.selectedColumns = new LinkedHashSet<>();
            this.tableName = null;
            this.resourceName = null;
            this.stripping = true;
            this.separator = ',';
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

        public Set<ColumnName> getSelectedColumns()
        {
            return Set.copyOf(this.selectedColumns);
        }

        public Map<ColumnName, ColumnName> getColumnRenames()
        {
            return Map.copyOf(this.renameHeaders);
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
    }

    public static TableColumnar read(DataSource ds, ReadSettings options, HeaderStrategy headerStrategy)
    {
        ReadSettings effectiveOptions = options == null ? new ReadSettings() : options;
        return read(ds, effectiveOptions, headerStrategy,
                RowConsumerCreateTable.create(effectiveOptions.getTableName(), effectiveOptions.getResourceName()));
    }

    public static TableColumnar read(DataSource ds, ReadSettings options, HeaderStrategy headerStrategy,
            RowConsumer<TableColumnar> rowConsumer)
    {
        if (options == null)
        {
            options = new ReadSettings();
        }
        if (headerStrategy == null)
        {
            headerStrategy = new HeaderStrategyAuto();
        }
        if (rowConsumer == null)
        {
            throw new IllegalArgumentException("rowConsumer must not be null");
        }
        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(headerStrategy)
                .withStripping(options.isStripping()).withSeparator(options.getSeparator())
                .withFixedWidths(options.getFixedWidths()).withAutoDetectEncoding(options.isAutoDetectEncoding())
                .withRowConsumer(rowConsumer);
        if (options.hasCharset())
        {
            reader.withCharset(options.getCharset());
        }
        if (options.getTableName() != null)
        {
            reader.withTableName(options.getTableName());
        }
        if (options.includeResourceName())
        {
            reader.withIncludeResourceName(options.getResourceName());
        }
        for (ColumnName selectedColumn : options.getSelectedColumns(new ArrayList<>()))
        {
            reader.withSelectedColumn(selectedColumn);
        }
        for (Map.Entry<ColumnName, ColumnName> entry : options.renameHeaders.entrySet())
        {
            reader.withColumnRename(entry.getKey(), entry.getValue());
        }
        TabularReader.Result result = reader.read(ds);
        if (result.getStatus() == TabularReader.Status.EXCEPTION)
        {
            Throwable cause = result.getCause();
            if (cause instanceof TableException)
            {
                throw (TableException) cause;
            }
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            throw new TableException("Failed to read table from data source '" + ds.getName() + "'.", cause);
        }
        return result.getTable();
    }

    private static RowProjected createRowProjected(ReadSettings options, HeaderDetection headerDetection)
    {
        RowProjected projectedRow = options.isStripping()
                ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                : new ProjectedRow(headerDetection.getSelectedPositions());
        return projectedRow;
    }

    private static ColumnName[] createProjectedColumnNames(ReadSettings options, HeaderDetection headerDetection)
    {
        String[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnName[] columnNames = new ColumnName[selectedHeaders.length];
        for (int i = 0; i < selectedHeaders.length; ++i)
        {
            columnNames[i] = options.getRenameColumnName(selectedHeaders[i]);
        }
        return columnNames;
    }

    private static Set<ColumnName> selectedColumns(ReadSettings options)
    {
        if (options == null)
        {
            return Set.of();
        }
        return new LinkedHashSet<>(options.getSelectedColumns(new ArrayList<>()));
    }
}
