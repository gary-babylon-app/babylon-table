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

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class ColumnDecimalViewTest
{
    @Test
    public void test1()
    {
        ColumnObject.Builder<BigDecimal> originalBuilder = ColumnObject.builderDecimal(ColumnName.of("Test"));
        originalBuilder.add(BigDecimals.parse("1.42"));
        originalBuilder.add(BigDecimals.parse("100,100.32"));
        originalBuilder.add(BigDecimals.parse(""));
        originalBuilder.add(BigDecimals.parse("99.99"));

        int[] viewIndex = new int[]
        {1, 3};
        ViewIndex rowIndex = ViewIndex.builder().addAll(viewIndex).build();

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> view = (ColumnObject<BigDecimal>) Columns.newView(originalBuilder.build(), rowIndex);

        assertEquals(viewIndex.length, view.size());
        assertEquals(new BigDecimal("100100.32"), view.get(0));
        assertEquals(new BigDecimal("99.99"), view.get(1));
    }
}
