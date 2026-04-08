/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import java.util.Objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class TableColumnarMap extends TableColumnarCommon
{
    private final TableName name;
    private final Map<ColumnName, Column> columnsByName;
    private final ColumnName[] columnOrder;
    private final int minColumnLength;

    TableColumnarMap(TableName tableName, Column... columns)
    {
        this(tableName, null, columns);
    }

    TableColumnarMap(TableName tableName, TableDescription description, Column... columns)
    {
        super(description);
        this.name = Objects.requireNonNull(tableName);
        ArgumentChecks.nonEmpty(columns);

        this.columnOrder = new ColumnName[columns.length];

        int mcl = 0;

        Map<ColumnName, Column> byName = new LinkedHashMap<>();
        int index = 0;
        for (Column column : columns)
        {
            ColumnName columnName = column.getName();

            if (index > 0)
            {
                mcl = Math.min(mcl, column.size());
            } else
            {
                mcl = column.size();
            }

            Column dupicateColumn = byName.get(columnName);
            if (dupicateColumn != null)
            {
                throw new RuntimeException("Duplicate detected, column " + dupicateColumn.getName()
                        + " already present, fail to add " + columnName);
            }
            this.columnOrder[index++] = columnName;
            byName.put(columnName, column);
        }

        this.columnsByName = Map.copyOf(byName);
        this.minColumnLength = mcl;
    }

    @Override
    public TableName getName()
    {
        return name;
    }

    @Override
    public ColumnName[] getColumnNames()
    {
        return Arrays.copyOf(this.columnOrder, this.columnOrder.length);
    }

    @Override
    public Collection<ColumnName> getColumnNames(Collection<ColumnName> x)
    {
        if (x == null)
        {
            x = new ArrayList<>();
        }

        for (int i = 0; i < this.columnOrder.length; ++i)
        {
            x.add(this.columnOrder[i]);
        }
        return x;
    }

    @Override
    public int getColumnCount()
    {
        return this.columnOrder.length;
    }

    @Override
    public int getRowCount()
    {
        return minColumnLength;
    }

    @Override
    public Column get(ColumnName x)
    {
        return this.columnsByName.get(x);
    }

    @Override
    public boolean contains(ColumnName x)
    {
        return this.columnsByName.containsKey(x);
    }

    @Override
    public Column[] getColumns()
    {
        Column[] columns = new Column[this.columnOrder.length];
        for (int i = 0; i < this.columnOrder.length; ++i)
        {
            columns[i] = this.columnsByName.get(this.columnOrder[i]);
        }
        return columns;
    }

    @Override
    public TableColumnar removeColumns(ColumnName... x)
    {
        if (!Empties.isEmpty(x))
        {
            Set<ColumnName> columnsToRemove = new HashSet<>();
            for (ColumnName columnName : x)
            {
                if (contains(columnName))
                {
                    columnsToRemove.add(columnName);
                }
            }
            if (columnsToRemove.isEmpty())
            {
                return this;
            }

            Column[] allColumns = new Column[this.columnOrder.length - columnsToRemove.size()];
            int j = 0;
            for (int i = 0; i < this.columnOrder.length; ++i)
            {
                ColumnName columnName = this.columnOrder[i];
                if (!columnsToRemove.contains(columnName))
                {
                    allColumns[j] = this.columnsByName.get(columnName);
                    ++j;
                }
            }
            return new TableColumnarMap(getName(), getDescription(), allColumns);
        } else
        {
            return this;
        }
    }
}
