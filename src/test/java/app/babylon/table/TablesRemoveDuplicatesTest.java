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

import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class TablesRemoveDuplicatesTest
{
    @Test
    void removeDuplicatesShouldKeepDistinctObjectKeys()
    {
        final ColumnName ID = ColumnName.of("id");
        final ColumnName NAME = ColumnName.of("name");

        ColumnObject.Builder<String> ids = ColumnObject.builder(ID, app.babylon.table.column.ColumnTypes.STRING);
        ids.add("A");
        ids.add("B");
        ids.add("B");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("Bob duplicate");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), ids.build(), names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, ID);

        assertEquals(2, deduped.getRowCount());
        assertEquals("A", deduped.getString(ID).get(0));
        assertEquals("B", deduped.getString(ID).get(1));
        assertEquals("Alice", deduped.getString(NAME).get(0));
        assertEquals("Bob", deduped.getString(NAME).get(1));
    }

    @Test
    void removeDuplicatesShouldKeepDistinctIntKeys()
    {
        final ColumnName ID = ColumnName.of("id");
        final ColumnName NAME = ColumnName.of("name");

        ColumnInt.Builder ids = ColumnInt.builder(ID);
        ids.add(1);
        ids.add(2);
        ids.add(2);

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("Bob duplicate");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), ids.build(), names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, ID);

        assertEquals(2, deduped.getRowCount());
        assertEquals(1, deduped.getInt(ID).get(0));
        assertEquals(2, deduped.getInt(ID).get(1));
        assertEquals("Alice", deduped.getString(NAME).get(0));
        assertEquals("Bob", deduped.getString(NAME).get(1));
    }

    @Test
    void removeDuplicatesShouldKeepDistinctLongKeys()
    {
        final ColumnName ID = ColumnName.of("id");
        final ColumnName NAME = ColumnName.of("name");

        ColumnLong.Builder ids = ColumnLong.builder(ID);
        ids.add(100L);
        ids.add(200L);
        ids.add(200L);

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("Bob duplicate");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), ids.build(), names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, ID);

        assertEquals(2, deduped.getRowCount());
        assertEquals(100L, deduped.getLong(ID).get(0));
        assertEquals(200L, deduped.getLong(ID).get(1));
        assertEquals("Alice", deduped.getString(NAME).get(0));
        assertEquals("Bob", deduped.getString(NAME).get(1));
    }

    @Test
    void removeDuplicatesShouldNormaliseBigDecimalTrailingZeros()
    {
        final ColumnName AMOUNT = ColumnName.of("amount");
        final ColumnName NAME = ColumnName.of("name");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.DECIMAL);
        amounts.add(new BigDecimal("1.0"));
        amounts.add(new BigDecimal("1.00"));
        amounts.add(new BigDecimal("2.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Alice");
        names.add("Alice duplicate");
        names.add("Bob");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amounts.build(),
                names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, AMOUNT);

        assertEquals(2, deduped.getRowCount());
        assertEquals(new BigDecimal("1.0"), deduped.getDecimal(AMOUNT).get(0));
        assertEquals(new BigDecimal("2.0"), deduped.getDecimal(AMOUNT).get(1));
        assertEquals("Alice", deduped.getString(NAME).get(0));
        assertEquals("Bob", deduped.getString(NAME).get(1));
    }

    @Test
    void removeDuplicatesShouldNormaliseBigDecimalZeroValues()
    {
        final ColumnName AMOUNT = ColumnName.of("amount");
        final ColumnName NAME = ColumnName.of("name");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.DECIMAL);
        amounts.add(new BigDecimal("0.0"));
        amounts.add(new BigDecimal("0.00"));
        amounts.add(new BigDecimal("1.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Zero");
        names.add("Zero duplicate");
        names.add("One");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amounts.build(),
                names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, AMOUNT);

        assertEquals(2, deduped.getRowCount());
        assertEquals(new BigDecimal("0.0"), deduped.getDecimal(AMOUNT).get(0));
        assertEquals(new BigDecimal("1.0"), deduped.getDecimal(AMOUNT).get(1));
        assertEquals("Zero", deduped.getString(NAME).get(0));
        assertEquals("One", deduped.getString(NAME).get(1));
    }

    @Test
    void removeDuplicatesShouldNormaliseBigDecimalLargerScaledValues()
    {
        final ColumnName AMOUNT = ColumnName.of("amount");
        final ColumnName NAME = ColumnName.of("name");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.DECIMAL);
        amounts.add(new BigDecimal("1000.0"));
        amounts.add(new BigDecimal("1000"));
        amounts.add(new BigDecimal("2000"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Thousand");
        names.add("Thousand duplicate");
        names.add("Two thousand");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amounts.build(),
                names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, AMOUNT);

        assertEquals(2, deduped.getRowCount());
        assertEquals(new BigDecimal("1000.0"), deduped.getDecimal(AMOUNT).get(0));
        assertEquals(new BigDecimal("2000"), deduped.getDecimal(AMOUNT).get(1));
        assertEquals("Thousand", deduped.getString(NAME).get(0));
        assertEquals("Two thousand", deduped.getString(NAME).get(1));
    }

    @Test
    void removeDuplicatesShouldTreatNullBigDecimalValuesAsEqual()
    {
        final ColumnName AMOUNT = ColumnName.of("amount");
        final ColumnName NAME = ColumnName.of("name");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.DECIMAL);
        amounts.addNull();
        amounts.addNull();
        amounts.add(new BigDecimal("1.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Null");
        names.add("Null duplicate");
        names.add("One");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amounts.build(),
                names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, AMOUNT);

        assertEquals(2, deduped.getRowCount());
        assertEquals("Null", deduped.getString(NAME).get(0));
        assertEquals("One", deduped.getString(NAME).get(1));
        assertEquals(new BigDecimal("1.0"), deduped.getDecimal(AMOUNT).get(1));
    }

    @Test
    void removeDuplicatesShouldTreatNullAndNonNullBigDecimalValuesAsDifferent()
    {
        final ColumnName AMOUNT = ColumnName.of("amount");
        final ColumnName NAME = ColumnName.of("name");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.DECIMAL);
        amounts.addNull();
        amounts.add(new BigDecimal("0.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("Null");
        names.add("Zero");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amounts.build(),
                names.build());

        TableColumnar deduped = Tables.removeDuplicates(table, AMOUNT);

        assertEquals(2, deduped.getRowCount());
        assertEquals("Null", deduped.getString(NAME).get(0));
        assertEquals("Zero", deduped.getString(NAME).get(1));
    }

    @Test
    void removeDuplicatesShouldRejectUnsupportedDoubleKeys()
    {
        final ColumnName VALUE = ColumnName.of("value");

        ColumnDouble.Builder values = ColumnDouble.builder(VALUE);
        values.add(1.0);
        values.add(2.0);

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), values.build());

        assertThrows(IllegalArgumentException.class, () -> Tables.removeDuplicates(table, VALUE));
    }
}
