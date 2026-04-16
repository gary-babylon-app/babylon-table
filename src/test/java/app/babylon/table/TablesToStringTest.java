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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TablesToStringTest
{
    @Test
    void printFullTableShouldRenderAllRows()
    {
        TableColumnar table = table("City", "Temp", new String[]
        {"London", "12"}, new String[]
        {"Paris", "18"});

        String printed = TablesToString.printFullTable(table);

        assertTrue(printed.contains("cities"));
        assertTrue(printed.contains("sample"));
        assertTrue(printed.contains("|City  |Temp|"));
        assertTrue(printed.contains("|London|  12|"));
        assertTrue(printed.contains("|Paris |  18|"));
    }

    @Test
    void printFullTableShouldRespectRowLimit()
    {
        TableColumnar table = table("City", "Temp", new String[]
        {"London", "12"}, new String[]
        {"Paris", "18"}, new String[]
        {"Rome", "22"});

        String printed = TablesToString.printFullTable(table, ToStringSettings.standard(), 2);

        assertTrue(printed.contains("|London|  12|"));
        assertTrue(printed.contains("|Paris |  18|"));
        assertTrue(!printed.contains("|Rome  |  22|"));
    }

    @Test
    void printSmallTableShouldMatchFullTableForSmallTables()
    {
        TableColumnar table = table("City", "Temp", new String[]
        {"London", "12"}, new String[]
        {"Paris", "18"});

        assertEquals(TablesToString.printFullTable(table), TablesToString.printSmallTable(table));
    }

    @Test
    void printSmallTableShouldUseSettingsWhenFormatting()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnObject.Builder<java.math.BigDecimal> amounts = ColumnObject.builderDecimal(AMOUNT);
        amounts.add(new java.math.BigDecimal("1.234"));
        TableColumnar table = Tables.newTable(TableName.of("amounts"), new TableDescription("formatted"),
                amounts.build());

        ToStringSettings settings = new ToStringSettings().withDecimalFormatter(new DecimalFormat("#0.000"));

        String printed = TablesToString.printSmallTable(table, settings);

        assertTrue(printed.contains("1.234"));
    }

    @Test
    void printFullTableShouldUsePlainDecimalNotation()
    {
        final ColumnName QUANTITY = ColumnName.of("QUANTITY");
        ColumnObject.Builder<BigDecimal> quantity = ColumnObject.builderDecimal(QUANTITY);
        quantity.add(new BigDecimal("1E+2"));
        quantity.add(new BigDecimal("-4E+1"));

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), quantity.build());

        String printed = table.toString(ToStringSettings.standard());

        assertTrue(printed.contains("100"));
        assertTrue(printed.contains("-40"));
        assertFalse(printed.contains("1E+2"));
        assertFalse(printed.contains("-4E+1"));
    }

    @Test
    void printSmallTableShouldTruncateLargeTables()
    {
        TableColumnar table = largeTable();

        String printed = TablesToString.printSmallTable(table);

        assertTrue(printed.contains(" ... |"));
        assertTrue(printed.contains("...\n...\n...\n"));
        assertTrue(printed.contains("C0"));
        assertTrue(printed.contains("C9"));
    }

    private static TableColumnar table(String firstName, String secondName, String[]... rows)
    {
        ColumnObject.Builder<String> first = ColumnObject.builder(ColumnName.of(firstName),
                app.babylon.table.column.ColumnTypes.STRING);
        ColumnObject.Builder<String> second = ColumnObject.builder(ColumnName.of(secondName),
                app.babylon.table.column.ColumnTypes.STRING);
        for (String[] row : rows)
        {
            first.add(row[0]);
            second.add(row[1]);
        }
        return Tables.newTable(TableName.of("cities"), new TableDescription("sample"), first.build(), second.build());
    }

    private static TableColumnar largeTable()
    {
        app.babylon.table.column.Column[] columns = new app.babylon.table.column.Column[10];
        for (int i = 0; i < columns.length; ++i)
        {
            ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("C" + i),
                    app.babylon.table.column.ColumnTypes.STRING);
            for (int row = 0; row < 30; ++row)
            {
                builder.add("r" + row + "c" + i);
            }
            columns[i] = builder.build();
        }
        return Tables.newTable(TableName.of("wide"), new TableDescription("large"), columns);
    }
}
