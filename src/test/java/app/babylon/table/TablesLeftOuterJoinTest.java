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

import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TablesLeftOuterJoinTest
{
    @Test
    public void leftOuterJoinShouldKeepAllLeftRowsAndAppendMatchedRightColumns()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName LEFT_VALUE = ColumnName.of("LeftValue");
        final ColumnName RIGHT_VALUE = ColumnName.of("RightValue");
        final ColumnName RIGHT_SCORE = ColumnName.of("RightScore");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, app.babylon.table.column.ColumnTypes.STRING);
        leftKey.add("A");
        leftKey.add("B");

        ColumnObject.Builder<String> leftValues = ColumnObject.builder(LEFT_VALUE,
                app.babylon.table.column.ColumnTypes.STRING);
        leftValues.add("left-a");
        leftValues.add("left-b");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build(), leftValues.build());

        ColumnObject.Builder<String> rightKeyBuilder = ColumnObject.builder(KEY,
                app.babylon.table.column.ColumnTypes.STRING);
        rightKeyBuilder.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(RIGHT_VALUE,
                app.babylon.table.column.ColumnTypes.STRING);
        rightValues.add("right-a");

        ColumnInt.Builder rightScores = ColumnInt.builder(RIGHT_SCORE);
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

        ColumnInt joinedRightScores = joined.getInt(RIGHT_SCORE);
        assertTrue(joinedRightScores.isSet(0));
        assertEquals(7, joinedRightScores.get(0));
        assertFalse(joinedRightScores.isSet(1));
    }

    @Test
    public void leftOuterJoinShouldSupportRepeatedObjectMatchesAndNullMisses()
    {
        final ColumnName KEY = ColumnName.of("Key");
        final ColumnName RIGHT_VALUE = ColumnName.of("RightValue");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(KEY, app.babylon.table.column.ColumnTypes.STRING);
        leftKey.add("A");
        leftKey.add("A");
        leftKey.add("B");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(KEY, app.babylon.table.column.ColumnTypes.STRING);
        rightKey.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(RIGHT_VALUE,
                app.babylon.table.column.ColumnTypes.STRING);
        rightValues.add("right-a");

        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), rightValues.build());

        TableColumnar joined = Tables.leftOuterJoin(left, right, KEY, KEY, RIGHT_VALUE);
        ColumnObject<String> values = joined.getString(RIGHT_VALUE);

        assertEquals("right-a", values.get(0));
        assertEquals("right-a", values.get(1));
        assertFalse(values.isSet(2));
        assertEquals(null, values.get(2));
    }
}
