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

import app.babylon.table.transform.ColumnLocalDates;
import app.babylon.table.transform.DateFormat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class ColumnLocalDateTest
{

    @Test
    public void testParse()
    {
        String v = "45436.2980092593";

        LocalDate d = ColumnLocalDates.stringToDate(v, DateFormat.ExcelLocalDateTime);

        assertNotNull(d);
    }

    @Test
    public void testIsConstant()
    {
        final ColumnName TEST = ColumnName.of("TEST");
        ColumnObject.Builder<LocalDate> dates = ColumnObject.builder(TEST,
                app.babylon.table.column.ColumnTypes.LOCALDATE);
        dates.add(LocalDate.of(2026, 8, 20));
        dates.addNull();
        dates.addNull();

        assertTrue(!dates.build().isConstant());
    }

    @Test
    public void testIsConstant2()
    {
        final ColumnName TEST = ColumnName.of("TEST");
        ColumnObject.Builder<LocalDate> dates = ColumnObject.builder(TEST,
                app.babylon.table.column.ColumnTypes.LOCALDATE);
        dates.addNull();
        dates.addNull();
        dates.add(LocalDate.of(2026, 8, 20));
        dates.addNull();

        assertTrue(!dates.build().isConstant());
    }
}
