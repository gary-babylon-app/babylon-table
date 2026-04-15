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

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CsvFormatProbeTest
{
    @Test
    void shouldDetectSemicolonSeparator()
    {
        CsvFormat format = CsvFormatProbe.detect("City;Note\nParis;'Price;12'\n", StandardCharsets.UTF_8, ',', '"');

        assertEquals(StandardCharsets.UTF_8, format.charset());
        assertEquals(';', format.separator());
        assertEquals('"', format.quote());
    }

    @Test
    void shouldPreferWiderCommaPairOverEarlySemicolonMetadata()
    {
        String sample = "" + "MetaKey;MetaValue\n" + "AnotherKey;AnotherValue\n"
                + "Date,Description,Amount,Account,Category,Country,Reference\n"
                + "2026-01-01,Coffee,3.50,Current,Food,UK,ABC123\n";

        CsvFormat format = CsvFormatProbe.detect(sample, StandardCharsets.UTF_8, ';', '"');

        assertEquals(',', format.separator());
        assertEquals('"', format.quote());
    }

    @Test
    void shouldPreferDominantSeparatorNearQuotes()
    {
        String sample = "" + "MetaKey;MetaValue\n" + "Date,Description,Amount\n"
                + "2026-01-01,\"Coffee, corner shop\",3.50\n" + "2026-01-02,\"Salary, Bonus\",1000.00\n";

        CsvFormat format = CsvFormatProbe.detect(sample, StandardCharsets.UTF_8, ';', '"');

        assertEquals(',', format.separator());
        assertEquals('"', format.quote());
    }
}
