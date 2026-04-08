/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.grouping;

import app.babylon.table.TableColumnar;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GroupBy
{
    private final ColumnName[] groupBys;
    private final Column.Type[] groupByTypes;

    private final Map<GroupKey, TableColumnar> groupedTables;

    public GroupBy(Column.Type[] groupByTypes, ColumnName[] groupBys, Map<GroupKey, TableColumnar> groupedTables)
    {
        this.groupByTypes = Arrays.copyOf(groupByTypes, groupByTypes.length);
        this.groupBys = Arrays.copyOf(groupBys, groupBys.length);
        this.groupedTables = new HashMap<>(groupedTables);

    }

    public Map<GroupKey, TableColumnar> getGroupedTables(Map<GroupKey, TableColumnar> x)
    {
        if (x == null)
        {
            x = new HashMap<>();
        }
        x.putAll(this.groupedTables);
        return x;
    }
}
