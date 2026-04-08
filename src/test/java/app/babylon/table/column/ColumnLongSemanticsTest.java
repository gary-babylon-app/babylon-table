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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnLongSemanticsTest
{
    @Test
    public void mutableColumnTracksNullsAndSupportsRegularValues()
    {
        ColumnLong.Builder columnBuilder = (ColumnLong.Builder) ColumnLong.builder(ColumnName.of("values"));
        columnBuilder.add(42L);
        columnBuilder.addNull();
        columnBuilder.add(-123L);
        ColumnLong column = columnBuilder.build();

        assertEquals(3, column.size());
        assertTrue(column.isSet(0), "regular values must remain set");
        assertFalse(column.isSet(1), "addNull should create an unset entry");
        assertTrue(column.isSet(2), "regular values after null should remain set");
        assertEquals("42", column.toString(0));
        assertEquals("", column.toString(1));
        assertEquals("-123", column.toString(2));
    }

    @Test
    public void mutableCopyPreservesValuesAndNullMarkers()
    {
        ColumnLong.Builder builder = (ColumnLong.Builder) ColumnLong.builder(ColumnName.of("original"));
        builder.add(10L);
        builder.addNull();
        builder.add(-3L);
        ColumnLong original = builder.build();

        ColumnLong copy = original.copy(original.getName());

        assertEquals(original.size(), copy.size());
        assertEquals(original.getName(), copy.getName());
        assertTrue(copy.isSet(0));
        assertFalse(copy.isSet(1));
        assertTrue(copy.isSet(2));
        assertEquals("10", copy.toString(0));
        assertEquals("", copy.toString(1));
        assertEquals("-3", copy.toString(2));
    }

    @Test
    public void copyWithNewNamePreservesData()
    {
        ColumnLong.Builder builder = (ColumnLong.Builder) ColumnLong.builder(ColumnName.of("source"));
        builder.add(7L);
        builder.addNull();
        ColumnLong original = builder.build();

        Column renamed = original.copy(ColumnName.of("target"));
        assertTrue(renamed instanceof ColumnLong);
        ColumnLong renamedLong = (ColumnLong) renamed;

        assertEquals(ColumnName.of("target"), renamedLong.getName());
        assertEquals(2, renamedLong.size());
        assertTrue(renamedLong.isSet(0));
        assertFalse(renamedLong.isSet(1));
    }

    @Test
    public void getAsColumnReturnsSingleRowConstantColumn()
    {
        ColumnLong.Builder sourceBuilder = (ColumnLong.Builder) ColumnLong.builder(ColumnName.of("source"));
        sourceBuilder.add(11L);
        sourceBuilder.add(22L);
        ColumnLong source = sourceBuilder.build();

        Column single = source.getAsColumn(1);
        assertTrue(single instanceof ColumnLong);
        assertEquals(1, single.size());
        assertEquals(ColumnName.of("source"), single.getName());
        assertEquals("22", single.toString(0));
        assertTrue(single.isSet(0));
    }

    @Test
    public void constantColumnWithSentinelValueRepresentsNulls()
    {
        ColumnLong constantNull = new ColumnLongConstant(ColumnName.of("constantNull"), 0L, 3, false);

        assertEquals(3, constantNull.size());
        assertFalse(constantNull.isSet(0));
        assertFalse(constantNull.isSet(1));
        assertFalse(constantNull.isSet(2));
        assertEquals("", constantNull.toString(0));
    }

    @Test
    public void constantViewReturnsResizedConstant()
    {
        ColumnLong constant = new ColumnLongConstant(ColumnName.of("c"), 7L, 10, true);
        ViewIndex.Builder indexBuilder = ViewIndex.builder();
        indexBuilder.add(8);
        indexBuilder.add(4);
        indexBuilder.add(0);

        ColumnLong view = (ColumnLong) constant.view(indexBuilder.build());

        assertEquals(3, view.size());
        assertEquals(ColumnName.of("c"), view.getName());
        assertTrue(view.isSet(0));
        assertTrue(view.isSet(1));
        assertTrue(view.isSet(2));
        assertEquals("7", view.toString(0));
        assertEquals("7", view.toString(1));
        assertEquals("7", view.toString(2));
    }
}
