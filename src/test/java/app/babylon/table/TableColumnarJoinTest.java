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
        ColumnName key = ColumnName.of("Key");
        ColumnName leftValue = ColumnName.of("LeftValue");
        ColumnName rightValue = ColumnName.of("RightValue");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(key, String.class);
        leftKey.add("A");
        leftKey.add("B");

        ColumnObject.Builder<String> leftValues = ColumnObject.builder(leftValue, String.class);
        leftValues.add("left-a");
        leftValues.add("left-b");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build(), leftValues.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(key, String.class);
        rightKey.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(rightValue, String.class);
        rightValues.add("right-a");

        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), rightValues.build());

        ViewIndex rowIndex = ViewIndex.builder().add(0).addNull().build();

        TableColumnar joined = new TableColumnarLeftOuterJoin(TableName.of("joined"), new TableDescription(""), left,
                right, rowIndex, rightValue);

        assertEquals(2, joined.getRowCount());
        assertTrue(joined.contains(key));
        assertTrue(joined.contains(leftValue));
        assertTrue(joined.contains(rightValue));
        assertEquals("left-a", joined.getString(leftValue).get(0));
        assertEquals("left-b", joined.getString(leftValue).get(1));
        assertEquals("right-a", joined.getString(rightValue).get(0));
        assertNull(joined.getString(rightValue).get(1));
        assertFalse(joined.getString(rightValue).isSet(1));

        Column first = joined.get(rightValue);
        Column second = joined.get(rightValue);
        assertSame(first, second);
    }

    @Test
    public void joinTableShouldUseNullableViewSemanticsForPrimitiveRightColumns()
    {
        ColumnName key = ColumnName.of("Key");
        ColumnName amount = ColumnName.of("Amount");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(key, String.class);
        leftKey.add("A");
        leftKey.add("B");
        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(key, String.class);
        rightKey.add("A");
        ColumnDouble.Builder amounts = ColumnDouble.builder(amount);
        amounts.add(42.5d);
        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), amounts.build());

        ViewIndex rowIndex = ViewIndex.builder().add(0).addNull().build();

        TableColumnar joined = new TableColumnarLeftOuterJoin(TableName.of("joined"), new TableDescription(""), left,
                right, rowIndex, amount);

        ColumnDouble amountView = (ColumnDouble) joined.get(amount);
        assertEquals(42.5d, amountView.get(0));
        assertTrue(amountView.isSet(0));
        assertEquals(0.0d, amountView.get(1));
        assertFalse(amountView.isSet(1));
    }
}
