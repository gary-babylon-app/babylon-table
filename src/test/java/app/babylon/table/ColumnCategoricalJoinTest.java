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
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class ColumnCategoricalJoinTest
{
    private enum E
    {
        A, B
    }

    @Test
    public void categoricalJoinShouldRespectNullRowsAndCategoryMapping()
    {
        ColumnCategorical.Builder<E> builder = ColumnCategorical.builder(ColumnName.of("e"), E.class);
        builder.add(E.A);
        builder.add(E.B);
        builder.add(E.A);
        ColumnCategorical<E> original = builder.build();

        ViewIndex rowIndex = ViewIndex.builder()
                .add(2)
                .addNull()
                .add(1)
                .build();

        ColumnCategorical<E> join = original.view(rowIndex);

        assertEquals(E.A, join.get(0));
        assertFalse(join.isSet(1));
        assertEquals(null, join.get(1));
        assertEquals(E.B, join.get(2));
        assertEquals(0, join.getCategoryCode(1));
        assertArrayEquals(new int[] {1, 2}, join.getCategoryCodes(null));
    }
}
