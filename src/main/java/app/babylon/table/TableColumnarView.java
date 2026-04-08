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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class TableColumnarView extends TableColumnarCommon
{
    private final TableName name;
    private final TableColumnar original;
    private final ViewIndex rowIndex;
    private final ColumnName[] activeColumnNames;
    private final ConcurrentMap<ColumnName, Column> columnViews;

    public TableColumnarView(TableName name, TableColumnar original, ViewIndex rowIndex)
    {
        this(name, null, original, rowIndex, original.getColumnNames());
    }

    public TableColumnarView(TableName name, TableDescription tableDescription, TableColumnar original,
            ViewIndex rowIndex)
    {
        this(name, tableDescription, original, rowIndex, original.getColumnNames());
    }

    public TableColumnarView(TableName name, TableDescription tableDescription, TableColumnar original,
            ViewIndex rowIndex, ColumnName[] activeColumnNames)
    {
        super(tableDescription);
        this.name = Objects.requireNonNull(name);
        this.original = Objects.requireNonNull(original);
        this.rowIndex = Objects.requireNonNull(rowIndex);
        this.activeColumnNames = Arrays.copyOf(Objects.requireNonNull(activeColumnNames), activeColumnNames.length);
        this.columnViews = new ConcurrentHashMap<>();
    }

    @Override
    public TableName getName()
    {
        return name;
    }

    @Override
    public int getColumnCount()
    {
        return this.activeColumnNames.length;
    }

    @Override
    public int getRowCount()
    {
        return this.rowIndex.size();
    }

    @Override
    public boolean contains(ColumnName x)
    {
        if (x == null)
        {
            return false;
        }
        for (ColumnName activeColumn : this.activeColumnNames)
        {
            if (activeColumn.equals(x))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<ColumnName> getColumnNames(Collection<ColumnName> x)
    {
        if (x == null)
        {
            x = new java.util.ArrayList<>();
        }
        for (ColumnName activeColumnName : this.activeColumnNames)
        {
            x.add(activeColumnName);
        }
        return x;
    }

    @Override
    public ColumnName[] getColumnNames()
    {
        return Arrays.copyOf(this.activeColumnNames, this.activeColumnNames.length);
    }

    @Override
    public Column get(ColumnName x)
    {
        if (!contains(x))
        {
            return null;
        }
        return this.columnViews.computeIfAbsent(x, colName -> {
            Column originalColumn = this.original.get(colName);
            if (originalColumn == null)
            {
                throw new IllegalStateException("Original table missing column " + colName + ".");
            }
            return originalColumn.view(this.rowIndex);
        });
    }

    @Override
    public Column[] getColumns()
    {
        Column[] c = new Column[getColumnCount()];
        ColumnName[] colNames = this.activeColumnNames;
        for (int i = 0; i < c.length; ++i)
        {
            c[i] = get(colNames[i]);
        }
        return c;
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
            ColumnName[] remainingColumnNames = new ColumnName[this.activeColumnNames.length - columnsToRemove.size()];
            int j = 0;
            for (ColumnName activeColumnName : this.activeColumnNames)
            {
                if (!columnsToRemove.contains(activeColumnName))
                {
                    remainingColumnNames[j] = activeColumnName;
                    ++j;
                }
            }
            return new TableColumnarView(getName(), getDescription(), this.original, this.rowIndex,
                    remainingColumnNames);
        }
        return this;
    }
}
