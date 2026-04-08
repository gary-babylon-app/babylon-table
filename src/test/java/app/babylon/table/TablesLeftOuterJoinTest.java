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

import org.junit.jupiter.api.Test;

public class TablesLeftOuterJoinTest
{
    @Test
    public void leftOuterJoinShouldKeepAllLeftRowsAndAppendMatchedRightColumns()
    {
        ColumnName key = ColumnName.of("Key");
        ColumnName leftValue = ColumnName.of("LeftValue");
        ColumnName rightValue = ColumnName.of("RightValue");
        ColumnName rightScore = ColumnName.of("RightScore");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(key, String.class);
        leftKey.add("A");
        leftKey.add("B");

        ColumnObject.Builder<String> leftValues = ColumnObject.builder(leftValue, String.class);
        leftValues.add("left-a");
        leftValues.add("left-b");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build(), leftValues.build());

        ColumnObject.Builder<String> rightKeyBuilder = ColumnObject.builder(key, String.class);
        rightKeyBuilder.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(rightValue, String.class);
        rightValues.add("right-a");

        ColumnInt.Builder rightScores = ColumnInt.builder(rightScore);
        rightScores.add(7);

        TableColumnar right = Tables.newTable(TableName.of("right"), rightKeyBuilder.build(), rightValues.build(),
                rightScores.build());

        TableColumnar joined = Tables.leftOuterJoin(left, right, key, key, rightValue, rightScore);

        assertEquals(2, joined.getRowCount());
        assertTrue(joined.contains(key));
        assertTrue(joined.contains(leftValue));
        assertTrue(joined.contains(rightValue));
        assertTrue(joined.contains(rightScore));

        ColumnObject<String> joinedRightValues = joined.getString(rightValue);
        assertEquals("right-a", joinedRightValues.get(0));
        assertEquals(null, joinedRightValues.get(1));

        ColumnInt joinedRightScores = joined.getInt(rightScore);
        assertTrue(joinedRightScores.isSet(0));
        assertEquals(7, joinedRightScores.get(0));
        assertFalse(joinedRightScores.isSet(1));
    }

    @Test
    public void leftOuterJoinShouldSupportRepeatedObjectMatchesAndNullMisses()
    {
        ColumnName key = ColumnName.of("Key");
        ColumnName rightValue = ColumnName.of("RightValue");

        ColumnObject.Builder<String> leftKey = ColumnObject.builder(key, String.class);
        leftKey.add("A");
        leftKey.add("A");
        leftKey.add("B");

        TableColumnar left = Tables.newTable(TableName.of("left"), leftKey.build());

        ColumnObject.Builder<String> rightKey = ColumnObject.builder(key, String.class);
        rightKey.add("A");

        ColumnObject.Builder<String> rightValues = ColumnObject.builder(rightValue, String.class);
        rightValues.add("right-a");

        TableColumnar right = Tables.newTable(TableName.of("right"), rightKey.build(), rightValues.build());

        TableColumnar joined = Tables.leftOuterJoin(left, right, key, key, rightValue);
        ColumnObject<String> values = joined.getString(rightValue);

        assertEquals("right-a", values.get(0));
        assertEquals("right-a", values.get(1));
        assertFalse(values.isSet(2));
        assertEquals(null, values.get(2));
    }
}
