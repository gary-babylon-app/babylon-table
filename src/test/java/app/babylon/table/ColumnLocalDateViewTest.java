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

import java.time.LocalDate;

public class ColumnLocalDateViewTest
{

    @Test
    public void test1()
    {

        ColumnObject.Builder<LocalDate> original = ColumnObject.builder(ColumnName.of("Test"), LocalDate.class);
        original.add(ColumnLocalDates.stringToDate("2024-08-20", DateFormat.YMD));
        original.add(ColumnLocalDates.stringToDate("2023/08/20", DateFormat.YMD));
        original.add(ColumnLocalDates.stringToDate("20230220", DateFormat.YMD));
        original.addNull();

        int[] viewIndex = new int[]
        {1, 3};
        ViewIndex rowIndex = ViewIndex.builder().addAll(viewIndex).build();

        ColumnObject<LocalDate> view = original.build().view(rowIndex);

        assertEquals(viewIndex.length, view.size());
        assertEquals(LocalDate.of(2023, 8, 20), view.get(0));
        assertEquals(null, view.get(1));
    }
}
