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

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.ViewIndex;

class ColumnObjectTransformTest
{
    @Test
    void transformShouldUseTransformerColumnNameAndPreserveNullnessOnView()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName AMOUNT_TEXT = ColumnName.of("AMOUNT_TEXT");
        ColumnObject.Builder<BigDecimal> builder = ColumnObject.builderDecimal(AMOUNT);
        builder.add(new BigDecimal("10.5"));
        builder.addNull();
        builder.add(new BigDecimal("3.0"));
        ColumnObject<BigDecimal> column = builder.build();

        ViewIndex rowIndex = ViewIndex.builder().add(2).add(1).build();
        ColumnObject<BigDecimal> view = (ColumnObject<BigDecimal>) column.view(rowIndex);

        ColumnObject<String> transformed = view
                .transform(Transformer.of(BigDecimal::toPlainString, String.class, AMOUNT_TEXT));

        assertEquals(AMOUNT_TEXT, transformed.getName());
        assertEquals("3.0", transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertEquals(2, transformed.size());
    }
}
