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

import org.junit.jupiter.api.Test;

public class TablesTest
{
    @Test
    public void pruneShouldRemoveColumnsThatAreNoneSet()
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

        TableColumnar pruned = table.prune();

        assertEquals(2, table.getColumnCount());
        assertEquals(1, pruned.getColumnCount());
        assertEquals(ColumnName.of("A"), pruned.getColumnNames()[0]);
        assertEquals("a0", pruned.getString(ColumnName.of("A")).get(0));
    }
}
