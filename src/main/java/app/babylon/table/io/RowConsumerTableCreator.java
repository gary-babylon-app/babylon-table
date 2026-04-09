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

import java.util.Collections;
import java.util.Map;

import app.babylon.io.DataSource;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBuilder;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public final class RowConsumerTableCreator implements RowConsumerResult<TableColumnar>
{
    private static final byte KIND_STRING = 0;
    private static final byte KIND_DOUBLE = 1;

    private final CsvRowFilter rowFilter;
    private final byte[] columnKinds;
    private final ColumnBuilder[] columnBuilders;
    private final String[] strippedValues;
    private final Csv.ReadSettings options;

    RowConsumerTableCreator(Csv.ReadSettings options, CsvRowFilter rowFilter, byte[] columnKinds,
            ColumnBuilder[] columnBuilders, String[] strippedValues)
    {
        this.options = options;
        this.rowFilter = rowFilter;
        this.columnKinds = columnKinds;
        this.columnBuilders = columnBuilders;
        this.strippedValues = strippedValues;
    }

    @Override
    public void accept(Row rowValues)
    {
        int columnCount = this.columnBuilders.length;
        if (rowValues.fieldCount() != columnCount)
        {
            return;
        }
        boolean isEmpty = true;
        char[] chars = rowValues.chars();
        for (int i = 0; i < columnCount; ++i)
        {
            String s = new String(chars, rowValues.start(i), rowValues.length(i));
            this.strippedValues[i] = s;
            if (!Strings.isEmpty(s))
            {
                isEmpty = false;
            }
        }
        if (isEmpty)
        {
            return;
        }
        boolean include = this.rowFilter.isInclude(this.strippedValues);
        boolean exclude = this.rowFilter.isExclude(this.strippedValues);
        if (exclude || !include)
        {
            return;
        }
        for (int i = 0; i < columnCount; ++i)
        {
            switch (this.columnKinds[i])
            {
                case KIND_DOUBLE :
                    if (Strings.isEmpty(this.strippedValues[i]))
                    {
                        ((ColumnDouble.Builder) this.columnBuilders[i]).addNull();
                    } else
                    {
                        ((ColumnDouble.Builder) this.columnBuilders[i]).add(chars, rowValues.start(i),
                                rowValues.length(i));
                    }
                    break;
                case KIND_STRING :
                default :
                    @SuppressWarnings("unchecked")
                    ColumnObject.Builder<String> builder = (ColumnObject.Builder<String>) this.columnBuilders[i];
                    builder.add(this.strippedValues[i]);
                    break;
            }
        }
    }

    @Override
    public TableColumnar buildResult(DataSource dataSource)
    {
        TableName name = this.options.getTableName();
        if (name == null)
        {
            name = TableName.of(extractLastPart(dataSource.getName()));
        }
        if (this.options.includeResourceName() && this.columnBuilders.length > 0)
        {
            Column[] builtColumns = new Column[this.columnBuilders.length + 1];
            for (int i = 0; i < this.columnBuilders.length; ++i)
            {
                builtColumns[i + 1] = this.columnBuilders[i].build();
            }
            int rowCount = builtColumns.length > 1 ? builtColumns[1].size() : 0;
            builtColumns[0] = Columns.newString(this.options.getResourceName(), dataSource.getName(), rowCount);
            return Tables.newTable(name, builtColumns);
        }
        return Tables.newTable(name, this.columnBuilders);
    }

    public static RowConsumerTableCreator create(Csv.ReadSettings options, HeaderDetection headerDetection)
    {
        return create(options, headerDetection, Collections.emptyMap());
    }

    public static RowConsumerTableCreator create(Csv.ReadSettings options, HeaderDetection headerDetection,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        String[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnName[] columnNames = initialiseSelectedColumns(options, selectedHeaders);
        byte[] columnKinds = initialiseColumnKinds(options, columnNames, explicitColumnTypes);
        ColumnBuilder[] columnBuilders = initialiseColumnBuilders(options, columnNames, explicitColumnTypes);
        CsvRowFilter rowFilter = new CsvRowFilter(options, columnNames);
        return new RowConsumerTableCreator(options, rowFilter, columnKinds, columnBuilders,
                new String[columnBuilders.length]);
    }

    public static RowConsumerFactory<TableColumnar> factory()
    {
        return RowConsumerTableCreator::create;
    }

    public static RowConsumerFactory<TableColumnar> factory(Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        return (options, headerDetection) -> create(options, headerDetection, explicitColumnTypes);
    }

    private static ColumnName[] initialiseSelectedColumns(Csv.ReadSettings options, String[] selectedHeadersFound)
    {
        ColumnName[] columnNames = new ColumnName[selectedHeadersFound.length];
        for (int i = 0; i < selectedHeadersFound.length; ++i)
        {
            String selectedHeader = selectedHeadersFound[i];
            columnNames[i] = options.getRenameColumnName(selectedHeader);
        }
        return columnNames;
    }

    private static byte[] initialiseColumnKinds(Csv.ReadSettings options, ColumnName[] columnNames,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        byte[] columnKinds = new byte[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i)
        {
            app.babylon.table.column.Column.Type columnType = explicitColumnTypes.get(columnNames[i]);
            if (ColumnDouble.TYPE.equals(columnType))
            {
                columnKinds[i] = KIND_DOUBLE;
            } else
            {
                columnKinds[i] = KIND_STRING;
            }
        }
        return columnKinds;
    }

    private static ColumnBuilder[] initialiseColumnBuilders(Csv.ReadSettings options, ColumnName[] columnNames,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        ColumnBuilder[] columnBuilders = new ColumnBuilder[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i)
        {
            ColumnName columnName = columnNames[i];
            app.babylon.table.column.Column.Type columnType = explicitColumnTypes.get(columnName);
            if (ColumnDouble.TYPE.equals(columnType))
            {
                columnBuilders[i] = ColumnDouble.builder(columnName);
            } else
            {
                columnBuilders[i] = ColumnObject.builder(columnName, String.class);
            }
        }
        return columnBuilders;
    }

    private static String extractLastPart(String s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        int lastSlash = s.lastIndexOf('/');
        int lastPeriod = s.lastIndexOf('.');
        if (lastPeriod >= 0 && lastPeriod > lastSlash)
        {
            return s.substring(lastSlash + 1, lastPeriod);
        }
        return s;
    }
}
