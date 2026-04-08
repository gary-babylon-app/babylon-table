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

class DateFormatInferenceTest
{
    @Test
    void inferFormatsShouldResolveAmbiguousFromClearColumn()
    {
        ColumnObject.Builder<String> ambiguous = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        ambiguous.add("01/02/2026");
        ambiguous.add("03/02/2026");

        ColumnObject.Builder<String> clearDmy = ColumnObject.builder(ColumnName.of("settle_date"), String.class);
        clearDmy.add("15/02/2026");
        clearDmy.add("16/02/2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {ambiguous.build(), clearDmy.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
        assertEquals(DateFormat.DMY, inferred[1]);
    }

    @Test
    void inferFormatsShouldKeepDifferentPerColumnFormats()
    {
        ColumnObject.Builder<String> dmy = ColumnObject.builder(ColumnName.of("start_date"), String.class);
        dmy.add("15/02/2026");
        dmy.add("16/02/2026");

        ColumnObject.Builder<String> ymd = ColumnObject.builder(ColumnName.of("end_date"), String.class);
        ymd.add("2026-03-01");
        ymd.add("2026-03-02");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {dmy.build(), ymd.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
        assertEquals(DateFormat.YMD, inferred[1]);
    }

    @Test
    void inferFormatsShouldRecogniseExcelDate()
    {
        ColumnObject.Builder<String> excel = ColumnObject.builder(ColumnName.of("payment_date"), String.class);
        excel.add("45200");
        excel.add("45201");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {excel.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.ExcelLocalDate, inferred[0]);
    }

    @Test
    void inferFormatsShouldRecogniseAlphaMonthDates()
    {
        ColumnObject.Builder<String> alpha = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        alpha.add("01-Jan-2026");
        alpha.add("14-Feb-2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {alpha.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }

    @Test
    void inferFormatsShouldRecogniseCompactAlphaMonthDates()
    {
        ColumnObject.Builder<String> alpha = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        alpha.add("01Jan2026");
        alpha.add("14Feb2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {alpha.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }

    @Test
    void inferFormatsShouldRecogniseSingleDigitDayAlphaMonthDates()
    {
        ColumnObject.Builder<String> alpha = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        alpha.add("1Jan2026");
        alpha.add("9Feb2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {alpha.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }

    @Test
    void inferFormatsShouldAllowSingleDigitNumericDayMonth()
    {
        ColumnObject.Builder<String> numeric = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        numeric.add("1-1-2026");
        numeric.add("13-1-2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {numeric.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }

    @Test
    void inferFormatsShouldReturnUnknownForNoisyMixedColumn()
    {
        ColumnObject.Builder<String> noisy = ColumnObject.builder(ColumnName.of("date_mixed"), String.class);
        noisy.add("01/02/2026");
        noisy.add("2026-03-01");
        noisy.add("abc");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {noisy.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.Unknown, inferred[0]);
    }

    @Test
    void inferFormatsShouldFallbackWhenSampleWinnerFailsFullColumnVerification()
    {
        ColumnObject.Builder<String> mixed = ColumnObject.builder(ColumnName.of("trade_date"), String.class);

        for (int i = 1; i <= 90; ++i)
        {
            int day = 13 + (i % 15);
            mixed.add("01/" + day + "/2026"); // MDY only
        }
        for (int i = 1; i <= 38; ++i)
        {
            int day = 13 + (i % 15);
            mixed.add(day + "/01/2026"); // DMY only
        }
        for (int i = 1; i <= 5000; ++i)
        {
            int day = 13 + (i % 15);
            mixed.add(day + "/02/2026"); // DMY only strongly dominates full data
        }

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {mixed.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }
}
