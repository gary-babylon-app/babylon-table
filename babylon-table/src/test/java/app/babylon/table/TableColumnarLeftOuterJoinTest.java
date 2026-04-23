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
import app.babylon.table.column.ColumnTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TableColumnarLeftOuterJoinTest
{
    @Test
    public void joinTableShouldExposeLeftColumnsAndJoinAwareRightColumns()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName LEFT_VALUE = ColumnName.of("LeftValue");
        final ColumnName RIGHT_VALUE = ColumnName.of("RightValue");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        leftKey.add("A");
        leftKey.add("B");

        ColumnObject.Builder<String> leftValues = ColumnObject.builder(LEFT_VALUE, ColumnTypes.STRING);
        leftValues.add("left-a");
        leftValues.add("left-b");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build(), leftValues.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        rightKey.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(RIGHT_VALUE, ColumnTypes.STRING);
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

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        leftKey.add("A");
        leftKey.add("B");
        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
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

    @Test
    public void tablesLeftOuterJoinShouldKeepAllLeftRowsAndAppendMatchedRightColumns()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName LEFT_VALUE = ColumnName.of("LeftValue");
        final ColumnName RIGHT_VALUE = ColumnName.of("RightValue");
        final ColumnName RIGHT_SCORE = ColumnName.of("RightScore");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        leftKey.add("A");
        leftKey.add("B");

        ColumnObject.Builder<String> leftValues = ColumnObject.builder(LEFT_VALUE, ColumnTypes.STRING);
        leftValues.add("left-a");
        leftValues.add("left-b");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build(), leftValues.build());

        ColumnObject.Builder<String> rightKeyBuilder = ColumnObject.builder(KEY, ColumnTypes.STRING);
        rightKeyBuilder.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(RIGHT_VALUE, ColumnTypes.STRING);
        rightValues.add("right-a");

        app.babylon.table.column.ColumnInt.Builder rightScores = app.babylon.table.column.ColumnInt
                .builder(RIGHT_SCORE);
        rightScores.add(7);

        TableColumnar right = Tables.newTable(TableName.of("right"), rightKeyBuilder.build(), rightValues.build(),
                rightScores.build());

        TableColumnar joined = Tables.leftOuterJoin(left, right, KEY, KEY, RIGHT_VALUE, RIGHT_SCORE);

        assertEquals(2, joined.getRowCount());
        assertTrue(joined.contains(KEY));
        assertTrue(joined.contains(LEFT_VALUE));
        assertTrue(joined.contains(RIGHT_VALUE));
        assertTrue(joined.contains(RIGHT_SCORE));

        ColumnObject<String> joinedRightValues = joined.getString(RIGHT_VALUE);
        assertEquals("right-a", joinedRightValues.get(0));
        assertEquals(null, joinedRightValues.get(1));

        app.babylon.table.column.ColumnInt joinedRightScores = joined.getInt(RIGHT_SCORE);
        assertTrue(joinedRightScores.isSet(0));
        assertEquals(7, joinedRightScores.get(0));
        assertFalse(joinedRightScores.isSet(1));
    }

    @Test
    public void tablesLeftOuterJoinShouldSupportRepeatedObjectMatchesAndNullMisses()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName RIGHT_VALUE = ColumnName.of("RightValue");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        leftKey.add("A");
        leftKey.add("A");
        leftKey.add("B");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        rightKey.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(RIGHT_VALUE, ColumnTypes.STRING);
        rightValues.add("right-a");

        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), rightValues.build());

        TableColumnar joined = Tables.leftOuterJoin(left, right, KEY, KEY, RIGHT_VALUE);
        ColumnObject<String> values = joined.getString(RIGHT_VALUE);

        assertEquals("right-a", values.get(0));
        assertEquals("right-a", values.get(1));
        assertFalse(values.isSet(2));
        assertEquals(null, values.get(2));
    }

    @Test
    public void pruneShouldRemoveColumnsThatAreNoneSetOnALeftOuterJoin()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName EMPTY_RIGHT = ColumnName.of("EmptyRight");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        leftKey.add("A");
        leftKey.add("B");
        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(KEY, ColumnTypes.STRING);
        rightKey.add("Z");
        ColumnObject.Builder<String> emptyRight = ColumnObject.builder(EMPTY_RIGHT, ColumnTypes.STRING);
        emptyRight.add("x");
        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), emptyRight.build());

        TableColumnar joined = new TableColumnarLeftOuterJoin(TableName.of("joined"), new TableDescription(""), left,
                right, ViewIndex.builder().addNull().addNull().build(), EMPTY_RIGHT);

        TableColumnar pruned = joined.prune();

        assertEquals(2, joined.getColumnCount());
        assertEquals(1, pruned.getColumnCount());
        assertEquals(KEY, pruned.getColumnNames()[0]);
    }
}
