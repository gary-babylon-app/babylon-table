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

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TableColumnarJoinTest
{
    @Test
    public void joinTableShouldExposeLeftColumnsAndJoinAwareRightColumns()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName LEFT_VALUE = ColumnName.of("LeftValue");
        final ColumnName RIGHT_VALUE = ColumnName.of("RightValue");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, String.class);
        leftKey.add("A");
        leftKey.add("B");

        ColumnObject.Builder<String> leftValues = ColumnObject.builder(LEFT_VALUE, String.class);
        leftValues.add("left-a");
        leftValues.add("left-b");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build(), leftValues.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(KEY, String.class);
        rightKey.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(RIGHT_VALUE, String.class);
        rightValues.add("right-a");

        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), rightValues.build());

        ViewIndex rowIndex = ViewIndex.builder().add(0).addNull().build();

        TableColumnar joined = new TableColumnarLeftOuterJoin(TableName.of("joined"), new TableDescription(""), left,
                right, rowIndex, RIGHT_VALUE);

        assertEquals(2, joined.getRowCount());
        assertTrue(joined.contains(KEY));
        assertTrue(joined.contains(LEFT_VALUE));
        assertTrue(joined.contains(RIGHT_VALUE));
        assertEquals("left-a", joined.getString(LEFT_VALUE).get(0));
        assertEquals("left-b", joined.getString(LEFT_VALUE).get(1));
        assertEquals("right-a", joined.getString(RIGHT_VALUE).get(0));
        assertNull(joined.getString(RIGHT_VALUE).get(1));
        assertFalse(joined.getString(RIGHT_VALUE).isSet(1));

        Column first = joined.get(RIGHT_VALUE);
        Column second = joined.get(RIGHT_VALUE);
        assertSame(first, second);
    }

    @Test
    public void joinTableShouldUseNullableViewSemanticsForPrimitiveRightColumns()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName AMOUNT = ColumnName.of("Amount");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, String.class);
        leftKey.add("A");
        leftKey.add("B");
        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(KEY, String.class);
        rightKey.add("A");
        ColumnDouble.Builder amounts = ColumnDouble.builder(AMOUNT);
        amounts.add(42.5d);
        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), amounts.build());

        ViewIndex rowIndex = ViewIndex.builder().add(0).addNull().build();

        TableColumnar joined = new TableColumnarLeftOuterJoin(TableName.of("joined"), new TableDescription(""), left,
                right, rowIndex, AMOUNT);

        ColumnDouble amountView = (ColumnDouble) joined.get(AMOUNT);
        assertEquals(42.5d, amountView.get(0));
        assertTrue(amountView.isSet(0));
        assertEquals(0.0d, amountView.get(1));
        assertFalse(amountView.isSet(1));
    }
}
