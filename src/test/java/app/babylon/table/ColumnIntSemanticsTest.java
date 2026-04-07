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


public class ColumnIntSemanticsTest
{
    @Test
    public void mutableColumnTracksNullsAndSupportsRegularValues()
    {
        ColumnInt.Builder columnBuilder = (ColumnInt.Builder)ColumnInt.builder(ColumnName.of("values"));
        columnBuilder.add(42);
        columnBuilder.addNull();
        columnBuilder.add(-123);

        ColumnInt column = columnBuilder.build();
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
        ColumnInt.Builder builder = (ColumnInt.Builder)ColumnInt.builder(ColumnName.of("original"));
        builder.add(10);
        builder.addNull();
        builder.add(-3);
        ColumnInt original = builder.build();

        ColumnInt typedCopy = original.copy(original.getName());

        assertEquals(original.size(), typedCopy.size());
        assertEquals(original.getName(), typedCopy.getName());
        assertTrue(typedCopy.isSet(0));
        assertFalse(typedCopy.isSet(1));
        assertTrue(typedCopy.isSet(2));
        assertEquals("10", typedCopy.toString(0));
        assertEquals("", typedCopy.toString(1));
        assertEquals("-3", typedCopy.toString(2));
    }

    @Test
    public void copyWithNewNamePreservesData()
    {
        ColumnInt.Builder builder = (ColumnInt.Builder)ColumnInt.builder(ColumnName.of("source"));
        builder.add(7);
        builder.addNull();
        ColumnInt original = builder.build();

        Column renamed = original.copy(ColumnName.of("target"));
        assertTrue(renamed instanceof ColumnInt);
        ColumnInt renamedInt = (ColumnInt)renamed;

        assertEquals(ColumnName.of("target"), renamedInt.getName());
        assertEquals(2, renamedInt.size());
        assertTrue(renamedInt.isSet(0));
        assertFalse(renamedInt.isSet(1));
    }

    @Test
    public void getAsColumnReturnsSingleRowConstantColumn()
    {
        ColumnInt.Builder sourceBuilder = (ColumnInt.Builder)ColumnInt.builder(ColumnName.of("source"));
        sourceBuilder.add(11);
        sourceBuilder.add(22);
        ColumnInt source = sourceBuilder.build();

        Column single = source.getAsColumn(1);
        assertTrue(single instanceof ColumnInt);
        assertEquals(1, single.size());
        assertEquals(ColumnName.of("source"), single.getName());
        assertEquals("22", single.toString(0));
        assertTrue(single.isSet(0));
    }

    @Test
    public void constantColumnWithSentinelValueRepresentsNulls()
    {
        ColumnInt constantNull = new ColumnIntConstant(ColumnName.of("constantNull"), 0, 3, false);

        assertEquals(3, constantNull.size());
        assertFalse(constantNull.isSet(0));
        assertFalse(constantNull.isSet(1));
        assertFalse(constantNull.isSet(2));
        assertEquals("", constantNull.toString(0));
    }

    @Test
    public void constantViewReturnsResizedConstant()
    {
        ColumnInt constant = new ColumnIntConstant(ColumnName.of("c"), 7, 10, true);
        ViewIndex.Builder indexBuilder = ViewIndex.builder();
        indexBuilder.add(9);
        indexBuilder.add(3);
        indexBuilder.add(1);

        ColumnInt view = (ColumnInt)constant.view(indexBuilder.build());

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
