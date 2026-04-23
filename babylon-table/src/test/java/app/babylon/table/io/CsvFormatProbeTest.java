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
    void shouldReturnDefaultsWhenNoInterestingCharactersArePresent()
    {
        CsvFormat format = CsvFormatProbe.detect("plain words only", StandardCharsets.UTF_8, ';', '\'');

        assertEquals(StandardCharsets.UTF_8, format.charset());
        assertEquals(';', format.separator());
        assertEquals('\'', format.quote());
        assertEquals(0.0d, format.confidence());
    }

    @Test
    void shouldCountAllInterestingCharactersThroughDetectPath()
    {
        String sample = "A,B\tC;D|E\"F\"\n";

        CsvFormat format = CsvFormatProbe.detect(sample, StandardCharsets.UTF_8, ',', '"');

        assertEquals(StandardCharsets.UTF_8, format.charset());
        assertEquals('"', format.quote());
        assertEquals(0.0d, format.confidence());
    }

    @Test
    void shouldIgnoreColonUsedInsideDateTimeValues()
    {
        String sample = "" + "Date,Time,Value\n" + "2026-01-01,10:15:30,1\n" + "2026-01-02,11:45:00,2\n";

        CsvFormat format = CsvFormatProbe.detect(sample, StandardCharsets.UTF_8, ';', '"');

        assertEquals(',', format.separator());
        assertEquals('"', format.quote());
    }

    @Test
    void shouldDetectSemicolonSeparator()
    {
        CsvFormat format = CsvFormatProbe.detect("City;Note\nParis;'Price;12'\n", StandardCharsets.UTF_8, ',', '"');

        assertEquals(StandardCharsets.UTF_8, format.charset());
        assertEquals(';', format.separator());
        assertEquals('"', format.quote());
    }

    @Test
    void shouldDetectTabSeparator()
    {
        String sample = "City\tNote\nParis\t\"Price\t12\"\n";

        CsvFormat format = CsvFormatProbe.detect(sample, StandardCharsets.UTF_8, ',', '"');

        assertEquals(StandardCharsets.UTF_8, format.charset());
        assertEquals('\t', format.separator());
        assertEquals('"', format.quote());
    }

    @Test
    void shouldDetectPipeSeparator()
    {
        String sample = "City|Note\nParis|\"Price|12\"\n";

        CsvFormat format = CsvFormatProbe.detect(sample, StandardCharsets.UTF_8, ',', '"');

        assertEquals(StandardCharsets.UTF_8, format.charset());
        assertEquals('|', format.separator());
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
