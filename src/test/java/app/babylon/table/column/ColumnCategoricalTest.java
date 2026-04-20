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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import app.babylon.table.ViewIndex;
import app.babylon.table.column.type.TypeParsers;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.selection.Selection;

class ColumnCategoricalTest
{
    private enum E
    {
        A, B
    }

    private enum Sample
    {
        A, B
    }

    private static final Column.Type E_TYPE = Column.Type.of(E.class, TypeParsers.NULL);
    private static final Column.Type SAMPLE_TYPE = Column.Type.of(Sample.class, TypeParsers.NULL);

    @Test
    void categoricalViewsShouldRespectNullRowsAndCategoryMapping()
    {
        final ColumnName E_2 = ColumnName.of("E");
        ColumnCategorical.Builder<E> builder = ColumnCategorical.builder(E_2, E_TYPE);
        builder.add(E.A);
        builder.add(E.B);
        builder.add(E.A);
        ColumnCategorical<E> original = builder.build();

        ColumnCategorical<E> join = original.view(ViewIndex.builder().add(2).addNull().add(1).build());

        assertEquals(E.A, join.get(0));
        assertFalse(join.isSet(1));
        assertEquals(null, join.get(1));
        assertEquals(E.B, join.get(2));
        assertEquals(0, join.getCategoryCode(1));
        assertArrayEquals(new int[]
        {1, 2}, join.getCategoryCodes(null));
    }

    @Test
    void transformShouldApplyOncePerCategoryAndPreserveNulls()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S, ColumnTypes.STRING);
        builder.add("A");
        builder.add("a");
        builder.addNull();
        builder.add("B");
        builder.add("b");
        ColumnCategorical<String> column = builder.build();

        ColumnCategorical<String> transformed = column
                .transform(Transformer.of(String::toLowerCase, ColumnTypes.STRING));

        assertEquals(5, transformed.size());
        assertEquals("a", transformed.get(0));
        assertEquals("a", transformed.get(1));
        assertFalse(transformed.isSet(2));
        assertEquals("b", transformed.get(3));
        assertEquals("b", transformed.get(4));

        assertNotEquals(transformed.getCategoryCode(0), transformed.getCategoryCode(1));
        assertNotEquals(transformed.getCategoryCode(3), transformed.getCategoryCode(4));
        assertNotEquals(transformed.getCategoryCode(0), transformed.getCategoryCode(3));
        assertArrayEquals(new int[]
        {1, 2, 3, 4}, transformed.getCategoryCodes(null));
    }

    @Test
    void transformConstantShouldRemainConstant()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical<String> constant = ColumnCategorical.constant(S, "X", 4, ColumnTypes.STRING);

        ColumnCategorical<String> transformed = constant
                .transform(Transformer.of(String::toLowerCase, ColumnTypes.STRING));

        assertTrue(transformed.isConstant());
        assertEquals(4, transformed.size());
        assertArrayEquals(new int[]
        {1}, transformed.getCategoryCodes(null));
        assertEquals("x", transformed.get(0));
        assertEquals("x", transformed.get(3));
    }

    @Test
    void transformMixedResultTypesShouldUseObjectType()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S, ColumnTypes.STRING);
        builder.add("1");
        builder.add("x");
        ColumnCategorical<String> column = builder.build();
        Column.Type objectType = Column.Type.of(Object.class, app.babylon.table.column.type.TypeParsers.NULL);

        ColumnCategorical<Object> transformed = column
                .transform(Transformer.of(s -> "1".equals(s) ? Integer.valueOf(1) : s, objectType));

        assertEquals(Object.class, transformed.getType().getValueClass());
        assertEquals(Integer.valueOf(1), transformed.get(0));
        assertEquals("x", transformed.get(1));
    }

    @Test
    void transformViewShouldApplyOnlyForCodesUsedInView()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S, ColumnTypes.STRING);
        builder.add("A");
        builder.add("B");
        builder.add("C");
        ColumnCategorical<String> original = builder.build();

        ColumnCategorical<String> view = original.view(ViewIndex.builder().add(2).add(2).build());

        AtomicInteger calls = new AtomicInteger(0);
        ColumnCategorical<String> transformed = view.transform(Transformer.of(x -> {
            calls.incrementAndGet();
            return x.toLowerCase();
        }, ColumnTypes.STRING));

        assertEquals(1, calls.get());
        assertEquals(2, transformed.size());
        assertEquals("c", transformed.get(0));
        assertEquals("c", transformed.get(1));
        assertEquals(view.getCategoryCode(0), transformed.getCategoryCode(0));
        assertArrayEquals(new int[]
        {view.getCategoryCode(0)}, transformed.getCategoryCodes(null));
    }

    @Test
    void viewOnTransformShouldPreserveUsedCodesFromTheTransformedColumn()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S, ColumnTypes.STRING);
        builder.add("A");
        builder.add("a");
        builder.add("B");
        builder.add("b");
        ColumnCategorical<String> original = builder.build();

        ColumnCategorical<String> transformed = original
                .transform(Transformer.of(String::toLowerCase, ColumnTypes.STRING));
        ColumnCategorical<String> view = transformed.view(ViewIndex.builder().add(0).add(1).add(0).build());

        assertEquals(3, view.size());
        assertEquals("a", view.get(0));
        assertEquals("a", view.get(1));
        assertEquals("a", view.get(2));
        assertNotEquals(view.getCategoryCode(0), view.getCategoryCode(1));
        assertEquals(view.getCategoryCode(0), view.getCategoryCode(2));
        assertArrayEquals(new int[]
        {view.getCategoryCode(0), view.getCategoryCode(1)}, view.getCategoryCodes(null));
    }

    @Test
    void transformShouldUseTransformerColumnNameWhenProvided()
    {
        final ColumnName OLD_NAME = ColumnName.of("OLD_NAME");
        final ColumnName NEW_NAME = ColumnName.of("NEW_NAME");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(OLD_NAME, ColumnTypes.STRING);
        builder.add("A");
        builder.add("B");
        ColumnCategorical<String> column = builder.build();

        ColumnCategorical<String> transformed = column
                .transform(Transformer.of(String::toLowerCase, ColumnTypes.STRING, NEW_NAME));

        assertEquals(NEW_NAME, transformed.getName());
        assertEquals("a", transformed.get(0));
        assertEquals("b", transformed.get(1));
    }

    @Test
    void selectShouldUsePredicateAndTreatNullAsFalse()
    {
        final ColumnName CAT = ColumnName.of("CAT");
        ColumnCategorical.Builder<Sample> builder = ColumnCategorical.builder(CAT, SAMPLE_TYPE);
        builder.add(Sample.A);
        builder.addNull();
        builder.add(Sample.B);
        builder.add(Sample.A);

        ColumnCategorical<Sample> column = builder.build();
        Selection selection = column.select(v -> v == Sample.A);

        assertEquals(4, selection.size());
        assertTrue(selection.get(0));
        assertFalse(selection.get(1));
        assertFalse(selection.get(2));
        assertTrue(selection.get(3));
        assertEquals(2, selection.selected());
    }

    @Test
    void categoricalColumnsShouldGetAllAndUniquesUsingCategories()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S, ColumnTypes.STRING);
        builder.add("A");
        builder.addNull();
        builder.add("B");
        builder.add("A");
        ColumnCategorical<String> column = builder.build();

        assertIterableEquals(List.of("A", "B", "A"), column.getAll(new ArrayList<>()));
        assertEquals(Set.of("A", "B"), column.getUniques(null));
        assertFalse(column.getUniques(null).contains(null));
    }

    @Test
    void categoricalConstantShouldReturnRepeatedValuesForGetAllAndSingleValueForUniques()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical<String> constant = ColumnCategorical.constant(S, "X", 4, ColumnTypes.STRING);

        assertIterableEquals(List.of("X", "X", "X", "X"), constant.getAll(new ArrayList<>()));
        assertEquals(Set.of("X"), constant.getUniques(null));
    }

    @Test
    void categoricalMinAndMaxShouldUseDictionaryAndThrowOnNullConstant()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S, ColumnTypes.STRING);
        builder.add("B");
        builder.add("A");
        builder.add("C");
        builder.add("A");

        ColumnCategorical<String> column = builder.build();

        assertEquals("C", column.max());
        assertEquals("A", column.min());

        ColumnCategorical<String> nullConstant = ColumnCategorical.constant(S, null, 3, ColumnTypes.STRING);
        assertThrows(RuntimeException.class, nullConstant::max);
        assertThrows(RuntimeException.class, nullConstant::min);
    }

    @Test
    void categoricalBuilderShouldBuildParsedColumnFromDictionary()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("10.5");
        builder.add("20.0");
        builder.add("10.5");
        builder.add("bad");
        builder.addNull();

        ColumnCategorical<BigDecimal> column = (ColumnCategorical<BigDecimal>) builder.build(ColumnTypes.DECIMAL);

        assertEquals(ColumnTypes.DECIMAL, column.getType());
        assertEquals(0, new BigDecimal("10.5").compareTo(column.get(0)));
        assertEquals(0, new BigDecimal("20.0").compareTo(column.get(1)));
        assertEquals(0, new BigDecimal("10.5").compareTo(column.get(2)));
        assertFalse(column.isSet(3));
        assertFalse(column.isSet(4));
        assertArrayEquals(new int[]
        {1, 2}, column.getCategoryCodes(null));
    }

    @Test
    void categoricalBuilderShouldBuildCurrencyColumnFromDictionary()
    {
        final ColumnName CCY = ColumnName.of("Currency");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(CCY);
        builder.add("BRL");
        builder.add("MXN");
        builder.add("BRL");
        builder.add("PLN");

        ColumnCategorical<Currency> column = (ColumnCategorical<Currency>) builder.build(ColumnTypes.CURRENCY);

        assertEquals(ColumnTypes.CURRENCY, column.getType());
        assertEquals(Currency.getInstance("BRL"), column.get(0));
        assertEquals(Currency.getInstance("MXN"), column.get(1));
        assertEquals(Currency.getInstance("BRL"), column.get(2));
        assertEquals(Currency.getInstance("PLN"), column.get(3));
        assertArrayEquals(new int[]
        {1, 2, 3}, column.getCategoryCodes(null));
    }

    @Test
    void categoricalBuilderShouldKeepUnsetRowsWhenBuildingCurrencyColumn()
    {
        final ColumnName CCY = ColumnName.of("Currency");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(CCY);
        builder.add("BRL");
        builder.addNull();
        builder.add("bad");
        builder.add("MXN");

        ColumnCategorical<Currency> column = (ColumnCategorical<Currency>) builder.build(ColumnTypes.CURRENCY);

        assertEquals(Currency.getInstance("BRL"), column.get(0));
        assertFalse(column.isSet(1));
        assertFalse(column.isSet(2));
        assertEquals(Currency.getInstance("MXN"), column.get(3));
    }

    @Test
    void categoricalBuilderShouldNotBeConstantWhenCurrencyTransformLeavesMixedSetAndUnsetRows()
    {
        final ColumnName CCY = ColumnName.of("Currency");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(CCY, ColumnTypes.STRING);
        builder.add("BRL");
        builder.add("hello");
        builder.addNull();

        ColumnCategorical<Currency> column = (ColumnCategorical<Currency>) builder.build(ColumnTypes.CURRENCY);

        assertFalse(column.isConstant());
        assertFalse(column.isAllSet());
        assertFalse(column.isNoneSet());
        assertEquals(Currency.getInstance("BRL"), column.get(0));
        assertFalse(column.isSet(1));
        assertFalse(column.isSet(2));
    }

    @Test
    void categoricalBuilderShouldBuildConstantCurrencyColumn()
    {
        final ColumnName CCY = ColumnName.of("Currency");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(CCY, ColumnTypes.STRING);
        builder.add("brl");
        builder.add("BRL");
        builder.add("BRL");

        ColumnCategorical<Currency> column = (ColumnCategorical<Currency>) builder.build(ColumnTypes.CURRENCY);

        assertTrue(column.isConstant());
        assertEquals(Currency.getInstance("BRL"), column.get(0));
        assertEquals(Currency.getInstance("BRL"), column.get(2));
        assertArrayEquals(new int[]
        {1, 2}, column.getCategoryCodes(null));
    }

    @Test
    void categoricalViewUniquesShouldOnlyContainCategoriesUsedByTheView()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S, ColumnTypes.STRING);
        builder.add("A");
        builder.add("B");
        builder.add("C");
        builder.addNull();
        builder.add("A");
        ColumnCategorical<String> original = builder.build();

        ColumnCategorical<String> view = original.view(ViewIndex.builder().add(2).add(3).add(4).build());

        assertIterableEquals(List.of("C", "A"), view.getAll(new ArrayList<>()));
        assertEquals(Set.of("A", "C"), view.getUniques(null));
        assertTrue(view.getUniques(null).contains("A"));
        assertTrue(view.getUniques(null).contains("C"));
        assertFalse(view.getUniques(null).contains("B"));
        assertFalse(view.getUniques(null).contains(null));
    }
}
