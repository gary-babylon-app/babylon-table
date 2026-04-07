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

import app.babylon.table.Column;
import app.babylon.table.ColumnBuilder;
import app.babylon.table.ColumnDouble;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.Columns;
import app.babylon.table.Strings;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;

final class RowConsumerTableBuilding implements RowConsumerResult<TableColumnar>
{
    private static final byte KIND_STRING = 0;
    private static final byte KIND_DOUBLE = 1;

    private final RowFilter rowFilter;
    private final byte[] columnKinds;
    private final ColumnBuilder[] columnBuilders;
    private final String[] strippedValues;
    private final ReadSettingsCSV options;

    RowConsumerTableBuilding(
            ReadSettingsCSV options,
            RowFilter rowFilter,
            byte[] columnKinds,
            ColumnBuilder[] columnBuilders,
            String[] strippedValues)
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
            case KIND_DOUBLE:
                if (Strings.isEmpty(this.strippedValues[i]))
                {
                    ((ColumnDouble.Builder) this.columnBuilders[i]).addNull();
                }
                else
                {
                    ((ColumnDouble.Builder) this.columnBuilders[i]).add(chars, rowValues.start(i), rowValues.length(i));
                }
                break;
            case KIND_STRING:
            default:
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

    static RowConsumerTableBuilding create(ReadSettingsCSV options, HeaderDetection headerDetection)
    {
        String[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnName[] columnNames = initialiseSelectedColumns(options, selectedHeaders);
        byte[] columnKinds = initialiseColumnKinds(options, columnNames);
        ColumnBuilder[] columnBuilders = initialiseColumnBuilders(options, columnNames);
        RowFilter rowFilter = new RowFilter(options, columnNames);
        return new RowConsumerTableBuilding(
                options,
                rowFilter,
                columnKinds,
                columnBuilders,
                new String[columnBuilders.length]);
    }

    static RowConsumerFactory<TableColumnar> factory()
    {
        return RowConsumerTableBuilding::create;
    }

    private static ColumnName[] initialiseSelectedColumns(ReadSettingsCSV options, String[] selectedHeadersFound)
    {
        ColumnName[] columnNames = new ColumnName[selectedHeadersFound.length];
        for (int i = 0; i < selectedHeadersFound.length; ++i)
        {
            String selectedHeader = selectedHeadersFound[i];
            columnNames[i] = options.getRenameColumnName(selectedHeader);
        }
        return columnNames;
    }

    private static byte[] initialiseColumnKinds(ReadSettingsCSV options, ColumnName[] columnNames)
    {
        byte[] columnKinds = new byte[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i)
        {
            app.babylon.table.Column.Type columnType = options.getColumnType(columnNames[i]);
            if (ColumnDouble.TYPE.equals(columnType))
            {
                columnKinds[i] = KIND_DOUBLE;
            }
            else
            {
                columnKinds[i] = KIND_STRING;
            }
        }
        return columnKinds;
    }

    private static ColumnBuilder[] initialiseColumnBuilders(
            ReadSettingsCSV options,
            ColumnName[] columnNames)
    {
        ColumnBuilder[] columnBuilders = new ColumnBuilder[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i)
        {
            ColumnName columnName = columnNames[i];
            app.babylon.table.Column.Type columnType = options.getColumnType(columnName);
            if (ColumnDouble.TYPE.equals(columnType))
            {
                columnBuilders[i] = ColumnDouble.builder(columnName);
            }
            else
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
