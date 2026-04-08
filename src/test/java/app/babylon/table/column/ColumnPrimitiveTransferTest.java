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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnPrimitiveTransferTest
{
    @Test
    public void intBuilderTransfersValuesAndNullsToImmutable()
    {
        ColumnInt.Builder builder = ColumnInt.builder(ColumnName.of("i"));
        builder.add(7);
        builder.addNull();
        builder.add(-1);

        ColumnInt immutable = builder.build();

        assertEquals(3, immutable.size());
        assertEquals(7, immutable.get(0));
        assertTrue(immutable.isSet(0));
        assertFalse(immutable.isSet(1));
        assertEquals(-1, immutable.get(2));
        assertTrue(immutable.isSet(2));

        assertThrows(IllegalStateException.class, () -> builder.add(11));
    }

    @Test
    public void longBuilderTransfersValuesAndNullsToImmutable()
    {
        ColumnLong.Builder builder = ColumnLong.builder(ColumnName.of("l"));
        builder.add(42L);
        builder.addNull();
        builder.add(-9L);

        ColumnLong immutable = builder.build();

        assertEquals(3, immutable.size());
        assertEquals(42L, immutable.get(0));
        assertTrue(immutable.isSet(0));
        assertFalse(immutable.isSet(1));
        assertEquals(-9L, immutable.get(2));
        assertTrue(immutable.isSet(2));
        assertThrows(IllegalStateException.class, () -> builder.add(12L));

    }

    @Test
    public void doubleBuilderTransfersValuesAndNullsToImmutable()
    {
        ColumnDouble.Builder builder = ColumnDouble.builder(ColumnName.of("d"));
        builder.add(1.5);
        builder.addNull();
        builder.add(-3.25);

        ColumnDouble immutable = builder.build();

        assertEquals(3, immutable.size());
        assertEquals(1.5, immutable.get(0), 1e-12);
        assertTrue(immutable.isSet(0));
        assertFalse(immutable.isSet(1));
        assertEquals(-3.25, immutable.get(2), 1e-12);
        assertTrue(immutable.isSet(2));
        assertThrows(IllegalStateException.class, () -> builder.add(3.0));

    }

    @Test
    public void primitiveColumnsShouldSupportViews()
    {
        ColumnInt.Builder ints = ColumnInt.builder(ColumnName.of("i"));
        ints.add(7);
        ints.addNull();
        ints.add(-1);

        ColumnLong.Builder longs = ColumnLong.builder(ColumnName.of("l"));
        longs.add(42L);
        longs.addNull();
        longs.add(-9L);

        ColumnDouble.Builder doubles = ColumnDouble.builder(ColumnName.of("d"));
        doubles.add(1.5);
        doubles.addNull();
        doubles.add(-3.25);

        ViewIndex.Builder rowIndex = ViewIndex.builder();
        rowIndex.add(2);
        rowIndex.add(1);
        rowIndex.add(0);

        ColumnInt intView = (ColumnInt) ints.build().view(rowIndex.build());
        assertEquals(3, intView.size());
        assertEquals(-1, intView.get(0));
        assertFalse(intView.isSet(1));
        assertEquals(7, intView.get(2));

        rowIndex = ViewIndex.builder();
        rowIndex.add(2);
        rowIndex.add(1);
        rowIndex.add(0);
        ColumnLong longView = (ColumnLong) longs.build().view(rowIndex.build());
        assertEquals(3, longView.size());
        assertEquals(-9L, longView.get(0));
        assertFalse(longView.isSet(1));
        assertEquals(42L, longView.get(2));

        rowIndex = ViewIndex.builder();
        rowIndex.add(2);
        rowIndex.add(1);
        rowIndex.add(0);
        ColumnDouble doubleView = (ColumnDouble) doubles.build().view(rowIndex.build());
        assertEquals(3, doubleView.size());
        assertEquals(-3.25, doubleView.get(0), 1e-12);
        assertFalse(doubleView.isSet(1));
        assertEquals(1.5, doubleView.get(2), 1e-12);
    }
}
