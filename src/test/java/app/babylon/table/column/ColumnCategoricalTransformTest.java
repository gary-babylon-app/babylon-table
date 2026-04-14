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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import app.babylon.table.ViewIndex;

class ColumnCategoricalTransformTest
{
    @Test
    void transformShouldApplyOncePerCategoryAndPreserveNulls()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("A");
        builder.add("a");
        builder.addNull();
        builder.add("B");
        builder.add("b");
        ColumnCategorical<String> column = builder.build();

        ColumnCategorical<String> transformed = column.transform(Transformer.of(String::toLowerCase, String.class));

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

        ColumnCategorical<String> transformed = constant.transform(Transformer.of(String::toLowerCase, String.class));

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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("1");
        builder.add("x");
        ColumnCategorical<String> column = builder.build();

        ColumnCategorical<Object> transformed = column
                .transform(Transformer.of(s -> "1".equals(s) ? Integer.valueOf(1) : s, Object.class));

        assertEquals(Object.class, transformed.getType().getValueClass());
        assertEquals(Integer.valueOf(1), transformed.get(0));
        assertEquals("x", transformed.get(1));
    }

    @Test
    void transformViewShouldApplyOnlyForCodesUsedInView()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("A");
        builder.add("B");
        builder.add("C");
        ColumnCategorical<String> original = builder.build();

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(2);
        rowIndexBuilder.add(2);
        ViewIndex rowIndex = rowIndexBuilder.build();
        ColumnCategorical<String> view = original.view(rowIndex);

        AtomicInteger calls = new AtomicInteger(0);
        ColumnCategorical<String> transformed = view.transform(Transformer.of(x -> {
            calls.incrementAndGet();
            return x.toLowerCase();
        }, app.babylon.table.column.ColumnTypes.STRING));

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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(S,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("A");
        builder.add("a");
        builder.add("B");
        builder.add("b");
        ColumnCategorical<String> original = builder.build();

        ColumnCategorical<String> transformed = original.transform(Transformer.of(String::toLowerCase, String.class));

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(0);
        rowIndexBuilder.add(1);
        rowIndexBuilder.add(0);
        ColumnCategorical<String> view = transformed.view(rowIndexBuilder.build());

        assertEquals(3, view.size());
        assertEquals("a", view.get(0));
        assertEquals("a", view.get(1));
        assertEquals("a", view.get(2));
        assertNotEquals(view.getCategoryCode(0), view.getCategoryCode(1));
        assertEquals(view.getCategoryCode(0), view.getCategoryCode(2));
        int[] liveCodes = view.getCategoryCodes(null);
        assertArrayEquals(new int[]
        {view.getCategoryCode(0), view.getCategoryCode(1)}, liveCodes);
    }

    @Test
    void transformShouldUseTransformerColumnNameWhenProvided()
    {
        final ColumnName OLD_NAME = ColumnName.of("OLD_NAME");
        final ColumnName NEW_NAME = ColumnName.of("NEW_NAME");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(OLD_NAME,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("A");
        builder.add("B");
        ColumnCategorical<String> column = builder.build();

        ColumnCategorical<String> transformed = column
                .transform(Transformer.of(String::toLowerCase, String.class, NEW_NAME));

        assertEquals(NEW_NAME, transformed.getName());
        assertEquals("a", transformed.get(0));
        assertEquals("b", transformed.get(1));
    }
}
