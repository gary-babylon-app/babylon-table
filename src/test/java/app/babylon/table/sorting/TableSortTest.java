/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.sorting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.sorting.TableSort.SortOrder;

public class TableSortTest
{
    @Test
    public void sortShouldOrderRowsNaturallyAndKeepUnsetValuesFirst()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName SCORE = ColumnName.of("Score");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Charlie");
        names.add("Alice");
        names.add("Bob");
        names.add("Delta");

        ColumnInt.Builder scores = ColumnInt.builder(SCORE);
        scores.add(3);
        scores.addNull();
        scores.add(2);
        scores.add(1);

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build(),
                scores.build());

        TableColumnar sorted = TableSort.sort(table, SCORE);

        assertEquals(4, sorted.getRowCount());
        assertEquals(0, sorted.getInt(SCORE).get(0));
        assertFalse(sorted.getInt(SCORE).isSet(0));
        assertEquals("Alice", sorted.getString(NAME).get(0));
        assertEquals(1, sorted.getInt(SCORE).get(1));
        assertEquals("Delta", sorted.getString(NAME).get(1));
        assertEquals(2, sorted.getInt(SCORE).get(2));
        assertEquals("Bob", sorted.getString(NAME).get(2));
        assertEquals(3, sorted.getInt(SCORE).get(3));
        assertEquals("Charlie", sorted.getString(NAME).get(3));
    }

    @Test
    public void sortShouldOrderRowsInReverse()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName SCORE = ColumnName.of("Score");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Charlie");
        names.add("Alice");
        names.add("Bob");
        names.add("Delta");

        ColumnInt.Builder scores = ColumnInt.builder(SCORE);
        scores.add(3);
        scores.addNull();
        scores.add(2);
        scores.add(1);

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build(),
                scores.build());

        TableColumnar sorted = TableSort.sort(table, SortOrder.Reverse, SCORE);

        assertEquals(4, sorted.getRowCount());
        assertEquals(3, sorted.getInt(SCORE).get(0));
        assertEquals("Charlie", sorted.getString(NAME).get(0));
        assertEquals(2, sorted.getInt(SCORE).get(1));
        assertEquals("Bob", sorted.getString(NAME).get(1));
        assertEquals(1, sorted.getInt(SCORE).get(2));
        assertEquals("Delta", sorted.getString(NAME).get(2));
        assertEquals(0, sorted.getInt(SCORE).get(3));
        assertFalse(sorted.getInt(SCORE).isSet(3));
        assertEquals("Alice", sorted.getString(NAME).get(3));
    }

    @Test
    public void sortShouldUseLaterColumnsToBreakTies()
    {
        final ColumnName REGION = ColumnName.of("Region");
        final ColumnName NAME = ColumnName.of("Name");

        ColumnObject.Builder<String> regions = ColumnObject.builder(REGION, ColumnTypes.STRING);
        regions.add("B");
        regions.add("A");
        regions.add("A");
        regions.addNull();
        regions.add("B");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Zulu");
        names.add("Bravo");
        names.add("Alpha");
        names.add("Ghost");
        names.add("Echo");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), regions.build(),
                names.build());

        TableColumnar sorted = TableSort.sort(table, REGION, NAME);

        assertEquals("Ghost", sorted.getString(NAME).get(0));
        assertEquals(null, sorted.getString(REGION).get(0));
        assertEquals("Alpha", sorted.getString(NAME).get(1));
        assertEquals("A", sorted.getString(REGION).get(1));
        assertEquals("Bravo", sorted.getString(NAME).get(2));
        assertEquals("A", sorted.getString(REGION).get(2));
        assertEquals("Echo", sorted.getString(NAME).get(3));
        assertEquals("B", sorted.getString(REGION).get(3));
        assertEquals("Zulu", sorted.getString(NAME).get(4));
        assertEquals("B", sorted.getString(REGION).get(4));
    }

    @Test
    public void sortShouldBeStableWhenKeysAreEqual()
    {
        final ColumnName GROUP = ColumnName.of("Group");
        final ColumnName NAME = ColumnName.of("Name");

        ColumnObject.Builder<String> groups = ColumnObject.builder(GROUP, ColumnTypes.STRING);
        groups.add("A");
        groups.add("A");
        groups.add("A");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("first");
        names.add("second");
        names.add("third");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), groups.build(),
                names.build());

        TableColumnar sorted = TableSort.sort(table, GROUP);

        assertEquals("first", sorted.getString(NAME).get(0));
        assertEquals("second", sorted.getString(NAME).get(1));
        assertEquals("third", sorted.getString(NAME).get(2));
    }

    @Test
    public void sortShouldReturnOriginalTableForEmptyTables()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        assertSame(table, TableSort.sort(table, NAME));
    }

    @Test
    public void sortShouldReturnOriginalTableWhenSortColumnsAreNull()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        assertSame(table, TableSort.sort(table, (ColumnName[]) null));
    }
}
