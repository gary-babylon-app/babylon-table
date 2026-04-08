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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class TablesToStringDecimalFormattingTest
{
    @Test
    void printFullTableShouldUsePlainDecimalNotation()
    {
        ColumnObject.Builder<BigDecimal> quantity = ColumnObject.builderDecimal(ColumnName.of("Quantity"));
        quantity.add(new BigDecimal("1E+2"));
        quantity.add(new BigDecimal("-4E+1"));

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), quantity.build());

        String printed = table.toString(ToStringSettings.standard());

        assertTrue(printed.contains("100"));
        assertTrue(printed.contains("-40"));
        assertFalse(printed.contains("1E+2"));
        assertFalse(printed.contains("-4E+1"));
    }
}
