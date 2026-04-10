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
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBuilder;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public final class RowConsumerCreateTable implements RowConsumer<TableColumnar>
{
    private static final byte KIND_STRING = 0;
    private static final byte KIND_DOUBLE = 1;

    private final TableName tableName;
    private final ColumnName resourceName;
    private final Map<ColumnName, Column.Type> explicitColumnTypes;
    private byte[] columnKinds;
    private ColumnBuilder[] columnBuilders;
    private String[] strippedValues;
    private String sourceName;

    RowConsumerCreateTable(TableName tableName, ColumnName resourceName,
            Map<ColumnName, Column.Type> explicitColumnTypes)
    {
        this.tableName = tableName;
        this.resourceName = resourceName;
        this.explicitColumnTypes = explicitColumnTypes;
        this.columnKinds = null;
        this.columnBuilders = null;
        this.strippedValues = null;
        this.sourceName = null;
    }

    @Override
    public void start(ColumnName[] columnNames)
    {
        this.columnKinds = initialiseColumnKinds(columnNames, this.explicitColumnTypes);
        this.columnBuilders = initialiseColumnBuilders(columnNames, this.explicitColumnTypes);
        this.strippedValues = new String[this.columnBuilders.length];
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

    private static byte[] initialiseColumnKinds(ColumnName[] columnNames,
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

    private static ColumnBuilder[] initialiseColumnBuilders(ColumnName[] columnNames,
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
