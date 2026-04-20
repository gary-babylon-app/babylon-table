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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.table.transform.DateFormat;
import app.babylon.table.transform.TransformToLocalDate;

public final class RowConsumerCreateTable implements RowConsumer
{
    private final TableName tableName;
    private final TableDescription tableDescription;
    private final Map<ColumnName, Column.Type> sourceColumnTypes;
    private final Map<ColumnName, Column.Type> planColumnTypes;
    private final DateFormat localDateFormat;
    private Column.Type[] columnTypes;
    private Column.Builder[] columnBuilders;

    RowConsumerCreateTable(TableName tableName, TableDescription tableDescription,
            Map<ColumnName, Column.Type> sourceColumnTypes, Map<ColumnName, Column.Type> planColumnTypes,
            DateFormat localDateFormat)
    {
        this.tableName = tableName;
        this.tableDescription = tableDescription;
        this.sourceColumnTypes = new LinkedHashMap<>(sourceColumnTypes);
        this.planColumnTypes = new LinkedHashMap<>(planColumnTypes);
        this.localDateFormat = localDateFormat;
        this.columnTypes = null;
        this.columnBuilders = null;
    }

    @Override
    public void start(ColumnName[] columnNames)
    {
        ArgumentCheck.nonEmpty(columnNames);
        this.columnTypes = new Column.Type[columnNames.length];
        this.columnBuilders = new Column.Builder[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i)
        {
            Column.Type columnType = builderColumnType(columnNames[i], this.sourceColumnTypes, this.planColumnTypes);
            this.columnTypes[i] = columnType;
            if (columnType.isPrimitive())
            {
                this.columnBuilders[i] = Columns.newBuilder(columnNames[i], columnType);
            }
            else
            {
                // we can optimise a bit here for huge tables where String creation causes
                // pressure or very fast parsers
                // but use String for now in the builder.
                this.columnBuilders[i] = Columns.newColumn(columnNames[i]);
            }
        }
    }

    @Override
    public void accept(Row rowValues)
    {
        int columnCount = this.columnBuilders.length;
        if (rowValues.size() != columnCount)
        {
            return;
        }
        if (rowValues.isEmpty())
        {
            return;
        }
        for (int i = 0; i < columnCount; ++i)
        {
            addValue(this.columnBuilders[i], rowValues, rowValues.start(i), rowValues.length(i));
        }
    }

    public TableColumnar build()
    {
        Column[] columns = new Column[this.columnBuilders.length];
        List<ColumnName> localDateColumns = new ArrayList<>();
        for (int i = 0; i < columns.length; ++i)
        {
            Column.Builder colBuilder = columnBuilders[i];
            ColumnName colName = colBuilder.getName();
            Column.Type type = this.planColumnTypes.get(colName);
            if (type == null)
            {
                type = this.sourceColumnTypes.get(colName);
            }
            if (colBuilder instanceof ColumnObject.Builder<?> objectBuilder)
            {
                if (type == null)
                {
                    columns[i] = objectBuilder.build();
                }
                else if (LocalDate.class.equals(type.getValueClass()))
                {
                    localDateColumns.add(colName);
                    columns[i] = objectBuilder.build();
                }
                else
                {
                    // Very convenient and fast transformation from string
                    columns[i] = objectBuilder.build(type);
                }
            }
            else
            {
                columns[i] = colBuilder.build();
            }
        }
        TableColumnar table = Tables.newTable(this.tableName, this.tableDescription, columns);
        if (!localDateColumns.isEmpty())
        {
            return table
                    .apply(new TransformToLocalDate(this.localDateFormat, localDateColumns.toArray(new ColumnName[0])));
        }
        return table;
    }

    public static RowConsumerCreateTable create(TableName tableName)
    {
        return create(tableName, null, Collections.emptyMap(), Collections.emptyMap(), null);
    }

    public static RowConsumerCreateTable create(TableName tableName, TableDescription tableDescription,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        return create(tableName, tableDescription, explicitColumnTypes, explicitColumnTypes, null);
    }

    public static RowConsumerCreateTable create(TableName tableName, TableDescription tableDescription,
            Map<ColumnName, Column.Type> explicitColumnTypes, DateFormat localDateFormat)
    {
        return create(tableName, tableDescription, explicitColumnTypes, explicitColumnTypes, localDateFormat);
    }

    public static RowConsumerCreateTable create(TableName tableName, TableDescription tableDescription,
            Map<ColumnName, Column.Type> sourceColumnTypes, Map<ColumnName, Column.Type> planColumnTypes,
            DateFormat localDateFormat)
    {
        return new RowConsumerCreateTable(tableName, tableDescription, sourceColumnTypes, planColumnTypes,
                localDateFormat);
    }

    private static Column.Type effectiveColumnType(ColumnName columnName,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        Column.Type columnType = explicitColumnTypes.get(columnName);
        if (columnType == null || columnType.getValueClass().equals(LocalDate.class))
        {
            return ColumnTypes.STRING;
        }
        return columnType;
    }

    private static Column.Type builderColumnType(ColumnName columnName, Map<ColumnName, Column.Type> sourceColumnTypes,
            Map<ColumnName, Column.Type> planColumnTypes)
    {
        Column.Type sourceType = sourceColumnTypes.get(columnName);
        Column.Type planType = planColumnTypes.get(columnName);
        if (planType != null && planType.isPrimitive() && (sourceType == null || !sourceType.isPrimitive()))
        {
            return planType;
        }
        return effectiveColumnType(columnName, sourceColumnTypes);
    }

    private static void addValue(Column.Builder builder, CharSequence chars, int start, int length)
    {
        if (length <= 0)
        {
            builder.add((CharSequence) null, 0, 0);
            return;
        }
        builder.add(chars, start, length);
    }
}
