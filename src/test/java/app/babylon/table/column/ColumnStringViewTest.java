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

import app.babylon.table.ViewIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ColumnStringViewTest
{
    @Test
    public void test1()
    {
        final ColumnName TEST = ColumnName.of("TEST");
        ColumnObject.Builder<String> original = ColumnObject.builder(TEST, String.class);
        original.add("abc1");
        original.add("abc2");
        original.add("abc3");
        original.add("abc4");

        int[] viewIndex = new int[]
        {1, 3};
        ViewIndex rowIndex = ViewIndex.builder().addAll(viewIndex).build();

        ColumnObject<String> view = Columns.newStringView(original.build(), rowIndex);

        assertEquals(viewIndex.length, view.size());
        assertEquals("abc2", view.get(0));
        assertEquals("abc4", view.get(1));

    }
}
