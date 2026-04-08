/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import app.babylon.table.ViewIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnObjectJoinTest
{
    @Test
    public void objectJoinShouldReturnNullAndUnsetForNullJoinRows()
    {
        ColumnObject.Builder<String> original = ColumnObject.builder(ColumnName.of("Test"), String.class);
        original.add("abc1");
        original.add("abc2");
        original.add("abc3");

        ViewIndex rowIndex = ViewIndex.builder().add(1).addNull().add(1).build();

        ColumnObject<String> join = original.build().view(rowIndex);

        assertEquals(3, join.size());
        assertEquals("abc2", join.get(0));
        assertTrue(join.isSet(0));
        assertNull(join.get(1));
        assertFalse(join.isSet(1));
        assertEquals("abc2", join.get(2));
        assertFalse(join.isConstant());
    }
}
