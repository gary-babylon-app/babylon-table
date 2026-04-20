/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.transform;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DateFormatInferenceTest
{
    @Test
    void inferFormatsShouldResolveAmbiguousFromClearColumn()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        final ColumnName SETTLE_DATE = ColumnName.of("settle_date");
        ColumnObject.Builder<String> ambiguous = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        ambiguous.add("01/02/2026");
        ambiguous.add("03/02/2026");

        ColumnObject.Builder<String> clearDmy = ColumnObject.builder(SETTLE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
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
        final ColumnName START_DATE = ColumnName.of("start_date");
        final ColumnName END_DATE = ColumnName.of("end_date");
        ColumnObject.Builder<String> dmy = ColumnObject.builder(START_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        dmy.add("15/02/2026");
        dmy.add("16/02/2026");

        ColumnObject.Builder<String> ymd = ColumnObject.builder(END_DATE, app.babylon.table.column.ColumnTypes.STRING);
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
    void inferFormatsShouldHandleNullColumnEntries()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        final ColumnName SETTLE_DATE = ColumnName.of("settle_date");
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        tradeDates.add("15/02/2026");
        tradeDates.add("16/02/2026");

        ColumnObject.Builder<String> settleDates = ColumnObject.builder(SETTLE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        settleDates.add("17/02/2026");
        settleDates.add("18/02/2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {tradeDates.build(), null, settleDates.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
        assertEquals(DateFormat.DMY, inferred[1]);
        assertEquals(DateFormat.DMY, inferred[2]);
    }

    @Test
    void inferFormatsShouldRecogniseExcelDate()
    {
        final ColumnName PAYMENT_DATE = ColumnName.of("payment_date");
        ColumnObject.Builder<String> excel = ColumnObject.builder(PAYMENT_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        excel.add("45200");
        excel.add("45201");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {excel.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.ExcelLocalDate, inferred[0]);
    }

    @Test
    void inferFormatsShouldRecogniseExcelDateTime()
    {
        final ColumnName BOOKING_DATE = ColumnName.of("booking_date");
        ColumnObject.Builder<String> excel = ColumnObject.builder(BOOKING_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        excel.add("45436.2980092593");
        excel.add("45437.5000000000");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {excel.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.ExcelLocalDateTime, inferred[0]);
    }

    @Test
    void inferFormatsShouldReturnUnknownForExcelDateTimeWithoutDateNameHint()
    {
        final ColumnName VALUE = ColumnName.of("value");
        ColumnObject.Builder<String> excel = ColumnObject.builder(VALUE, app.babylon.table.column.ColumnTypes.STRING);
        excel.add("45436.2980092593");
        excel.add("45437.5000000000");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {excel.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.Unknown, inferred[0]);
    }

    @Test
    void inferFormatsShouldReturnUnknownForEmptyColumn()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> empty = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        empty.addNull();
        empty.add("   ");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {empty.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.Unknown, inferred[0]);
    }

    @Test
    void inferFormatsShouldRecogniseAlphaMonthDates()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> alpha = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        alpha.add("01-Jan-2026");
        alpha.addNull();
        alpha.add("   ");
        alpha.add("14-Feb-2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {alpha.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }

    @Test
    void inferFormatsShouldRecogniseCompactNumericYmdDates()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> compact = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        compact.add("20260228");
        compact.add("20240229");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {compact.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.YMD, inferred[0]);
    }

    @Test
    void inferFormatsShouldReturnUnknownForInvalidCompactLeapDay()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> compact = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        compact.add("20230229");
        compact.add("20230230");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {compact.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.Unknown, inferred[0]);
    }

    @Test
    void inferFormatsShouldRecogniseCompactAlphaMonthDates()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> alpha = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        alpha.add("01Jan2026");
        alpha.add("14Feb2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {alpha.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }

    @Test
    void inferFormatsShouldVerifyAlphaMonthYmdAgainstDominantYmdColumn()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> ymd = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        ymd.add("2026-03-01");
        ymd.add("2026-03-02");
        ymd.add("2026-Mar-03");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {ymd.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.YMD, inferred[0]);
    }

    @Test
    void inferFormatShouldRecogniseMdyWhenOnlyThatFormatIsValid()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> mdy = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        mdy.add("12/31/2026");
        mdy.add("11/30/2026");

        DateFormat inferred = DateFormatInference.inferFormat(mdy.build());

        assertEquals(DateFormat.MDY, inferred);
    }

    @Test
    void inferFormatShouldReturnUnknownWhenBestCandidateFailsFullVerification()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> mixed = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        for (int i = 0; i < 128; ++i)
        {
            mixed.add("12/31/2026");
        }
        for (int i = 0; i < 5; ++i)
        {
            mixed.add("invalid");
        }

        DateFormat inferred = DateFormatInference.inferFormat(mixed.build());

        assertEquals(DateFormat.Unknown, inferred);
    }

    @Test
    void inferFormatsShouldRecogniseSingleDigitDayAlphaMonthDates()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> alpha = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
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
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> numeric = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
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
        final ColumnName DATE_MIXED = ColumnName.of("date_mixed");
        ColumnObject.Builder<String> noisy = ColumnObject.builder(DATE_MIXED,
                app.babylon.table.column.ColumnTypes.STRING);
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
    void inferFormatsShouldReturnUnknownForInvalidAlphaMonthText()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> alpha = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        alpha.add("01-Xxx-2026");
        alpha.add("14-Xxx-2026");

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {alpha.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.Unknown, inferred[0]);
    }

    @Test
    void inferFormatsShouldFallbackWhenSampleWinnerFailsFullColumnVerification()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> mixed = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);

        for (int i = 1; i <= 90; ++i)
        {
            int day = 13 + (i % 15);
            mixed.add("01/" + day + "/2026");
        }
        for (int i = 1; i <= 38; ++i)
        {
            int day = 13 + (i % 15);
            mixed.add(day + "/01/2026");
        }
        for (int i = 1; i <= 5000; ++i)
        {
            int day = 13 + (i % 15);
            mixed.add(day + "/02/2026");
        }

        @SuppressWarnings("unchecked")
        ColumnObject<String>[] columns = new ColumnObject[]
        {mixed.build()};
        DateFormat[] inferred = DateFormatInference.inferFormats(columns);

        assertEquals(DateFormat.DMY, inferred[0]);
    }

    @Test
    void inferFormatShouldInferSingleColumnDirectly()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> dates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        dates.add("2026-03-01");
        dates.addNull();
        dates.add("2026-03-02");

        DateFormat inferred = DateFormatInference.inferFormat(dates.build());

        assertEquals(DateFormat.YMD, inferred);
    }

    @Test
    void isLikelyDateShouldRejectWhitespaceOnlyString()
    {
        assertEquals(false, DateFormatInference.isLikelyDate("   "));
    }

    @Test
    void isLikelyDateShouldRecogniseExcelDateTimeWhenDateNamed()
    {
        assertEquals(true, DateFormatInference.isLikelyDate("45436.2980092593", true));
        assertEquals(false, DateFormatInference.isLikelyDate("45436.2980092593", false));
    }

    @Test
    void isStrictIntegerShouldRejectWhitespaceOnlyString()
    {
        assertEquals(true, DateFormatInference.isStrictInteger("+123"));
        assertEquals(false, DateFormatInference.isStrictInteger("    "));
        assertEquals(false, DateFormatInference.isStrictInteger(null));
        assertEquals(false, DateFormatInference.isStrictInteger("+"));
        assertEquals(false, DateFormatInference.isStrictInteger("12a"));
    }

    @Test
    void isStrictDecimalShouldRejectWhitespaceOnlyString()
    {
        assertEquals(true, DateFormatInference.isStrictDecimal("123"));
        assertEquals(true, DateFormatInference.isStrictDecimal("-123.45"));
        assertEquals(false, DateFormatInference.isStrictDecimal("    "));
        assertEquals(false, DateFormatInference.isStrictDecimal(null));
        assertEquals(false, DateFormatInference.isStrictDecimal("+"));
        assertEquals(false, DateFormatInference.isStrictDecimal("."));
        assertEquals(false, DateFormatInference.isStrictDecimal("+."));
        assertEquals(false, DateFormatInference.isStrictDecimal("123."));
        assertEquals(false, DateFormatInference.isStrictDecimal("20.08.74"));
        assertEquals(false, DateFormatInference.isStrictDecimal("1a2"));
    }

    @Test
    void privateHelpersShouldCoverMonthParsingLeapYearsAndExcelBounds() throws Exception
    {
        assertEquals(4, invokeInt("parseMonthText", "Apr"));
        assertEquals(5, invokeInt("parseMonthText", "May"));
        assertEquals(6, invokeInt("parseMonthText", "Jun"));
        assertEquals(7, invokeInt("parseMonthText", "Jul"));
        assertEquals(8, invokeInt("parseMonthText", "Aug"));
        assertEquals(9, invokeInt("parseMonthText", "Sep"));
        assertEquals(10, invokeInt("parseMonthText", "Oct"));
        assertEquals(11, invokeInt("parseMonthText", "Nov"));
        assertEquals(12, invokeInt("parseMonthText", "Dec"));
        assertEquals(-1, invokeInt("parseMonthText", "Foo"));

        assertEquals(true, invokeBoolean("isValidYyyyMmDd8", DateValueFacts.from("20000229")));
        assertEquals(false, invokeBoolean("isValidYyyyMmDd8", DateValueFacts.from("19000229")));
        assertEquals(false, invokeBoolean("isValidYyyyMmDd8", DateValueFacts.from("20261301")));
        assertEquals(false, invokeBoolean("isExcelLocalDate", DateValueFacts.from("25568")));
        assertEquals(true, invokeBoolean("isExcelLocalDate", DateValueFacts.from("25569")));
        assertEquals(false, invokeBoolean("isExcelLocalDate", DateValueFacts.from("25A69")));
        assertEquals(0, invokeInt("maxDayInMonth", Integer.valueOf(2026), Integer.valueOf(13)));
    }

    @Test
    void privateHelpersShouldCoverMalformedSplitDates() throws Exception
    {
        assertEquals(EnumSet.noneOf(DateFormat.class),
                invokeDateFormatSet("numericCandidatesForSplitDate", DateValueFacts.from("----")));
        assertEquals(EnumSet.noneOf(DateFormat.class),
                invokeDateFormatSet("numericCandidatesForSplitDate", DateValueFacts.from("2026----")));
        assertEquals(EnumSet.noneOf(DateFormat.class),
                invokeDateFormatSet("alphaMonthCandidatesForSplitDate", DateValueFacts.from("----")));
        assertEquals(EnumSet.noneOf(DateFormat.class),
                invokeDateFormatSet("alphaMonthCandidatesForSplitDate", DateValueFacts.from("AA-Jan-2026")));
        assertEquals(EnumSet.noneOf(DateFormat.class),
                invokeDateFormatSet("alphaMonthCandidatesForSplitDate", DateValueFacts.from("01-March-2026")));
    }

    @Test
    void privateHelpersShouldCoverRankingAndMatchingEdges() throws Exception
    {
        Map<DateFormat, Integer> votes = new EnumMap<>(DateFormat.class);
        Object emptyRanking = invokeObject("rank", votes, Integer.valueOf(2));
        assertEquals(null, invokeObject(emptyRanking, "best"));
        assertEquals(0.0d, (double) invokeObject(emptyRanking, "bestConfidence"));

        assertFalse(invokeBoolean(new Class<?>[]
        {DateValueFacts.class, DateFormat.class}, "matchesFormat", null, DateFormat.DMY));
        assertFalse(invokeBoolean(new Class<?>[]
        {DateValueFacts.class, DateFormat.class}, "matchesFormat", DateValueFacts.from("2026-03-01"), null));
        assertFalse(invokeBoolean("matchesFormat", DateValueFacts.from("20260228"), DateFormat.DMY));
        assertFalse(invokeBoolean("matchesFormat", DateValueFacts.from("2026Mar03"), DateFormat.DMY));
        assertFalse(invokeBoolean("matchesFormat", DateValueFacts.from("--Jan-2026"), DateFormat.DMY));
        assertFalse(invokeBoolean("matchesFormat", DateValueFacts.from("01-Ab-2026"), DateFormat.DMY));
        assertFalse(invokeBoolean("matchesFormat", DateValueFacts.from("2026-Mar-03"), DateFormat.MDY));
        assertFalse(invokeBoolean("matchesFormat", DateValueFacts.from("2026////01"), DateFormat.DMY));

        final ColumnName TRADE_DATE = ColumnName.of("trade_date");
        ColumnObject.Builder<String> empty = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        empty.addNull();
        empty.add("   ");

        Object verification = invokeObject("verifyColumn", empty.build(), DateFormat.DMY);
        assertEquals(1.0d, (double) invokeObject(verification, "failureRate"));
    }

    @Test
    void inferFormatsShouldRejectNullColumnsArray()
    {
        try
        {
            DateFormatInference.inferFormats(null);
        }
        catch (RuntimeException e)
        {
            assertNotNull(e.getMessage());
            assertFalse(e.getMessage().isBlank());
            return;
        }
        throw new AssertionError("Expected inferFormats(null) to throw");
    }

    private static boolean invokeBoolean(String methodName, Object... args) throws Exception
    {
        return (boolean) invokeStatic(methodName, args);
    }

    private static boolean invokeBoolean(Class<?>[] parameterTypes, String methodName, Object... args) throws Exception
    {
        return (boolean) invokeStatic(parameterTypes, methodName, args);
    }

    private static int invokeInt(String methodName, Object... args) throws Exception
    {
        return (int) invokeStatic(methodName, args);
    }

    @SuppressWarnings("unchecked")
    private static EnumSet<DateFormat> invokeDateFormatSet(String methodName, Object... args) throws Exception
    {
        return (EnumSet<DateFormat>) invokeStatic(methodName, args);
    }

    private static Object invokeStatic(String methodName, Object... args) throws Exception
    {
        return invokeStatic(parameterTypes(args), methodName, args);
    }

    private static Object invokeStatic(Class<?>[] parameterTypes, String methodName, Object... args) throws Exception
    {
        Method method = DateFormatInference.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object invokeObject(String methodName, Object... args) throws Exception
    {
        return invokeStatic(methodName, args);
    }

    private static Object invokeObject(Object target, String methodName) throws Exception
    {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Class<?>[] parameterTypes(Object[] args)
    {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; ++i)
        {
            parameterTypes[i] = switch (args[i])
            {
                case DateValueFacts facts -> DateValueFacts.class;
                case ColumnObject<?> column -> ColumnObject.class;
                case DateFormat format -> DateFormat.class;
                case Integer value -> int.class;
                case Map<?, ?> map -> Map.class;
                default -> args[i].getClass();
            };
        }
        return parameterTypes;
    }
}
