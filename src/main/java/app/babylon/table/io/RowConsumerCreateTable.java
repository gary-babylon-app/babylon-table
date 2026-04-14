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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBuilder;
import app.babylon.table.column.CharSliceBuilder;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public final class RowConsumerCreateTable implements RowConsumer
{
    private final TableName tableName;
    private final TableDescription tableDescription;
    private final ColumnName resourceName;
    private final String sourceName;
    private final Map<ColumnName, Column.Type> explicitColumnTypes;
    private Column.Type[] columnTypes;
    private CharSliceBuilder[] columnBuilders;

    RowConsumerCreateTable(TableName tableName, TableDescription tableDescription, ColumnName resourceName,
            String sourceName, Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        this.tableName = tableName;
        this.tableDescription = tableDescription;
        this.resourceName = resourceName;
        this.sourceName = sourceName;
        this.explicitColumnTypes = new LinkedHashMap<>(explicitColumnTypes);
        this.columnTypes = null;
        this.columnBuilders = null;
    }

    @Override
    public void start(ColumnName[] columnNames)
    {
        ArgumentCheck.nonEmpty(columnNames);
        this.columnTypes = new Column.Type[columnNames.length];
        this.columnBuilders = new CharSliceBuilder[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i)
        {
            Column.Type columnType = effectiveColumnType(columnNames[i], this.explicitColumnTypes);
            this.columnTypes[i] = columnType;
            this.columnBuilders[i] = Columns.newCharSliceBuilder(columnNames[i], columnType);
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
        if (rowValues.isEmpty())
        {
            return;
        }
        char[] chars = rowValues.chars();
        for (int i = 0; i < columnCount; ++i)
        {
            addValue(this.columnBuilders[i], chars, rowValues.start(i), rowValues.length(i));
        }
    }

    public TableColumnar build()
    {
        TableName name = this.tableName;
        TableDescription description = this.tableDescription;
        String sourceName = this.sourceName;
        if (name == null)
        {
            name = TableName.of(extractLastPart(sourceName));
        }
        if (this.resourceName != null && this.columnBuilders.length > 0)
        {
            Column[] builtColumns = new Column[this.columnBuilders.length + 1];
            for (int i = 0; i < this.columnBuilders.length; ++i)
            {
                builtColumns[i + 1] = this.columnBuilders[i].build();
            }
            int rowCount = builtColumns.length > 1 ? builtColumns[1].size() : 0;
            builtColumns[0] = Columns.newString(this.resourceName, sourceName, rowCount);
            return Tables.newTable(name, description, builtColumns);
        }
        return Tables.newTable(name, description, this.columnBuilders);
    }

    public static RowConsumerCreateTable create(TableName tableName, ColumnName resourceName)
    {
        return create(tableName, null, resourceName, null, Collections.emptyMap());
    }

    public static RowConsumerCreateTable create(TableName tableName, TableDescription tableDescription,
            ColumnName resourceName, String sourceName, Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        return new RowConsumerCreateTable(tableName, tableDescription, resourceName, sourceName, explicitColumnTypes);
    }

    private static Column.Type effectiveColumnType(ColumnName columnName,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        Column.Type columnType = explicitColumnTypes.get(columnName);
        if (columnType == null)
        {
            return ColumnTypes.STRING;
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

    private static void addValue(CharSliceBuilder builder, char[] chars, int start, int length)
    {
        if (length <= 0)
        {
            builder.add(null, 0, 0);
            return;
        }
        builder.add(chars, start, length);
    }
}
