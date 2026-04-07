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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class LocalDatesStringToDateTest
{
    @Test
    void stringToDate2ShouldParseEightDigitYmd()
    {
        LocalDate d = ColumnLocalDates.stringToDate("20240229", DateFormat.YMD);
        assertEquals(LocalDate.of(2024, 2, 29), d);
    }

    @Test
    void stringToDate2ShouldParseDmyWithSeparators()
    {
        LocalDate d = ColumnLocalDates.stringToDate("15-02-2026", DateFormat.DMY);
        assertEquals(LocalDate.of(2026, 2, 15), d);
    }

    @Test
    void stringToDate2ShouldParseAlphaMonth()
    {
        LocalDate d = ColumnLocalDates.stringToDate("1-Jan-2026", DateFormat.DMY);
        assertEquals(LocalDate.of(2026, 1, 1), d);
    }

    @Test
    void stringToDate2ShouldParseAlphaMonth2()
    {
        LocalDate d = ColumnLocalDates.stringToDate("2026Jan1", DateFormat.YMD);
        assertEquals(LocalDate.of(2026, 1, 1), d);
    }

    @Test
    void stringToDate2ShouldParseExcelDate()
    {
        LocalDate d = ColumnLocalDates.stringToDate("45200", null);
        assertEquals(LocalDate.ofEpochDay(45200L + (LocalDate.EPOCH.toEpochDay() - 25569L)), d);
    }

    @Test
    void stringToDate2ShouldReturnNullForInvalidDate()
    {
        assertNull(ColumnLocalDates.stringToDate("31-02-2026", DateFormat.DMY));
        assertNull(ColumnLocalDates.stringToDate("abc", DateFormat.DMY));
    }
}
