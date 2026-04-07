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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TableViewTest
{
    private static TableColumnar sampleTable()
    {
        ColumnObject.Builder<String> a = ColumnObject.builder(ColumnName.of("A"), String.class);
        a.add("a0");
        a.add("a1");
        a.add("a2");

        ColumnObject.Builder<String> b = ColumnObject.builder(ColumnName.of("B"), String.class);
        b.add("b0");
        b.add("b1");
        b.add("b2");

        return Tables.newTable(TableName.of("t"), new TableDescription(""), a.build(), b.build());
    }

    @Test
    public void newTableViewShouldProjectRowsInSpecifiedOrder()
    {
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(0);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        assertEquals(2, view.getRowCount());
        assertEquals("a2", view.getString(ColumnName.of("A")).get(0));
        assertEquals("b2", view.getString(ColumnName.of("B")).get(0));
        assertEquals("a0", view.getString(ColumnName.of("A")).get(1));
        assertEquals("b0", view.getString(ColumnName.of("B")).get(1));
    }

    @Test
    public void newTableViewShouldDefensivelyCopyViewIndex()
    {
        TableColumnar table = sampleTable();
        int[] indexes = new int[] { 1 };
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.addAll(indexes);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        indexes[0] = 0;

        assertEquals("a1", view.getString(ColumnName.of("A")).get(0));
        assertEquals("b1", view.getString(ColumnName.of("B")).get(0));
    }

    @Test
    public void tableViewShouldRemoveColumnsWithoutAffectingOriginal()
    {
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(0).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        TableColumnar reduced = view.removeColumns(ColumnName.of("A"));

        assertEquals(2, view.getColumnCount());
        assertEquals(1, reduced.getColumnCount());
        assertEquals(ColumnName.of("B"), reduced.getColumnNames()[0]);
        assertEquals("b0", reduced.getString(ColumnName.of("B")).get(0));
    }

    @Test
    public void pruneShouldRemoveColumnsThatAreNoneSetOnATableView()
    {
        ColumnObject.Builder<String> a = ColumnObject.builder(ColumnName.of("A"), String.class);
        a.add("a0");
        a.add("a1");

        ColumnObject.Builder<String> empty = ColumnObject.builder(ColumnName.of("Empty"), String.class);
        empty.addNull();
        empty.addNull();

        TableColumnar table = Tables.newTable(
                TableName.of("t"),
                new TableDescription(""),
                a.build(),
                empty.build());

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(1).add(0);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        TableColumnar pruned = view.prune();

        assertEquals(2, view.getColumnCount());
        assertEquals(1, pruned.getColumnCount());
        assertEquals(ColumnName.of("A"), pruned.getColumnNames()[0]);
        assertEquals("a1", pruned.getString(ColumnName.of("A")).get(0));
        assertEquals("a0", pruned.getString(ColumnName.of("A")).get(1));
    }


    @Test
    public void newTableViewWithRowIndexShouldProjectRows()
    {
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        assertEquals(2, view.getRowCount());
        assertEquals("a2", view.getString(ColumnName.of("A")).get(0));
        assertEquals("a1", view.getString(ColumnName.of("A")).get(1));
    }

    @Test
    public void getByNameShouldCacheSameColumnInstance()
    {
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        Column first = view.get(ColumnName.of("A"));
        Column second = view.get(ColumnName.of("A"));
        assertSame(first, second);
    }

    @Test
    public void getByIndexShouldMatchGetByNameAndOriginalOrder()
    {
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        assertEquals(table.get(ColumnName.of("A")).getName(), view.get(ColumnName.of("A")).getName());
        assertEquals(table.get(ColumnName.of("B")).getName(), view.get(ColumnName.of("B")).getName());
    }

    @Test
    public void replaceShouldThrowForMissingColumnName()
    {
        TableColumnar table = sampleTable();
        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2).add(1);
        TableColumnar view = Tables.newTableView(TableName.of("v"), table, rowIndexBuilder.build());

        ColumnObject.Builder<String> replaceA = ColumnObject.builder(ColumnName.of("A"), String.class);
        replaceA.add("ra0").add("ra1");

        ColumnObject.Builder<String> missingColumn = ColumnObject.builder(ColumnName.of("C"), String.class);
        missingColumn.add("c0").add("c1");

        assertThrows(RuntimeException.class, () -> view.replace(replaceA.build(), missingColumn.build(), null));
    }
}
