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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColumnObjectBuilderCompositeTest
{
    @Test
    void builderShouldChooseArrayForMostlyUniqueObjectValues()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnObject.Builder<String> builder = ColumnObject.builder(S, ColumnTypes.STRING);
        for (int i = 0; i < 230; ++i)
        {
            builder.add("value-" + i);
        }
        for (int i = 0; i < 26; ++i)
        {
            builder.add("repeat");
        }

        ColumnObject<String> built = builder.build();

        assertFalse(built instanceof ColumnCategorical<?>);
        assertTrue(built instanceof ColumnObjectArray<?>);
    }

    @Test
    void builderShouldChooseCategoricalForRepeatedObjectValues()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnObject.Builder<String> builder = ColumnObject.builder(S, ColumnTypes.STRING);
        for (int i = 0; i < 229; ++i)
        {
            builder.add("value-" + i);
        }
        for (int i = 0; i < 27; ++i)
        {
            builder.add("repeat");
        }

        ColumnObject<String> built = builder.build();

        assertTrue(built instanceof ColumnCategorical<?>);
    }
}
