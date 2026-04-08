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

public class TableSort
{
    public enum SortOrder
    {
        Natural, Reverse
    };

    public static TableColumnar sort(TableColumnar table, ColumnName... x)
    {
        return sort(table, SortOrder.Natural, x);
    }

    public static TableColumnar sort(TableColumnar table, SortOrder sortOrder, ColumnName... x)
    {
        if (x == null || table.getRowCount() == 0)
        {
            return table;
        }

        int[] sortArray = new int[table.getRowCount()];
        for (int i = 0; i < sortArray.length; ++i)
        {
            sortArray[i] = i;
        }

        ColumnsComparator columnsComparator = new ColumnsComparator(table, x);
        ComparatorInt comparator = columnsComparator::compareRows;
        if (sortOrder == SortOrder.Reverse)
        {
            comparator = (a, b) -> columnsComparator.compareRows(b, a);
        }
        SortInt.stableSort(sortArray, comparator);
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.addAll(sortArray);
        return Tables.newTableView(table.getName(), table.getDescription(), table, rowIndexBuilder.build());
    }
}
