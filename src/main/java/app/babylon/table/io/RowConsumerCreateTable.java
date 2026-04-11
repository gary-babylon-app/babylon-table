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
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.CharSliceBuilder;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBuilder;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public final class RowConsumerCreateTable implements RowConsumer<TableColumnar>
{
    private final TableName tableName;
    private final ColumnName resourceName;
    private final Map<ColumnName, Column.Type> explicitColumnTypes;
    private ColumnBuilder[] columnBuilders;
    private CharSliceBuilder[] charSliceBuilders;
    private String sourceName;

    RowConsumerCreateTable(TableName tableName, ColumnName resourceName,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        this.tableName = tableName;
        this.resourceName = resourceName;
        this.explicitColumnTypes = explicitColumnTypes;
        this.columnBuilders = null;
        this.charSliceBuilders = null;
        this.sourceName = null;
    }

    @Override
    public void start(ColumnName[] columnNames)
    {
        this.columnBuilders = new ColumnBuilder[columnNames.length];
        this.charSliceBuilders = new CharSliceBuilder[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i)
        {
            Column.Type columnType = effectiveColumnType(columnNames[i], this.explicitColumnTypes);
            CharSliceBuilder builder = Columns.newCharSliceBuilder(columnNames[i], columnType);
            this.columnBuilders[i] = builder;
            this.charSliceBuilders[i] = builder;
        }
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
            if (rowValues.length(i) > 0)
            {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty)
        {
            return;
        }
        for (int i = 0; i < columnCount; ++i)
        {
            this.charSliceBuilders[i].add(chars, rowValues.start(i), rowValues.length(i));
        }
    }

    @Override
    public TableColumnar build()
    {
        TableName name = this.tableName;
        if (name == null)
        {
            name = TableName.of(extractLastPart(this.sourceName));
        }
        if (this.resourceName != null && this.columnBuilders.length > 0)
        {
            Column[] builtColumns = new Column[this.columnBuilders.length + 1];
            for (int i = 0; i < this.columnBuilders.length; ++i)
            {
                builtColumns[i + 1] = this.columnBuilders[i].build();
            }
            int rowCount = builtColumns.length > 1 ? builtColumns[1].size() : 0;
            builtColumns[0] = Columns.newString(this.resourceName, this.sourceName, rowCount);
            return Tables.newTable(name, builtColumns);
        }
        return Tables.newTable(name, this.columnBuilders);
    }

    void setSourceName(String sourceName)
    {
        this.sourceName = sourceName;
    }

    public static RowConsumerCreateTable create(TableName tableName, ColumnName resourceName)
    {
        return create(tableName, resourceName, Collections.emptyMap());
    }

    public static RowConsumerCreateTable create(TableName tableName, ColumnName resourceName,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        return new RowConsumerCreateTable(tableName, resourceName, explicitColumnTypes);
    }

    private static Column.Type effectiveColumnType(ColumnName columnName,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        Column.Type columnType = explicitColumnTypes.get(columnName);
        if (columnType == null)
        {
            return Column.Type.of(String.class);
        }
        return columnType;
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
