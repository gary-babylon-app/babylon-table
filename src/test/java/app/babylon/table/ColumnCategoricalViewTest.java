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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;


public class ColumnCategoricalViewTest
{
    private enum E
    {
        A, B
    }

    @Test
    public void categoryCode_usesViewRowMapping()
    {
        ColumnCategorical.Builder<E> builder = ColumnCategorical.builder(ColumnName.of("e"), E.class);
        builder.add(E.A);
        builder.add(E.B);
        builder.add(E.A);
        ColumnCategorical<E> original = builder.build();

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2);
        rowIndexBuilder.add(0);
        rowIndexBuilder.add(1);
        ViewIndex rowIndex = rowIndexBuilder.build();

        ColumnCategorical<E> view = (ColumnCategorical<E>)original.view(rowIndex);

        assertEquals(original.getCategoryCode(2), view.getCategoryCode(0));
        assertEquals(original.getCategoryCode(0), view.getCategoryCode(1));
        assertEquals(original.getCategoryCode(1), view.getCategoryCode(2));
        assertArrayEquals(new int[] {1, 2}, view.getCategoryCodes(null));
    }

    @Test
    public void dictionarySize_reflectsBackingDictionaryNotViewSubset()
    {
        ColumnCategorical.Builder<E> builder = ColumnCategorical.builder(ColumnName.of("e"), E.class);
        builder.add(E.A);
        builder.add(E.B);
        builder.add(E.A);
        ColumnCategorical<E> original = builder.build();

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(0);
        rowIndexBuilder.add(2);
        ViewIndex rowIndex = rowIndexBuilder.build();

        ColumnCategorical<E> view = (ColumnCategorical<E>)original.view(rowIndex);
        assertArrayEquals(new int[] {1}, view.getCategoryCodes(null));
    }
}
