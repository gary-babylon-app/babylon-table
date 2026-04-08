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

import app.babylon.table.column.Transformer;
import app.babylon.table.ViewIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ColumnCategoricalTransformTest
{
    @Test
    void transformShouldApplyOncePerCategoryAndPreserveNulls()
    {
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("s"), String.class);
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
        ColumnCategorical<String> constant = ColumnCategorical.constant(ColumnName.of("s"), "X", 4, ColumnTypes.STRING);

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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("s"), String.class);
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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("s"), String.class);
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
        }, String.class));

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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("s"), String.class);
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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("old_name"), String.class);
        builder.add("A");
        builder.add("B");
        ColumnCategorical<String> column = builder.build();

        ColumnCategorical<String> transformed = column
                .transform(Transformer.of(String::toLowerCase, String.class, ColumnName.of("new_name")));

        assertEquals(ColumnName.of("new_name"), transformed.getName());
        assertEquals("a", transformed.get(0));
        assertEquals("b", transformed.get(1));
    }
}
