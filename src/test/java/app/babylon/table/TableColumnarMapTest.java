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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Transformer;
import app.babylon.table.column.type.TypeParsers;

class TableColumnarMapTest
{
    private enum Status
    {
        OPEN, CLOSED
    }

    private static final Column.Type STATUS_TYPE = Column.Type.of(Status.class, TypeParsers.NULL);

    private static TableColumnar sampleTable()
    {
        final ColumnName ID = ColumnName.of("Id");
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName STATUS = ColumnName.of("Status");
        final ColumnName MISSING = ColumnName.of("Missing");

        ColumnInt.Builder ids = ColumnInt.builder(ID);
        ids.add(1);
        ids.add(2);

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT, ColumnTypes.DECIMAL);
        amounts.add(new BigDecimal("10.5"));
        amounts.add(new BigDecimal("20.0"));

        ColumnCategorical.Builder<Status> statuses = ColumnCategorical.builder(STATUS, STATUS_TYPE);
        statuses.add(Status.OPEN);
        statuses.add(Status.CLOSED);

        return Tables.newTable(TableName.of("sample"), new TableDescription("desc"), ids.build(), names.build(),
                amounts.build(), statuses.build());
    }

    void commonAccessorsShouldExposeTypedColumnsRowsAndCollections()
    {
        final ColumnName ID = ColumnName.of("Id");
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName STATUS = ColumnName.of("Status");
        final ColumnName MISSING = ColumnName.of("Missing");

        TableColumnar table = sampleTable();

        assertEquals(TableName.of("sample"), table.getName());
        assertEquals("desc", table.getDescription().getValue());
        assertEquals(4, table.getColumnCount());
        assertEquals(2, table.getRowCount());

        assertSame(table.getString(NAME), table.getObject(NAME, ColumnTypes.STRING));
        assertSame(table.getDecimal(AMOUNT), table.getObject(AMOUNT, ColumnTypes.DECIMAL));
        assertSame(table.getEnum(STATUS), table.getCategorical(STATUS));
        assertSame(table.getEnum(STATUS), table.getCategorical(STATUS, STATUS_TYPE));
        assertNull(table.getString(MISSING));
        assertNull(table.getDouble(MISSING));
        assertNull(table.getLong(MISSING));
        assertNull(table.getInt(MISSING));
        assertNull(table.getCategorical(MISSING));

        List<ColumnName> names = new ArrayList<>();
        table.getColumnNames(names);
        assertArrayEquals(new ColumnName[]
        {ID, NAME, AMOUNT, STATUS}, names.toArray(new ColumnName[0]));

        List<ColumnName> namesFromArray = List.of(table.getColumnNames());
        assertEquals(4, namesFromArray.size());
        assertEquals(ID, namesFromArray.get(0));
        assertEquals(STATUS, namesFromArray.get(3));

        List<Column> iterated = new ArrayList<>();
        for (Column column : table.columns())
        {
            iterated.add(column);
        }
        assertEquals(4, iterated.size());
        assertSame(table.get(ID), iterated.get(0));
        assertSame(table.get(STATUS), iterated.get(3));

        Map<ColumnName, Column> selected = table.getColumns(new LinkedHashMap<>(), NAME, STATUS);
        assertEquals(2, selected.size());
        assertTrue(selected.containsKey(NAME));
        assertTrue(selected.containsKey(STATUS));

        TableColumnar firstRow = table.getFirstRow();
        TableColumnar lastRow = table.getLastRow();
        TableColumnar secondRow = table.getRow(1);
        assertEquals(1, firstRow.getRowCount());
        assertEquals(1, lastRow.getRowCount());
        assertEquals("Alice", firstRow.getString(NAME).get(0));
        assertEquals("Bob", lastRow.getString(NAME).get(0));
        assertEquals(2, secondRow.getInt(ID).get(0));
    }

    @Test
    void commonAccessorsShouldValidateWrongTypeRequests()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName STATUS = ColumnName.of("Status");

        TableColumnar table = sampleTable();

        assertThrows(IllegalArgumentException.class, () -> table.getObject(NAME, null));
        assertThrows(IllegalArgumentException.class, () -> table.getCategorical(STATUS, null));
        assertThrows(RuntimeException.class, () -> table.getDouble(NAME));
        assertThrows(RuntimeException.class, () -> table.getLong(NAME));
        assertThrows(RuntimeException.class, () -> table.getInt(NAME));
        assertThrows(RuntimeException.class, () -> table.getCategorical(NAME));
        assertThrows(RuntimeException.class, () -> table.getObject(NAME, ColumnTypes.DECIMAL));
        assertThrows(RuntimeException.class, () -> table.getCategorical(STATUS, ColumnTypes.STRING));
    }

    @Test
    void pruneShouldRemoveColumnsThatAreNoneSet()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName EMPTY = ColumnName.of("EMPTY");
        ColumnObject.Builder<String> a = ColumnObject.builder(A_2, ColumnTypes.STRING);
        a.add("a0");
        a.add("a1");

        ColumnObject.Builder<String> empty = ColumnObject.builder(EMPTY, ColumnTypes.STRING);
        empty.addNull();
        empty.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), a.build(), empty.build());
        TableColumnar pruned = table.prune();

        assertEquals(2, table.getColumnCount());
        assertEquals(1, pruned.getColumnCount());
        assertEquals(A_2, pruned.getColumnNames()[0]);
        assertEquals("a0", pruned.getString(A_2).get(0));
    }

    @Test
    void pruneShouldReturnSameTableWhenNoColumnsAreEmpty()
    {
        TableColumnar table = sampleTable();

        TableColumnar pruned = table.prune();

        assertSame(table, pruned);
    }

    @Test
    void replaceShouldSwapNamedColumnsAndPreserveOrder()
    {
        final ColumnName ID = ColumnName.of("Id");
        final ColumnName STATUS = ColumnName.of("Status");
        final ColumnName AMOUNT = ColumnName.of("Amount");

        TableColumnar table = sampleTable();

        ColumnObject.Builder<String> amountStrings = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        amountStrings.add("30.5");
        amountStrings.add("40.0");
        ColumnObject<BigDecimal> amounts = amountStrings.build()
                .transform(Transformer.of(BigDecimal::new, ColumnTypes.DECIMAL));

        ColumnCategorical.Builder<Status> statuses = ColumnCategorical.builder(STATUS, STATUS_TYPE);
        statuses.add(Status.CLOSED);
        statuses.add(Status.OPEN);

        TableColumnar replaced = table.replace(amounts, statuses.build());

        assertArrayEquals(table.getColumnNames(), replaced.getColumnNames());
        assertEquals(new BigDecimal("30.5"), replaced.getDecimal(AMOUNT).get(0));
        assertEquals(new BigDecimal("40.0"), replaced.getDecimal(AMOUNT).get(1));
        assertEquals(Status.CLOSED, replaced.getEnum(STATUS).get(0));
        assertEquals(Status.OPEN, replaced.getEnum(STATUS).get(1));
        assertEquals(1, replaced.getInt(ID).get(0));
    }

    @Test
    void replaceShouldThrowWhenNamedColumnDoesNotExist()
    {
        final ColumnName MISSING = ColumnName.of("Missing");
        TableColumnar table = sampleTable();

        ColumnObject.Builder<String> missing = ColumnObject.builder(MISSING, ColumnTypes.STRING);
        missing.add("x");
        missing.add("y");

        assertThrows(RuntimeException.class, () -> table.replace(missing.build()));
    }

    @Test
    void addShouldIgnoreNullColumn()
    {
        TableColumnar table = sampleTable();

        assertSame(table, table.add((Column) null));
        assertSame(table, table.add((Column[]) null));
    }

    @Test
    void pruneShouldRemoveZeroRowColumns()
    {
        final ColumnName A_2 = ColumnName.of("A");
        final ColumnName B_2 = ColumnName.of("B");

        ColumnObject.Builder<String> a = ColumnObject.builder(A_2, ColumnTypes.STRING);
        ColumnInt.Builder b = ColumnInt.builder(B_2);

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), a.build(), b.build());
        TableColumnar pruned = table.prune();

        assertEquals(2, table.getColumnCount());
        assertEquals(0, table.getRowCount());
        assertEquals(0, pruned.getColumnCount());
        assertEquals(0, pruned.getRowCount());
    }

    @Test
    void removeDuplicatesShouldKeepDistinctObjectKeys()
    {
        final ColumnName ID = ColumnName.of("id");
        final ColumnName NAME = ColumnName.of("name");

        ColumnObject.Builder<String> ids = ColumnObject.builder(ID, ColumnTypes.STRING);
        ids.add("A");
        ids.add("B");
        ids.add("B");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT, ColumnTypes.DECIMAL);
        amounts.add(new BigDecimal("1.0"));
        amounts.add(new BigDecimal("1.00"));
        amounts.add(new BigDecimal("2.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT, ColumnTypes.DECIMAL);
        amounts.add(new BigDecimal("0.0"));
        amounts.add(new BigDecimal("0.00"));
        amounts.add(new BigDecimal("1.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT, ColumnTypes.DECIMAL);
        amounts.add(new BigDecimal("1000.0"));
        amounts.add(new BigDecimal("1000"));
        amounts.add(new BigDecimal("2000"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT, ColumnTypes.DECIMAL);
        amounts.addNull();
        amounts.addNull();
        amounts.add(new BigDecimal("1.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builder(AMOUNT, ColumnTypes.DECIMAL);
        amounts.addNull();
        amounts.add(new BigDecimal("0.0"));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
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
