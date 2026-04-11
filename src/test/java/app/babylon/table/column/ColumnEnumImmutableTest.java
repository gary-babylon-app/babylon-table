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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnEnumImmutableTest
{
    private enum TestEnum
    {
        A, B, C
    }

    @Test
    public void mutableIsConstantShouldTrackEqualAndUnequalSequences()
    {
        final ColumnName SAME = ColumnName.of("SAME");
        final ColumnName MIXED = ColumnName.of("MIXED");
        final ColumnName NULLS = ColumnName.of("NULLS");
        ColumnCategorical.Builder<TestEnum> sameBuilder = ColumnCategorical.builder(SAME, TestEnum.class);
        sameBuilder.add(TestEnum.A);
        sameBuilder.add(TestEnum.A);
        sameBuilder.add(TestEnum.A);
        ColumnCategorical<TestEnum> same = sameBuilder.build();
        assertTrue(same.isConstant());

        ColumnCategorical.Builder<TestEnum> mixedBuilder = ColumnCategorical.builder(MIXED, TestEnum.class);
        mixedBuilder.add(TestEnum.A);
        mixedBuilder.add(TestEnum.B);
        ColumnCategorical<TestEnum> mixed = mixedBuilder.build();
        assertFalse(mixed.isConstant());

        ColumnCategorical.Builder<TestEnum> nullsBuilder = ColumnCategorical.builder(NULLS, TestEnum.class);
        nullsBuilder.add(null);
        nullsBuilder.add(null);
        ColumnCategorical<TestEnum> nulls = nullsBuilder.build();
        assertTrue(nulls.isConstant());
    }

    @Test
    public void immutableShouldPreserveMixedNullAndSetValues()
    {
        final ColumnName E = ColumnName.of("E");
        ColumnCategorical.Builder<TestEnum> mutable = ColumnCategorical.builder(E, TestEnum.class);
        mutable.add(TestEnum.A);
        mutable.add(null);
        mutable.add(TestEnum.C);

        ColumnCategorical<TestEnum> immutable = mutable.build();

        assertNotNull(immutable);
        assertEquals(3, immutable.size());
        assertTrue(immutable.isSet(0));
        assertFalse(immutable.isSet(1));
        assertTrue(immutable.isSet(2));
        assertEquals(TestEnum.A, immutable.get(0));
        assertNull(immutable.get(1));
        assertEquals(TestEnum.C, immutable.get(2));
    }

    @Test
    public void immutableShouldHandleAllNullValues()
    {
        final ColumnName E = ColumnName.of("E");
        ColumnCategorical.Builder<TestEnum> mutable = ColumnCategorical.builder(E, TestEnum.class);
        mutable.add(null);
        mutable.add(null);

        ColumnCategorical<TestEnum> immutable = mutable.build();

        assertNotNull(immutable);
        assertEquals(2, immutable.size());
        assertFalse(immutable.isSet(0));
        assertFalse(immutable.isSet(1));
        assertNull(immutable.get(0));
        assertNull(immutable.get(1));
    }
}
