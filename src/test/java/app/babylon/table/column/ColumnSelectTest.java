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

import app.babylon.table.selection.Selection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnSelectTest
{
    private enum Sample
    {
        A, B
    }

    @Test
    public void select_onColumnObject_usesPredicateAndTreatsNullAsFalse()
    {
        final ColumnName OBJ = ColumnName.of("OBJ");
        ColumnObject.Builder<String> builder = ColumnObject.builder(OBJ, String.class);
        builder.add("a");
        builder.addNull();
        builder.add("bb");

        ColumnObject<String> column = builder.build();
        Selection selection = column.select(s -> s.length() == 1);

        assertSelection(selection, true, false, false);
    }

    @Test
    public void select_onColumnCategorical_usesPredicateAndTreatsNullAsFalse()
    {
        final ColumnName CAT = ColumnName.of("CAT");
        ColumnCategorical.Builder<Sample> builder = ColumnCategorical.builder(CAT, Sample.class);
        builder.add(Sample.A);
        builder.addNull();
        builder.add(Sample.B);
        builder.add(Sample.A);

        ColumnCategorical<Sample> column = builder.build();
        Selection selection = column.select(v -> v == Sample.A);

        assertSelection(selection, true, false, false, true);
    }

    @Test
    public void select_onColumnInt_usesIntPredicateAndTreatsNullAsFalse()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder builder = ColumnInt.builder(I);
        builder.add(1);
        builder.addNull();
        builder.add(3);

        ColumnInt column = builder.build();
        Selection selection = column.select(v -> v > 1);

        assertSelection(selection, false, false, true);
    }

    @Test
    public void select_onColumnLong_usesLongPredicateAndTreatsNullAsFalse()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(2L);
        builder.addNull();
        builder.add(5L);

        ColumnLong column = builder.build();
        Selection selection = column.select(v -> v >= 5L);

        assertSelection(selection, false, false, true);
    }

    @Test
    public void select_onColumnDouble_usesDoublePredicateAndTreatsNullAsFalse()
    {
        final ColumnName D = ColumnName.of("D");
        ColumnDouble.Builder builder = ColumnDouble.builder(D);
        builder.add(1.25d);
        builder.addNull();
        builder.add(2.50d);

        ColumnDouble column = builder.build();
        Selection selection = column.select(v -> v > 2.0d);

        assertSelection(selection, false, false, true);
    }

    private static void assertSelection(Selection selection, boolean... expected)
    {
        assertEquals(expected.length, selection.size());
        int selected = 0;
        for (int i = 0; i < expected.length; ++i)
        {
            if (expected[i])
            {
                assertTrue(selection.get(i));
                selected++;
            }
            else
            {
                assertFalse(selection.get(i));
            }
        }
        assertEquals(selected, selection.selected());
    }
}
