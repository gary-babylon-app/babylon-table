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

import java.util.Objects;

import java.util.Comparator;

public class ColumnsComparator implements Comparator<Integer>
{
    private Column[] columns;

    public ColumnsComparator(TableColumnar table, ColumnName... x)
    {
        this.columns = Objects.requireNonNull(table.getColumns(x));
    }

    public int compareRows(int row1, int row2)
    {
        int result = 0;
        for (int i = 0; i < this.columns.length; ++i)
        {
            result = this.columns[i].compare(row1, row2);
            if (result != 0)
            {
                return result;
            }
        }
        return result;
    }

    @Override
    public int compare(Integer o1, Integer o2)
    {
        return compareRows(o1.intValue(), o2.intValue());
    }

}
