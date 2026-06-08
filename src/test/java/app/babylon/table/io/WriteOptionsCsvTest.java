/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import app.babylon.table.ToStringSettings;

class WriteOptionsCsvTest
{
    @Test
    void standardUsesCsvWriteDefaults()
    {
        WriteOptionsCsv options = WriteOptionsCsv.standard();

        assertEquals(StandardCharsets.UTF_8, options.charset());
        assertNotNull(options.toStringSettings());
        assertTrue(options.includeHeaders());
        assertEquals(',', options.separator());
        assertEquals("\r\n", options.lineSeparator());
    }

    @Test
    void builderCreatesOptions()
    {
        WriteOptionsCsv options = WriteOptionsCsv.builder().withCharset(StandardCharsets.ISO_8859_1)
                .withIncludeHeaders(false).withSeparator(';').withLineSeparator("\n").build();

        assertEquals(StandardCharsets.ISO_8859_1, options.charset());
        assertNotNull(options.toStringSettings());
        assertFalse(options.includeHeaders());
        assertEquals(';', options.separator());
        assertEquals("\n", options.lineSeparator());
    }

    @Test
    void builderKeepsConfiguredToStringSettings()
    {
        ToStringSettings settings = ToStringSettings.standard();
        WriteOptionsCsv options = WriteOptionsCsv.builder().withToStringSettings(settings).build();

        assertEquals(settings, options.toStringSettings());
    }
}
