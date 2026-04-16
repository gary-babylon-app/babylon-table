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
import app.babylon.table.column.ColumnObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TableColumnarViewTest
{
    private static TableColumnar sampleTable()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName B_2 = ColumnName.of("B");
        ColumnObject.Builder<String> a = ColumnObject.builder(A_2, app.babylon.table.column.ColumnTypes.STRING);
        a.add("a0");
        a.add("a1");
        a.add("a2");

        ColumnObject.Builder<String> b = ColumnObject.builder(B_2, app.babylon.table.column.ColumnTypes.STRING);
        b.add("b0");
        b.add("b1");
        b.add("b2");

        return Tables.newTable(TableName.of("t"), new TableDescription(""), a.build(), b.build());
    }

    @Test
    public void newTableViewShouldProjectRowsInSpecifiedOrder()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName B_2 = ColumnName.of("B");
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(0);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        assertEquals(2, view.getRowCount());
        assertEquals("a2", view.getString(A_2).get(0));
        assertEquals("b2", view.getString(B_2).get(0));
        assertEquals("a0", view.getString(A_2).get(1));
        assertEquals("b0", view.getString(B_2).get(1));
    }

    @Test
    public void newTableViewShouldDefensivelyCopyViewIndex()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName B_2 = ColumnName.of("B");
        TableColumnar table = sampleTable();
        int[] indexes = new int[]
        {1};
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.addAll(indexes);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        indexes[0] = 0;

        assertEquals("a1", view.getString(A_2).get(0));
        assertEquals("b1", view.getString(B_2).get(0));
    }

    @Test
    public void tableViewShouldRemoveColumnsWithoutAffectingOriginal()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName B_2 = ColumnName.of("B");
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(0).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        TableColumnar reduced = view.removeColumns(A_2);

        assertEquals(2, view.getColumnCount());
        assertEquals(1, reduced.getColumnCount());
        assertEquals(B_2, reduced.getColumnNames()[0]);
        assertEquals("b0", reduced.getString(B_2).get(0));
    }

    @Test
    public void pruneShouldRemoveColumnsThatAreNoneSetOnATableView()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName EMPTY = ColumnName.of("EMPTY");
        ColumnObject.Builder<String> a = ColumnObject.builder(A_2, app.babylon.table.column.ColumnTypes.STRING);
        a.add("a0");
        a.add("a1");

        ColumnObject.Builder<String> empty = ColumnObject.builder(EMPTY, app.babylon.table.column.ColumnTypes.STRING);
        empty.addNull();
        empty.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), a.build(), empty.build());

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(1).add(0);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        TableColumnar pruned = view.prune();

        assertEquals(2, view.getColumnCount());
        assertEquals(1, pruned.getColumnCount());
        assertEquals(A_2, pruned.getColumnNames()[0]);
        assertEquals("a1", pruned.getString(A_2).get(0));
        assertEquals("a0", pruned.getString(A_2).get(1));
    }

    @Test
    public void newTableViewWithRowIndexShouldProjectRows()
    {
        final ColumnName A_2 = ColumnName.of("A");
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        assertEquals(2, view.getRowCount());
        assertEquals("a2", view.getString(A_2).get(0));
        assertEquals("a1", view.getString(A_2).get(1));
    }

    @Test
    public void getByNameShouldCacheSameColumnInstance()
    {
        final ColumnName A_2 = ColumnName.of("A");
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        Column first = view.get(A_2);
        Column second = view.get(A_2);
        assertSame(first, second);
    }

    @Test
    public void getByIndexShouldMatchGetByNameAndOriginalOrder()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName B_2 = ColumnName.of("B");
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        assertEquals(table.get(A_2).getName(), view.get(A_2).getName());
        assertEquals(table.get(B_2).getName(), view.get(B_2).getName());
    }

    @Test
    public void replaceShouldThrowForMissingColumnName()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName C_2 = ColumnName.of("C");
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        ColumnObject.Builder<String> replaceA = ColumnObject.builder(A_2, app.babylon.table.column.ColumnTypes.STRING);
        replaceA.add("ra0").add("ra1");

        ColumnObject.Builder<String> missingColumn = ColumnObject.builder(C_2,
                app.babylon.table.column.ColumnTypes.STRING);
        missingColumn.add("c0").add("c1");

        assertThrows(RuntimeException.class, () -> view.replace(replaceA.build(), missingColumn.build(), null));
    }
}
