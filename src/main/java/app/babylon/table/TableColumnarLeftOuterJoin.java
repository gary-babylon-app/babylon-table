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

import app.babylon.lang.ArgumentCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

class TableColumnarLeftOuterJoin extends TableColumnarCommon
{
    private final TableName name;
    private final TableColumnar left;
    private final TableColumnar right;
    private final ViewIndex rowIndex;
    private final ColumnName[] rightColumnsToAdd;
    private final Set<ColumnName> rightColumnNames;
    private final ConcurrentMap<ColumnName, Column> rightColumnViews;

    TableColumnarLeftOuterJoin(TableName name, TableDescription description, TableColumnar left, TableColumnar right,
            ViewIndex rowIndex, ColumnName... rightColumnsToAdd)
    {
        super(description);
        this.name = ArgumentCheck.nonNull(name);
        this.left = ArgumentCheck.nonNull(left);
        this.right = ArgumentCheck.nonNull(right);
        this.rowIndex = ArgumentCheck.nonNull(rowIndex);
        this.rightColumnsToAdd = ArgumentCheck.nonNull(rightColumnsToAdd);
        this.rightColumnNames = Set.of(rightColumnsToAdd);
        this.rightColumnViews = new ConcurrentHashMap<>();
    }

    @Override
    public TableName getName()
    {
        return this.name;
    }

    @Override
    public int getColumnCount()
    {
        return this.left.getColumnCount() + this.rightColumnsToAdd.length;
    }

    @Override
    public int getRowCount()
    {
        return this.left.getRowCount();
    }

    @Override
    public boolean contains(ColumnName x)
    {
        return this.left.contains(x) || this.rightColumnNames.contains(x);
    }

    @Override
    public Collection<ColumnName> getColumnNames(Collection<ColumnName> x)
    {
        x = this.left.getColumnNames(x);
        for (ColumnName rightColumn : this.rightColumnsToAdd)
        {
            x.add(rightColumn);
        }
        return x;
    }

    @Override
    public Column get(ColumnName x)
    {
        if (this.left.contains(x))
        {
            return this.left.get(x);
        }
        if (!this.rightColumnNames.contains(x))
        {
            return null;
        }
        return this.rightColumnViews.computeIfAbsent(x, colName -> {
            Column originalColumn = this.right.get(colName);
            return originalColumn.view(this.rowIndex);
        });
    }

    @Override
    public Column[] getColumns()
    {
        Column[] columns = new Column[getColumnCount()];
        ColumnName[] names = getColumnNames();
        for (int i = 0; i < names.length; ++i)
        {
            columns[i] = get(names[i]);
        }
        return columns;
    }

    @Override
    public TableColumnar removeColumns(ColumnName... x)
    {
        if (!Is.empty(x))
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

            TableColumnar reducedLeft = this.left
                    .removeColumns(columnsToRemove.toArray(new ColumnName[columnsToRemove.size()]));
            List<ColumnName> remainingRightColumns = new ArrayList<>();
            for (ColumnName rightColumn : this.rightColumnsToAdd)
            {
                if (!columnsToRemove.contains(rightColumn))
                {
                    remainingRightColumns.add(rightColumn);
                }
            }

            return new TableColumnarLeftOuterJoin(getName(), getDescription(), reducedLeft, this.right, this.rowIndex,
                    remainingRightColumns.toArray(new ColumnName[remainingRightColumns.size()]));
        }
        return this;
    }
}
