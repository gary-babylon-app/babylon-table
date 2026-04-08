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

public class ColumnStringTest
{
    @Test
    public void testSmallMutable()
    {
        ColumnObject.Builder<String> cs = ColumnObject.builder(ColumnName.of("Test"), String.class);
        cs.add(BigDecimal.ZERO.toPlainString());
        cs.add(BigDecimal.ONE.toPlainString());
        cs.add(BigDecimal.TEN.toPlainString());

        String actual = cs.build().toString();
        String expected = "Test[0, 1, ... ,10]";
        assertEquals(expected, actual);
    }
}
