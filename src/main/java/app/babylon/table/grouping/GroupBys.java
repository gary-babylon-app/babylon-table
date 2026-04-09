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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.ViewIndex;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;

public final class GroupBys
{
    private GroupBys()
    {
    }

    public static GroupBy groupBy(TableColumnar table, ColumnName... x)
    {
        ArgumentCheck.nonEmpty(x);
        GroupingSpec spec = categoricalGroupingSpec(table, x);
        int rowCount = table.getRowCount();
        Map<GroupKey, ViewIndex.Builder> groupedRowIndexes;
        if (spec.columns.length == 1)
        {
            groupedRowIndexes = groupBySingleCategorical(spec.columns[0], rowCount);
        } else
        {
            groupedRowIndexes = groupByMultipleCategorical(spec.columns, rowCount);
        }
        return buildGroupByFromRowIndexes(table, spec.types, spec.names, groupedRowIndexes);
    }

    private static GroupingSpec categoricalGroupingSpec(TableColumnar table, ColumnName[] requestedGroupColumns)
    {
        ColumnCategorical<?>[] columns = new ColumnCategorical<?>[requestedGroupColumns.length];
        Column.Type[] types = new Column.Type[requestedGroupColumns.length];
        ColumnName[] names = new ColumnName[requestedGroupColumns.length];
        for (int i = 0; i < requestedGroupColumns.length; ++i)
        {
            ColumnName requested = requestedGroupColumns[i];
            Column column = table.get(requested);
            if (column == null)
            {
                throw new IllegalArgumentException("GroupBy column not found: " + requested);
            }
            if (!(column instanceof ColumnCategorical<?> categorical))
            {
                throw new IllegalArgumentException("GroupBy requires categorical columns only: " + requested + " is "
                        + column.getClass().getSimpleName());
            }
            columns[i] = categorical;
            types[i] = column.getType();
            names[i] = column.getName();
        }
        return new GroupingSpec(columns, types, names);
    }

    private static Map<GroupKey, ViewIndex.Builder> groupBySingleCategorical(ColumnCategorical<?> column, int rowCount)
    {
        Map<Integer, ViewIndex.Builder> rowIndexesByCode = new LinkedHashMap<>();
        Map<Integer, GroupKey> groupKeysByCode = new LinkedHashMap<>();
        for (int i = 0; i < rowCount; ++i)
        {
            int code = bucketIndex(column, i);
            ViewIndex.Builder rows = rowIndexesByCode.get(code);
            if (rows == null)
            {
                rows = ViewIndex.builder();
                rowIndexesByCode.put(code, rows);
                groupKeysByCode.put(code, createGroupKey(new ColumnCategorical<?>[]
                {column}, i));
            }
            rows.add(i);
        }
        Map<GroupKey, ViewIndex.Builder> grouped = new LinkedHashMap<>();
        for (Entry<Integer, ViewIndex.Builder> e : rowIndexesByCode.entrySet())
        {
            grouped.put(groupKeysByCode.get(e.getKey()), e.getValue());
        }
        return grouped;
    }

    private static Map<GroupKey, ViewIndex.Builder> groupByMultipleCategorical(ColumnCategorical<?>[] columns,
            int rowCount)
    {
        Map<GroupCategoryCode, ViewIndex.Builder> rowsByCompositeCode = new LinkedHashMap<>();
        Map<GroupCategoryCode, GroupKey> keysByCompositeCode = new LinkedHashMap<>();
        for (int i = 0; i < rowCount; ++i)
        {
            GroupCategoryCode compositeCode = groupKeyCode(columns, i);
            ViewIndex.Builder rows = rowsByCompositeCode.get(compositeCode);
            if (rows == null)
            {
                rows = ViewIndex.builder();
                rowsByCompositeCode.put(compositeCode, rows);
                keysByCompositeCode.put(compositeCode, createGroupKey(columns, i));
            }
            rows.add(i);
        }

        Map<GroupKey, ViewIndex.Builder> grouped = new LinkedHashMap<>();
        for (Entry<GroupCategoryCode, ViewIndex.Builder> e : rowsByCompositeCode.entrySet())
        {
            grouped.put(keysByCompositeCode.get(e.getKey()), e.getValue());
        }
        return grouped;
    }

    private static int bucketIndex(ColumnCategorical<?> column, int row)
    {
        if (!column.isSet(row))
        {
            return 0;
        }
        return column.getCategoryCode(row);
    }

    private static GroupKey createGroupKey(ColumnCategorical<?>[] columns, int row)
    {
        Object[] elements = new Object[columns.length];
        for (int i = 0; i < columns.length; ++i)
        {
            ColumnCategorical<?> column = columns[i];
            elements[i] = column.get(row);
        }
        return GroupKey.of(elements);
    }

    private static GroupCategoryCode groupKeyCode(ColumnCategorical<?>[] columns, int row)
    {
        int n = columns.length;
        if (n == 1)
        {
            return GroupCategoryCode.of(bucketIndex(columns[0], row));
        }
        if (n == 2)
        {
            return GroupCategoryCode.of(bucketIndex(columns[0], row), bucketIndex(columns[1], row));
        }
        if (n == 3)
        {
            return GroupCategoryCode.of(bucketIndex(columns[0], row), bucketIndex(columns[1], row),
                    bucketIndex(columns[2], row));
        }
        int[] codes = new int[n];
        for (int i = 0; i < n; ++i)
        {
            codes[i] = bucketIndex(columns[i], row);
        }
        return GroupCategoryCode.of(codes);
    }

    private static GroupBy buildGroupByFromRowIndexes(TableColumnar table, Column.Type[] groupTypes,
            ColumnName[] groupNames, Map<GroupKey, ViewIndex.Builder> rowIndexesByGroup)
    {
        Map<GroupKey, TableColumnar> groupTables = new HashMap<>();
        for (Entry<GroupKey, ViewIndex.Builder> e : rowIndexesByGroup.entrySet())
        {
            ViewIndex rowIndex = e.getValue().build();
            TableDescription desc = new TableDescription("Grouping");
            TableName name = TableName.of(e.getKey().toString());
            TableColumnar groupTable = Tables.newTableView(name, desc, table, rowIndex);
            groupTables.put(e.getKey(), groupTable);
        }
        return new GroupBy(groupTables);
    }

    private static final class GroupingSpec
    {
        private final ColumnCategorical<?>[] columns;
        private final Column.Type[] types;
        private final ColumnName[] names;

        private GroupingSpec(ColumnCategorical<?>[] columns, Column.Type[] types, ColumnName[] names)
        {
            this.columns = columns;
            this.types = types;
            this.names = names;
        }
    }
}
