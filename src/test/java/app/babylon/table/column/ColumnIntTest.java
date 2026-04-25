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
import app.babylon.table.selection.Selection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnIntTest
{
    @Test
    public void mutableColumnTracksNullsAndSupportsRegularValues()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt.Builder columnBuilder = (ColumnInt.Builder) ColumnInt.builder(VALUES);
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
        assertEquals(ColumnInt.TYPE, column.getType());
    }

    @Test
    public void mutableCopyPreservesValuesAndNullMarkers()
    {
        final ColumnName ORIGINAL = ColumnName.of("original");
        ColumnInt.Builder builder = (ColumnInt.Builder) ColumnInt.builder(ORIGINAL);
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
    public void builderTransfersValuesAndNullsToImmutable()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder builder = ColumnInt.builder(I);
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
    public void builderShouldCreateIntConstantWhenAllValuesAreSetAndEqual()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder builder = ColumnInt.builder(I);
        builder.add(7);
        builder.add(7);
        builder.add(7);

        ColumnInt column = builder.build();

        assertTrue(column instanceof ColumnIntConstant);
    }

    @Test
    public void builderShouldNotCreateIntConstantWhenNullsArePresent()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder builder = ColumnInt.builder(I);
        builder.add(7);
        builder.addNull();
        builder.add(7);

        ColumnInt column = builder.build();

        assertFalse(column instanceof ColumnIntConstant);
    }

    @Test
    public void copyWithNewNamePreservesData()
    {
        final ColumnName SOURCE = ColumnName.of("source");
        final ColumnName TARGET = ColumnName.of("target");
        ColumnInt.Builder builder = (ColumnInt.Builder) ColumnInt.builder(SOURCE);
        builder.add(7);
        builder.addNull();
        ColumnInt original = builder.build();

        Column renamed = original.copy(TARGET);
        assertTrue(renamed instanceof ColumnInt);
        ColumnInt renamedInt = (ColumnInt) renamed;

        assertEquals(TARGET, renamedInt.getName());
        assertEquals(2, renamedInt.size());
        assertTrue(renamedInt.isSet(0));
        assertFalse(renamedInt.isSet(1));
    }

    @Test
    public void selectRowReturnsSingleRowConstantColumn()
    {
        final ColumnName SOURCE = ColumnName.of("source");
        ColumnInt.Builder sourceBuilder = (ColumnInt.Builder) ColumnInt.builder(SOURCE);
        sourceBuilder.add(11);
        sourceBuilder.add(22);
        ColumnInt source = sourceBuilder.build();

        Column single = source.selectRow(1);
        assertTrue(single instanceof ColumnInt);
        assertEquals(1, single.size());
        assertEquals(SOURCE, single.getName());
        assertEquals("22", single.toString(0));
        assertTrue(single.isSet(0));
    }

    @Test
    public void constantColumnWithSentinelValueRepresentsNulls()
    {
        final ColumnName CONSTANT_NULL = ColumnName.of("constantNull");
        ColumnInt constantNull = ColumnIntConstant.createNull(CONSTANT_NULL, 3);

        assertEquals(3, constantNull.size());
        assertFalse(constantNull.isSet(0));
        assertFalse(constantNull.isSet(1));
        assertFalse(constantNull.isSet(2));
        assertEquals("", constantNull.toString(0));
    }

    @Test
    public void constantViewReturnsResizedConstant()
    {
        final ColumnName C = ColumnName.of("c");
        ColumnInt constant = new ColumnIntConstant(C, 7, 10, true);
        ViewIndex.Builder indexBuilder = ViewIndex.builder();
        indexBuilder.add(9);
        indexBuilder.add(3);
        indexBuilder.add(1);

        ColumnInt view = (ColumnInt) constant.view(indexBuilder.build());

        assertEquals(3, view.size());
        assertEquals(C, view.getName());
        assertTrue(view.isSet(0));
        assertTrue(view.isSet(1));
        assertTrue(view.isSet(2));
        assertEquals("7", view.toString(0));
        assertEquals("7", view.toString(1));
        assertEquals("7", view.toString(2));
    }

    @Test
    public void intConstantFactoryAndAccessorsShouldExposeStoredValue()
    {
        final ColumnName C = ColumnName.of("c");
        ColumnIntConstant constant = ColumnIntConstant.of(C, 7, 4);

        assertEquals(C, constant.getName());
        assertEquals(4, constant.getSize());
        assertEquals(4, constant.size());
        assertEquals(7, constant.getValue());
        assertEquals(7, constant.get(0));
        assertTrue(constant.isSet(0));
        assertTrue(constant.isAllSet());
        assertFalse(constant.isNoneSet());
        assertTrue(constant.isConstant());

        int[] target = new int[6];
        int[] values = constant.toArray(target);
        assertTrue(values == target);
        assertEquals(7, values[0]);
        assertEquals(7, values[1]);
        assertEquals(7, values[2]);
        assertEquals(7, values[3]);
    }

    @Test
    public void intColumnsShouldExposeMinAndMax()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt column = ColumnInt.builder(I).add(7).addNull().add(-3).add(10).build();

        assertEquals(10, column.max());
        assertEquals(-3, column.min());

        ColumnInt nullConstant = ColumnIntConstant.createNull(I, 2);
        assertThrows(RuntimeException.class, nullConstant::max);
        assertThrows(RuntimeException.class, nullConstant::min);
    }

    @Test
    public void intConstantViewShouldMaterializeNullRowsWhenRowIndexContainsGaps()
    {
        final ColumnName C = ColumnName.of("c");
        ColumnInt constant = new ColumnIntConstant(C, 7, 10, true);

        ViewIndex viewIndex = ViewIndex.builder().add(3).addNull().add(1).build();
        ColumnInt view = (ColumnInt) constant.view(viewIndex);

        assertEquals(3, view.size());
        assertEquals(7, view.get(0));
        assertTrue(view.isSet(0));
        assertFalse(view.isSet(1));
        assertEquals(7, view.get(2));
        assertFalse(view.isConstant());
        assertFalse(view.isAllSet());
        assertFalse(view.isNoneSet());
    }

    @Test
    public void viewShouldExposeArrayAndSetState()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt.Builder builder = (ColumnInt.Builder) ColumnInt.builder(VALUES);
        builder.add(10);
        builder.addNull();
        builder.add(30);
        builder.add(40);
        ColumnInt original = builder.build();

        ViewIndex.Builder indexBuilder = ViewIndex.builder();
        indexBuilder.add(2);
        indexBuilder.addNull();
        indexBuilder.add(0);

        ColumnInt view = (ColumnInt) original.view(indexBuilder.build());

        int[] values = view.toArray(null);
        assertEquals(3, values.length);
        assertEquals(30, values[0]);
        assertEquals(0, values[1]);
        assertEquals(10, values[2]);
        assertFalse(view.isAllSet());
        assertFalse(view.isNoneSet());
        assertFalse(view.isConstant());
    }

    @Test
    public void viewShouldTransferValuesAndNulls()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder builder = ColumnInt.builder(I);
        builder.add(7);
        builder.addNull();
        builder.add(-1);

        ViewIndex.Builder rowIndex = ViewIndex.builder();
        rowIndex.add(2);
        rowIndex.add(1);
        rowIndex.add(0);

        ColumnInt view = (ColumnInt) builder.build().view(rowIndex.build());

        assertEquals(3, view.size());
        assertEquals(-1, view.get(0));
        assertFalse(view.isSet(1));
        assertEquals(7, view.get(2));
    }

    @Test
    public void baseArrayShouldExposeCompareAndToArray()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder builder = ColumnInt.builder(I);
        builder.add(42);
        builder.addNull();
        builder.add(-9);
        ColumnInt column = builder.build();

        int[] target = new int[5];
        int[] values = column.toArray(target);

        assertTrue(values == target);
        assertEquals(42, values[0]);
        assertEquals(0, values[1]);
        assertEquals(-9, values[2]);
        assertTrue(column.compare(0, 0) == 0);
        assertTrue(column.compare(2, 0) < 0);
        assertTrue(column.compare(0, 2) > 0);
        assertTrue(column.compare(1, 0) < 0);
        assertTrue(column.compare(0, 1) > 0);
        assertTrue(column.compare(1, 1) == 0);
    }

    @Test
    public void viewShouldExposeCompareAsWellAsArrayAndFlags()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt.Builder builder = (ColumnInt.Builder) ColumnInt.builder(VALUES);
        builder.add(10);
        builder.addNull();
        builder.add(30);
        builder.add(40);
        ColumnInt original = builder.build();

        ViewIndex index = ViewIndex.builder().add(2).addNull().add(0).build();
        ColumnInt view = (ColumnInt) original.view(index);

        int[] values = view.toArray(null);
        assertEquals(3, values.length);
        assertEquals(30, values[0]);
        assertEquals(0, values[1]);
        assertEquals(10, values[2]);
        assertFalse(view.isAllSet());
        assertFalse(view.isNoneSet());
        assertFalse(view.isConstant());
        assertTrue(view.compare(2, 0) < 0);
        assertTrue(view.compare(1, 0) < 0);
    }

    @Test
    public void selectShouldUseIntPredicateAndTreatNullAsFalse()
    {
        final ColumnName I = ColumnName.of("I");
        ColumnInt.Builder builder = ColumnInt.builder(I);
        builder.add(1);
        builder.addNull();
        builder.add(3);

        ColumnInt column = builder.build();
        Selection selection = column.select(v -> v > 1);

        assertEquals(3, selection.size());
        assertFalse(selection.get(0));
        assertFalse(selection.get(1));
        assertTrue(selection.get(2));
        assertEquals(1, selection.selected());
    }

    @Test
    public void viewShouldDetectConstantAndSetFlags()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt.Builder builder = (ColumnInt.Builder) ColumnInt.builder(VALUES);
        builder.add(7);
        builder.add(7);
        builder.add(7);
        ColumnInt original = builder.build();

        ViewIndex.Builder indexBuilder = ViewIndex.builder();
        indexBuilder.add(2);
        indexBuilder.add(1);
        indexBuilder.add(0);

        ColumnInt view = (ColumnInt) original.view(indexBuilder.build());
        int[] target = new int[5];
        int[] values = view.toArray(target);

        assertTrue(values == target);
        assertEquals(7, values[0]);
        assertEquals(7, values[1]);
        assertEquals(7, values[2]);
        assertTrue(view.isConstant());
        assertTrue(view.isAllSet());
        assertFalse(view.isNoneSet());
    }
}
