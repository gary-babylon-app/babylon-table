/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.sorting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ComparatorIntTest
{
    @Test
    public void comparatorIntShouldWorkAsFunctionalInterface()
    {
        ComparatorInt cmp = (a, b) -> Integer.compare(a, b);

        assertTrue(cmp.compare(1, 2) < 0);
        assertEquals(0, cmp.compare(7, 7));
        assertTrue(cmp.compare(9, 3) > 0);
    }
}
