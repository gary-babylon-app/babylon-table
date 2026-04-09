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

import org.junit.jupiter.api.Test;

class TableDescriptionTest
{
    @Test
    void shouldCollapseWhitespaceAndControlCharacters()
    {
        TableDescription description = new TableDescription("  alpha\t\tbeta\n\u0000gamma\r\n  delta  ");

        assertEquals("alpha beta gamma delta", description.getValue());
        assertEquals("alpha beta gamma delta", description.toString());
    }

    @Test
    void shouldAllowEmptyDescriptions()
    {
        assertEquals("", new TableDescription("   \t\r\n ").getValue());
        assertEquals("", new TableDescription().getValue());
    }

    @Test
    void shouldTreatNullAsEmpty()
    {
        TableDescription description = new TableDescription(null);

        assertEquals("", description.getValue());
        assertEquals(0, description.length());
    }
}
