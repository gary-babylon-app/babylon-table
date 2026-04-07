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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DictionaryEncodingTest
{
    @Test
    void codesShouldStartAtOneAndReserveZeroForNull()
    {
        DictionaryEncoding<String> encoding = DictionaryEncoding.of();

        assertEquals(1, encoding.codeOf("A"));
        assertEquals(2, encoding.codeOf("B"));
        assertEquals(1, encoding.codeOf("A"));
        assertEquals(3, encoding.size());
        assertNull(encoding.valueOf(0));
        assertEquals("A", encoding.valueOf(1));
        assertEquals("B", encoding.valueOf(2));
    }

    @Test
    void codeOfNullShouldThrow()
    {
        DictionaryEncoding<String> encoding = DictionaryEncoding.of();
        assertThrows(IllegalArgumentException.class, () -> encoding.codeOf(null));
    }
}
