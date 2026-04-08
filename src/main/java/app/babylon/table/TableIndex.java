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

import java.util.HashMap;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.grouping.GroupKey;

public class TableIndex
{
    private final TableColumnar table;
    private final Map<GroupKey, Integer> index;

    public TableIndex(TableColumnar table, ColumnName... columnNames)
    {
        this.table = app.babylon.lang.ArgumentCheck.nonNull(table);
        ArgumentCheck.nonEmpty(columnNames);
        Column[] columns = table.getColumns(columnNames);
        this.index = new HashMap<>();

        for (int i = 0; i < table.getRowCount(); ++i)
        {
            Object[] key = new Object[columns.length];
            for (int j = 0; j < columns.length; ++j)
            {
                if (columns[j] instanceof ColumnObject co)
                {
                    key[j] = co.get(i);
                } else
                {
                    throw new RuntimeException(
                            "column " + columns[j].getName() + " not columnobject, cannot form an index");
                }
            }
            GroupKey groupKey = GroupKey.of(key);
            index.put(groupKey, Integer.valueOf(i));
        }
    }

    public TableColumnar getTable()
    {
        return this.table;
    }
    public Integer index(Object... x)
    {
        GroupKey key = GroupKey.of(x);
        return index.get(key);
    }

}
