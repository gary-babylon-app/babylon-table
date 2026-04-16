package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class ColumnLocalDatesTest
{
    @Test
    void stringToDateShouldHandleDefaultAndExplicitFormats()
    {
        assertEquals(LocalDate.of(2026, 3, 1), ColumnLocalDates.stringToDate("2026-03-01"));
        assertEquals(LocalDate.of(2026, 3, 1), ColumnLocalDates.stringToDate("01/03/2026", DateFormat.DMY));
        assertEquals(LocalDate.of(2024, 2, 29), ColumnLocalDates.stringToDate("20240229", DateFormat.YMD));
        assertEquals(LocalDate.of(2026, 2, 15), ColumnLocalDates.stringToDate("15-02-2026", DateFormat.DMY));
        assertEquals(LocalDate.of(2026, 1, 1), ColumnLocalDates.stringToDate("1-Jan-2026", DateFormat.DMY));
        assertEquals(LocalDate.of(2026, 1, 1), ColumnLocalDates.stringToDate("2026Jan1", DateFormat.YMD));
        assertEquals(LocalDate.ofEpochDay(45200L + (LocalDate.EPOCH.toEpochDay() - 25569L)),
                ColumnLocalDates.stringToDate("45200", null));
        assertNull(ColumnLocalDates.stringToDate("31-02-2026", DateFormat.DMY));
        assertNull(ColumnLocalDates.stringToDate("abc", DateFormat.DMY));
        assertEquals(null, ColumnLocalDates.stringToDate(" "));
    }

    @Test
    void shouldRecogniseDateColumnsAndComputeMinimum()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> strings = ColumnObject.builder(TRADE_DATE, ColumnTypes.STRING);
        strings.add("2026-03-01");
        strings.add("2026-03-02");

        ColumnObject.Builder<LocalDate> dates = ColumnObject.builder(TRADE_DATE, ColumnTypes.LOCALDATE);
        dates.add(LocalDate.of(2026, 3, 2));
        dates.add(LocalDate.of(2026, 3, 1));

        ColumnObject.Builder<Integer> ints = ColumnObject.builder(ColumnName.of("quantity"), ColumnTypes.INT_OBJECT);
        ints.add(1);
        ColumnObject<String> stringDates = strings.build();
        ColumnObject<LocalDate> localDates = dates.build();
        ColumnObject<Integer> quantities = ints.build();

        assertTrue(ColumnLocalDates.isAllDates(stringDates));
        assertTrue(ColumnLocalDates.isLocalDate((Column) stringDates));
        assertTrue(ColumnLocalDates.isLocalDate((Column) localDates));
        assertFalse(ColumnLocalDates.isLocalDate((Column) quantities));
        assertEquals(LocalDate.of(2026, 3, 1), ColumnLocalDates.getMinimum(localDates));
    }

    @Test
    void getMinimumShouldRejectEmptyOrUnsetColumns()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<LocalDate> empty = ColumnObject.builder(TRADE_DATE, ColumnTypes.LOCALDATE);
        ColumnObject.Builder<LocalDate> unset = ColumnObject.builder(TRADE_DATE, ColumnTypes.LOCALDATE);
        unset.addNull();

        assertThrows(RuntimeException.class, () -> ColumnLocalDates.getMinimum(empty.build()));
        assertThrows(RuntimeException.class, () -> ColumnLocalDates.getMinimum(unset.build()));
    }
}
