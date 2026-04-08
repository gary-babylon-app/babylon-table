/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class ColumnFilterTest
{
    @Test
    void filterShouldWorkForObjectColumns()
    {
        ColumnObject.Builder<String> names = ColumnObject.builder(ColumnName.of("name"), String.class);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        TableColumnar filtered = table
                .filter(ColumnFilter.of(ColumnName.of("name"), (Predicate<Object>) x -> ((String) x).startsWith("A")));

        assertEquals(2, filtered.getRowCount());
        assertEquals("Alice", filtered.getString(ColumnName.of("name")).get(0));
        assertEquals("Amy", filtered.getString(ColumnName.of("name")).get(1));
    }

    @Test
    void filterShouldWorkForPrimitiveColumns()
    {
        ColumnInt.Builder ints = ColumnInt.builder(ColumnName.of("i"));
        ints.add(1);
        ints.add(2);
        ints.add(3);

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), ints.build());

        TableColumnar filtered = table.filter(ColumnFilter.of(ColumnName.of("i"), (int x) -> x >= 2));

        assertEquals(2, filtered.getRowCount());
    }

    @Test
    void filterShouldComposeAndShortCircuit()
    {
        ColumnObject.Builder<String> names = ColumnObject.builder(ColumnName.of("name"), String.class);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        AtomicInteger rightEvaluations = new AtomicInteger();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        ColumnFilter left = ColumnFilter.of(ColumnName.of("name"),
                (Predicate<Object>) x -> ((String) x).startsWith("A"));
        ColumnFilter right = ColumnFilter.of(ColumnName.of("name"), (Predicate<Object>) x -> {
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
        ColumnObject.Builder<String> names = ColumnObject.builder(ColumnName.of("name"), String.class);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        ColumnFilter startsWithA = ColumnFilter.of(ColumnName.of("name"),
                (Predicate<Object>) x -> ((String) x).startsWith("A"));
        ColumnFilter startsWithB = ColumnFilter.of(ColumnName.of("name"),
                (Predicate<Object>) x -> ((String) x).startsWith("B"));

        TableColumnar filtered = table.filter(startsWithA.or(startsWithB));

        assertEquals(3, filtered.getRowCount());
    }

    @Test
    void filterShouldComposeNot()
    {
        ColumnObject.Builder<String> names = ColumnObject.builder(ColumnName.of("name"), String.class);
        names.add("Alice");
        names.add("Bob");
        names.add("Amy");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        ColumnFilter startsWithA = ColumnFilter.of(ColumnName.of("name"),
                (Predicate<Object>) x -> ((String) x).startsWith("A"));

        TableColumnar filtered = table.filter(startsWithA.not());

        assertEquals(1, filtered.getRowCount());
        assertEquals("Bob", filtered.getString(ColumnName.of("name")).get(0));
    }

    @Test
    void filterShouldReturnEmptyTableInsteadOfNullWhenNoRowsMatch()
    {
        ColumnObject.Builder<String> names = ColumnObject.builder(ColumnName.of("name"), String.class);
        names.add("Alice");
        names.add("Bob");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        TableColumnar filtered = table
                .filter(ColumnFilter.of(ColumnName.of("name"), (Predicate<Object>) x -> ((String) x).startsWith("Z")));

        assertNotNull(filtered);
        assertEquals(0, filtered.getRowCount());
        assertEquals(1, filtered.getColumnCount());
        assertEquals(ColumnName.of("name"), filtered.getColumnNames()[0]);
    }
}
