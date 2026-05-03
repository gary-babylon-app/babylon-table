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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.ViewIndex;
import app.babylon.table.selection.RowPredicate;
import app.babylon.table.selection.Selection;

class ColumnBooleanTest
{
    @Test
    void builderTracksValuesAndNulls()
    {
        ColumnName values = ColumnName.of("values");
        ColumnBoolean.Builder builder = ColumnBoolean.builder(values);
        builder.add(true);
        builder.addNull();
        builder.add(false);

        ColumnBoolean column = builder.build();

        assertEquals(3, column.size());
        assertEquals(ColumnBoolean.TYPE, column.getType());
        assertTrue(column.get(0));
        assertTrue(column.isSet(0));
        assertFalse(column.isSet(1));
        assertFalse(column.get(2));
        assertTrue(column.isSet(2));
        assertEquals("true", column.toString(0));
        assertEquals("", column.toString(1));
        assertEquals("false", column.toString(2));
        assertThrows(IllegalStateException.class, () -> builder.add(true));
    }

    @Test
    void builderParsesCharacterSlices()
    {
        ColumnName values = ColumnName.of("values");
        ColumnBoolean column = ColumnBoolean.builder(values).add("true", 0, 4).add("false", 0, 5).add("bad", 0, 3)
                .build();

        assertTrue(column.get(0));
        assertFalse(column.get(1));
        assertFalse(column.isSet(2));
    }

    @Test
    void copySelectRowViewAndArrayPreserveState()
    {
        ColumnName values = ColumnName.of("values");
        ColumnBoolean original = ColumnBoolean.builder(values).add(true).addNull().add(false).build();

        ColumnBoolean copy = original.copy(ColumnName.of("copy"));
        ColumnBoolean single = (ColumnBoolean) original.selectRow(0);
        ColumnBoolean view = (ColumnBoolean) original.view(ViewIndex.builder().add(2).addNull().add(0).build());
        boolean[] array = view.toArray(new boolean[4]);

        assertEquals(ColumnName.of("copy"), copy.getName());
        assertTrue(copy.get(0));
        assertFalse(copy.isSet(1));
        assertEquals(1, single.size());
        assertTrue(single.get(0));
        assertFalse(view.get(0));
        assertFalse(view.isSet(1));
        assertTrue(view.get(2));
        assertFalse(array[0]);
        assertFalse(array[1]);
        assertTrue(array[2]);
    }

    @Test
    void constantColumnExposesFlags()
    {
        ColumnName values = ColumnName.of("values");
        ColumnBoolean constant = ColumnBoolean.builder(values).add(true).add(true).build();

        assertTrue(constant instanceof ColumnBooleanConstant);
        assertTrue(constant.isConstant());
        assertTrue(constant.isAllSet());
        assertFalse(constant.isNoneSet());
        assertTrue(constant.get(0));
    }

    @Test
    void predicateUsesTypedBooleanComparisons()
    {
        ColumnName values = ColumnName.of("values");
        ColumnBoolean column = ColumnBoolean.builder(values).add(true).addNull().add(false).build();
        RowPredicate trueRows = column.predicate(Column.Operator.EQUAL, "true");
        RowPredicate falseRows = column.predicate(Column.Operator.IN, "false");

        assertTrue(trueRows.test(0));
        assertFalse(trueRows.test(1));
        assertFalse(trueRows.test(2));
        assertFalse(falseRows.test(0));
        assertFalse(falseRows.test(1));
        assertTrue(falseRows.test(2));
    }

    @Test
    void selectTreatsNullAsFalse()
    {
        ColumnName values = ColumnName.of("values");
        ColumnBoolean column = ColumnBoolean.builder(values).add(true).addNull().add(false).build();

        Selection selection = column.select(Boolean::booleanValue);

        assertEquals(3, selection.size());
        assertTrue(selection.get(0));
        assertFalse(selection.get(1));
        assertFalse(selection.get(2));
        assertEquals(1, selection.selected());
    }
}
