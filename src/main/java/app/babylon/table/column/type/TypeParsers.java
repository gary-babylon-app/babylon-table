/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column.type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.Currency;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.transform.ColumnLocalDates;
import app.babylon.table.transform.DateFormat;
import app.babylon.text.BigDecimals;
import app.babylon.text.Currencys;
import app.babylon.text.Strings;

public final class TypeParsers
{
    /**
     * Minimum text length accepted by {@link Instant#parse(CharSequence)} for a
     * complete ISO-8601 instant, for example {@code 1970-01-01T00:00:00Z}.
     */
    private static final int INSTANT_MIN_LENGTH = 20;
    /**
     * Minimum text length accepted by {@link LocalDateTime#parse(CharSequence)} for
     * a complete ISO-8601 local date-time, for example {@code 1970-01-01T00:00:00}.
     */
    private static final int LOCAL_DATE_TIME_MIN_LENGTH = 19;
    /**
     * Minimum text length accepted by {@link LocalTime#parse(CharSequence)} for a
     * complete ISO-8601 local time, for example {@code 00:00}.
     */
    private static final int LOCAL_TIME_MIN_LENGTH = 5;
    /**
     * Minimum text length accepted by {@link OffsetDateTime#parse(CharSequence)}
     * for a complete ISO-8601 offset date-time, for example
     * {@code 1970-01-01T00:00:00Z}.
     */
    private static final int OFFSET_DATE_TIME_MIN_LENGTH = 20;
    /**
     * Minimum text length accepted by {@link YearMonth#parse(CharSequence)} for a
     * complete ISO-8601 year-month, for example {@code 1970-01}.
     */
    private static final int YEAR_MONTH_MIN_LENGTH = 7;
    /**
     * Minimum text length accepted by {@link Period#parse(CharSequence)} for a
     * complete ISO-8601 period, for example {@code P1D}. The relaxed parser below
     * also accepts forms like {@code 3M} and normalises them to {@code P3M}.
     */
    private static final int PERIOD_MIN_LENGTH = 2;
    private static final Period PERIOD_1M = Period.ofMonths(1);
    private static final Period PERIOD_3M = Period.ofMonths(3);
    private static final Period PERIOD_6M = Period.ofMonths(6);
    private static final Period PERIOD_12M = Period.ofMonths(12);
    private static final Period PERIOD_1Y = Period.ofYears(1);

    public static final TypeParser<Object> NULL = s -> null;
    public static final TypeParser<String> STRING = s -> s == null ? null : s.toString();
    public static final TypeParser<Byte> BYTE = new TypeParser<>()
    {
        @Override
        public Byte parse(CharSequence s)
        {
            if (Strings.isEmpty(s) || !Strings.isInt(s))
            {
                return null;
            }
            try
            {
                return parseByte(s);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public Byte parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0 || !Strings.isInt(s, offset, length))
            {
                return null;
            }
            try
            {
                return parseByte(s, offset, length);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<Integer> INT = new TypeParser<>()
    {
        @Override
        public Integer parse(CharSequence s)
        {
            if (Strings.isEmpty(s) || !Strings.isInt(s))
            {
                return null;
            }
            try
            {
                return parseInt(s);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public Integer parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0 || !Strings.isInt(s, offset, length))
            {
                return null;
            }
            try
            {
                return parseInt(s, offset, length);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<Long> LONG = new TypeParser<>()
    {
        @Override
        public Long parse(CharSequence s)
        {
            if (Strings.isEmpty(s) || !Strings.isLong(s))
            {
                return null;
            }
            try
            {
                return parseLong(s);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public Long parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0 || !Strings.isLong(s, offset, length))
            {
                return null;
            }
            try
            {
                return parseLong(s, offset, length);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<Double> DOUBLE = new TypeParser<>()
    {
        @Override
        public Double parse(CharSequence s)
        {
            return Strings.isEmpty(s) ? null : BigDecimals.parseDouble(s);
        }

        @Override
        public Double parse(CharSequence s, int offset, int length)
        {
            return s == null || length <= 0 ? null : BigDecimals.parseDouble(s.subSequence(offset, offset + length));
        }
    };
    public static final TypeParser<BigDecimal> BIG_DECIMAL = BigDecimals::parse;
    public static final TypeParser<Instant> INSTANT = new TypeParser<>()
    {
        @Override
        public Instant parse(CharSequence s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, 0, s.length());
            int strippedEnd = Strings.stripEnd(s, 0, s.length());
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < INSTANT_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return Instant.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public Instant parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0)
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, offset, length);
            int strippedEnd = Strings.stripEnd(s, offset, length);
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < INSTANT_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return Instant.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<LocalDateTime> LOCAL_DATE_TIME = new TypeParser<>()
    {
        @Override
        public LocalDateTime parse(CharSequence s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, 0, s.length());
            int strippedEnd = Strings.stripEnd(s, 0, s.length());
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < LOCAL_DATE_TIME_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return LocalDateTime.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public LocalDateTime parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0)
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, offset, length);
            int strippedEnd = Strings.stripEnd(s, offset, length);
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < LOCAL_DATE_TIME_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return LocalDateTime.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<LocalTime> LOCAL_TIME = new TypeParser<>()
    {
        @Override
        public LocalTime parse(CharSequence s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, 0, s.length());
            int strippedEnd = Strings.stripEnd(s, 0, s.length());
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < LOCAL_TIME_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return LocalTime.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public LocalTime parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0)
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, offset, length);
            int strippedEnd = Strings.stripEnd(s, offset, length);
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < LOCAL_TIME_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return LocalTime.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<OffsetDateTime> OFFSET_DATE_TIME = new TypeParser<>()
    {
        @Override
        public OffsetDateTime parse(CharSequence s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, 0, s.length());
            int strippedEnd = Strings.stripEnd(s, 0, s.length());
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < OFFSET_DATE_TIME_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return OffsetDateTime.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public OffsetDateTime parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0)
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, offset, length);
            int strippedEnd = Strings.stripEnd(s, offset, length);
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < OFFSET_DATE_TIME_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return OffsetDateTime.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<YearMonth> YEAR_MONTH = new TypeParser<>()
    {
        @Override
        public YearMonth parse(CharSequence s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, 0, s.length());
            int strippedEnd = Strings.stripEnd(s, 0, s.length());
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < YEAR_MONTH_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return YearMonth.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public YearMonth parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0)
            {
                return null;
            }
            int strippedOffset = Strings.stripStart(s, offset, length);
            int strippedEnd = Strings.stripEnd(s, offset, length);
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < YEAR_MONTH_MIN_LENGTH)
            {
                return null;
            }
            CharSequence candidate = strippedOffset == 0 && strippedEnd == s.length()
                    ? s
                    : s.subSequence(strippedOffset, strippedEnd);
            try
            {
                return YearMonth.parse(candidate);
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<Period> PERIOD = new TypeParser<>()
    {
        @Override
        public Period parse(CharSequence s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            Period common = parseCommonPeriod(s, 0, s.length());
            if (common != null)
            {
                return common;
            }
            int strippedOffset = Strings.stripStart(s, 0, s.length());
            int strippedEnd = Strings.stripEnd(s, 0, s.length());
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < PERIOD_MIN_LENGTH)
            {
                return null;
            }
            try
            {
                return Period.parse(normalisePeriodText(s, strippedOffset, strippedEnd));
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }

        @Override
        public Period parse(CharSequence s, int offset, int length)
        {
            if (s == null || length <= 0)
            {
                return null;
            }
            Period common = parseCommonPeriod(s, offset, offset + length);
            if (common != null)
            {
                return common;
            }
            int strippedOffset = Strings.stripStart(s, offset, length);
            int strippedEnd = Strings.stripEnd(s, offset, length);
            if (strippedOffset >= strippedEnd)
            {
                return null;
            }
            if (strippedEnd - strippedOffset < PERIOD_MIN_LENGTH)
            {
                return null;
            }
            try
            {
                return Period.parse(normalisePeriodText(s, strippedOffset, strippedEnd));
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
    };
    public static final TypeParser<LocalDate> LOCAL_DATE_YMD = localDate(DateFormat.YMD);
    public static final TypeParser<Currency> CURRENCY = new TypeParser<>()
    {
        @Override
        public Currency parse(CharSequence s)
        {
            return Currencys.parse(s);
        }

        @Override
        public Currency parse(CharSequence s, int offset, int length)
        {
            return Currencys.parse(s, offset, length);
        }
    };

    private TypeParsers()
    {
    }

    public static TypeParser<LocalDate> localDate(DateFormat format)
    {
        DateFormat x = ArgumentCheck.nonNull(format);
        return s -> ColumnLocalDates.stringToDate(s, x);
    }

    private static CharSequence normalisePeriodText(CharSequence s, int start, int endExclusive)
    {
        char first = s.charAt(start);
        if (first == 'P' || first == 'p')
        {
            return start == 0 && endExclusive == s.length() ? s : s.subSequence(start, endExclusive);
        }
        if ((first == '-' || first == '+') && start + 1 < endExclusive)
        {
            char second = s.charAt(start + 1);
            if (second == 'P' || second == 'p')
            {
                return start == 0 && endExclusive == s.length() ? s : s.subSequence(start, endExclusive);
            }
            return new StringBuilder(endExclusive - start + 1).append(first).append('P')
                    .append(s, start + 1, endExclusive).toString();
        }
        return new StringBuilder(endExclusive - start + 1).append('P').append(s, start, endExclusive);
    }

    private static Period parseCommonPeriod(CharSequence s, int start, int endExclusive)
    {
        if (s.charAt(start) == 'P' || s.charAt(start) == 'p')
        {
            return null;
        }
        int actualStart = start;
        int length = endExclusive - actualStart;
        if (length == 2)
        {
            char number = s.charAt(actualStart);
            char unit = s.charAt(actualStart + 1);
            if (unit == 'M' || unit == 'm')
            {
                return switch (number)
                {
                    case '1' -> PERIOD_1M;
                    case '3' -> PERIOD_3M;
                    case '6' -> PERIOD_6M;
                    default -> null;
                };
            }
            if (number == '1' && (unit == 'Y' || unit == 'y'))
            {
                return PERIOD_1Y;
            }
            return null;
        }
        if (length == 3 && s.charAt(actualStart) == '1' && s.charAt(actualStart + 1) == '2'
                && (s.charAt(actualStart + 2) == 'M' || s.charAt(actualStart + 2) == 'm'))
        {
            return PERIOD_12M;
        }
        return null;
    }
}
