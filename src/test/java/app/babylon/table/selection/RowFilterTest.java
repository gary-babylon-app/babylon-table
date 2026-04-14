/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.selection;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class RowFilterTest
{
    @Test
    void filterShouldWorkForObjectColumns()
    {
        final ColumnName NAME = ColumnName.of("NAME");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        TableColumnar filtered = table
                .filter(RowFilter.of(NAME, (Predicate<Object>) x -> ((String) x).startsWith("A")));

        assertEquals(2, filtered.getRowCount());
        assertEquals("Alice", filtered.getString(NAME).get(0));
        assertEquals("Amy", filtered.getString(NAME).get(1));
    }

    @Test
    void filterShouldWorkForPrimitiveColumns()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder ints = ColumnInt.builder(I);
        ints.add(1);
        ints.add(2);
        ints.add(3);

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), ints.build());

        TableColumnar filtered = table.filter(RowFilter.of(I, (int x) -> x >= 2));

        assertEquals(2, filtered.getRowCount());
    }

    @Test
    void filterShouldComposeAndShortCircuit()
    {
        final ColumnName NAME = ColumnName.of("NAME");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        AtomicInteger rightEvaluations = new AtomicInteger();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        RowFilter left = RowFilter.of(NAME, (Predicate<Object>) x -> ((String) x).startsWith("A"));
        RowFilter right = RowFilter.of(NAME, (Predicate<Object>) x -> {
            rightEvaluations.incrementAndGet();
            return ((String) x).length() > 3;
        });

        TableColumnar filtered = table.filter(left.and(right.not().not()));

        assertEquals(1, filtered.getRowCount());
        assertEquals(2, rightEvaluations.get());
    }

    @Test
    void filterShouldComposeOr()
    {
        final ColumnName NAME = ColumnName.of("NAME");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        RowFilter startsWithA = RowFilter.of(NAME, (Predicate<Object>) x -> ((String) x).startsWith("A"));
        RowFilter startsWithB = RowFilter.of(NAME, (Predicate<Object>) x -> ((String) x).startsWith("B"));

        TableColumnar filtered = table.filter(startsWithA.or(startsWithB));

        assertEquals(3, filtered.getRowCount());
    }

    @Test
    void filterShouldComposeNot()
    {
        final ColumnName NAME = ColumnName.of("NAME");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        RowFilter startsWithA = RowFilter.of(NAME, (Predicate<Object>) x -> ((String) x).startsWith("A"));

        TableColumnar filtered = table.filter(startsWithA.not());

        assertEquals(1, filtered.getRowCount());
        assertEquals("Bob", filtered.getString(NAME).get(0));
    }

    @Test
    void filterShouldReturnEmptyTableInsteadOfNullWhenNoRowsMatch()
    {
        final ColumnName NAME = ColumnName.of("NAME");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        TableColumnar filtered = table
                .filter(RowFilter.of(NAME, (Predicate<Object>) x -> ((String) x).startsWith("Z")));

        assertNotNull(filtered);
        assertEquals(0, filtered.getRowCount());
        assertEquals(1, filtered.getColumnCount());
        assertEquals(NAME, filtered.getColumnNames()[0]);
    }
}
