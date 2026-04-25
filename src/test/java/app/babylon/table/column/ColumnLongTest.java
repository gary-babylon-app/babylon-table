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

public class ColumnLongTest
{
    @Test
    public void mutableColumnTracksNullsAndSupportsRegularValues()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnLong.Builder columnBuilder = (ColumnLong.Builder) ColumnLong.builder(VALUES);
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
        assertEquals(ColumnLong.TYPE, column.getType());
    }

    @Test
    public void mutableCopyPreservesValuesAndNullMarkers()
    {
        final ColumnName ORIGINAL = ColumnName.of("original");
        ColumnLong.Builder builder = (ColumnLong.Builder) ColumnLong.builder(ORIGINAL);
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
    public void builderTransfersValuesAndNullsToImmutable()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
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
    public void builderShouldCreateLongConstantWhenAllValuesAreSetAndEqual()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(7L);
        builder.add(7L);
        builder.add(7L);

        ColumnLong column = builder.build();

        assertTrue(column instanceof ColumnLongConstant);
    }

    @Test
    public void builderWithInitialSizeShouldBuildLongColumn()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L, 2);
        builder.add(5L);
        builder.add(6L);

        ColumnLong column = builder.build();

        assertEquals(2, column.size());
        assertEquals(5L, column.get(0));
        assertEquals(6L, column.get(1));
    }

    @Test
    public void builderShouldNotCreateLongConstantWhenNullsArePresent()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(7L);
        builder.addNull();
        builder.add(7L);

        ColumnLong column = builder.build();

        assertFalse(column instanceof ColumnLongConstant);
    }

    @Test
    public void copyWithNewNamePreservesData()
    {
        final ColumnName SOURCE = ColumnName.of("source");
        final ColumnName TARGET = ColumnName.of("target");
        ColumnLong.Builder builder = (ColumnLong.Builder) ColumnLong.builder(SOURCE);
        builder.add(7L);
        builder.addNull();
        ColumnLong original = builder.build();

        Column renamed = original.copy(TARGET);
        assertTrue(renamed instanceof ColumnLong);
        ColumnLong renamedLong = (ColumnLong) renamed;

        assertEquals(TARGET, renamedLong.getName());
        assertEquals(2, renamedLong.size());
        assertTrue(renamedLong.isSet(0));
        assertFalse(renamedLong.isSet(1));
    }

    @Test
    public void selectRowReturnsSingleRowConstantColumn()
    {
        final ColumnName SOURCE = ColumnName.of("source");
        ColumnLong.Builder sourceBuilder = (ColumnLong.Builder) ColumnLong.builder(SOURCE);
        sourceBuilder.add(11L);
        sourceBuilder.add(22L);
        ColumnLong source = sourceBuilder.build();

        Column single = source.selectRow(1);
        assertTrue(single instanceof ColumnLong);
        assertEquals(1, single.size());
        assertEquals(SOURCE, single.getName());
        assertEquals("22", single.toString(0));
        assertTrue(single.isSet(0));
    }

    @Test
    public void constantColumnWithSentinelValueRepresentsNulls()
    {
        final ColumnName CONSTANT_NULL = ColumnName.of("constantNull");
        ColumnLong constantNull = ColumnLongConstant.createNull(CONSTANT_NULL, 3);

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
        ColumnLong constant = new ColumnLongConstant(C, 7L, 10, true);
        ViewIndex.Builder indexBuilder = ViewIndex.builder();
        indexBuilder.add(8);
        indexBuilder.add(4);
        indexBuilder.add(0);

        ColumnLong view = (ColumnLong) constant.view(indexBuilder.build());

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
    public void longConstantAccessorsShouldExposeStoredValue()
    {
        final ColumnName C = ColumnName.of("c");
        ColumnLongConstant constant = new ColumnLongConstant(C, 7L, 4, true);

        assertEquals(C, constant.getName());
        assertEquals(4, constant.size());
        assertEquals(7L, constant.getValue());
        assertEquals(7L, constant.get(0));
        assertTrue(constant.isSet(0));
        assertTrue(constant.isAllSet());
        assertFalse(constant.isNoneSet());
        assertTrue(constant.isConstant());

        long[] target = new long[6];
        long[] values = constant.toArray(target);
        assertTrue(values == target);
        assertEquals(7L, values[0]);
        assertEquals(7L, values[1]);
        assertEquals(7L, values[2]);
        assertEquals(7L, values[3]);
    }

    @Test
    public void longColumnsShouldExposeMinAndMax()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong column = ColumnLong.builder(L).add(42L).addNull().add(-9L).add(7L).build();

        assertEquals(42L, column.max());
        assertEquals(-9L, column.min());

        ColumnLong nullConstant = ColumnLongConstant.createNull(L, 2);
        assertThrows(RuntimeException.class, nullConstant::max);
        assertThrows(RuntimeException.class, nullConstant::min);
    }

    @Test
    public void longConstantViewShouldMaterializeNullRowsWhenRowIndexContainsGaps()
    {
        final ColumnName C = ColumnName.of("c");
        ColumnLong constant = new ColumnLongConstant(C, 7L, 10, true);

        ViewIndex viewIndex = ViewIndex.builder().add(3).addNull().add(1).build();
        ColumnLong view = (ColumnLong) constant.view(viewIndex);

        assertEquals(3, view.size());
        assertEquals(7L, view.get(0));
        assertTrue(view.isSet(0));
        assertFalse(view.isSet(1));
        assertEquals(7L, view.get(2));
        assertFalse(view.isConstant());
        assertFalse(view.isAllSet());
        assertFalse(view.isNoneSet());
    }

    @Test
    public void viewShouldTransferValuesAndNulls()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(42L);
        builder.addNull();
        builder.add(-9L);

        ViewIndex.Builder rowIndex = ViewIndex.builder();
        rowIndex.add(2);
        rowIndex.add(1);
        rowIndex.add(0);

        ColumnLong view = (ColumnLong) builder.build().view(rowIndex.build());

        assertEquals(3, view.size());
        assertEquals(-9L, view.get(0));
        assertFalse(view.isSet(1));
        assertEquals(42L, view.get(2));
    }

    @Test
    public void baseArrayShouldExposeCompareAndToArray()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(42L);
        builder.addNull();
        builder.add(-9L);
        ColumnLong column = builder.build();

        long[] target = new long[5];
        long[] values = column.toArray(target);

        assertTrue(values == target);
        assertEquals(42L, values[0]);
        assertEquals(0L, values[1]);
        assertEquals(-9L, values[2]);
        assertTrue(column.compare(0, 0) == 0);
        assertTrue(column.compare(2, 0) < 0);
        assertTrue(column.compare(0, 2) > 0);
        assertTrue(column.compare(1, 0) < 0);
        assertTrue(column.compare(0, 1) > 0);
        assertTrue(column.compare(1, 1) == 0);
    }

    @Test
    public void viewShouldExposeArrayCompareAndFlags()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(10L);
        builder.addNull();
        builder.add(30L);
        builder.add(40L);
        ColumnLong original = builder.build();

        ViewIndex index = ViewIndex.builder().add(2).addNull().add(0).build();
        ColumnLong view = (ColumnLong) original.view(index);

        long[] values = view.toArray(null);
        assertEquals(3, values.length);
        assertEquals(30L, values[0]);
        assertEquals(0L, values[1]);
        assertEquals(10L, values[2]);
        assertFalse(view.isAllSet());
        assertFalse(view.isNoneSet());
        assertFalse(view.isConstant());
        assertTrue(view.compare(2, 0) < 0);
        assertTrue(view.compare(1, 0) < 0);
    }

    @Test
    public void viewShouldDetectConstantAllSetAndNoneSetCases()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(7L);
        builder.add(7L);
        builder.add(7L);
        ColumnLong original = builder.build();

        ColumnLong constantView = (ColumnLong) original.view(ViewIndex.builder().add(2).add(1).add(0).build());
        long[] target = new long[4];
        long[] values = constantView.toArray(target);

        assertTrue(values == target);
        assertEquals(7L, values[0]);
        assertEquals(7L, values[1]);
        assertEquals(7L, values[2]);
        assertTrue(constantView.isConstant());
        assertTrue(constantView.isAllSet());
        assertFalse(constantView.isNoneSet());

        ColumnLong noneSetView = (ColumnLong) ColumnLongConstant.createNull(L, 3)
                .view(ViewIndex.builder().add(2).add(1).add(0).build());
        assertTrue(noneSetView.isConstant());
        assertFalse(noneSetView.isAllSet());
        assertTrue(noneSetView.isNoneSet());
    }

    @Test
    public void selectShouldUseLongPredicateAndTreatNullAsFalse()
    {
        final ColumnName L = ColumnName.of("L");
        ColumnLong.Builder builder = ColumnLong.builder(L);
        builder.add(2L);
        builder.addNull();
        builder.add(5L);

        ColumnLong column = builder.build();
        Selection selection = column.select(v -> v >= 5L);

        assertEquals(3, selection.size());
        assertFalse(selection.get(0));
        assertFalse(selection.get(1));
        assertTrue(selection.get(2));
        assertEquals(1, selection.selected());
    }
}
