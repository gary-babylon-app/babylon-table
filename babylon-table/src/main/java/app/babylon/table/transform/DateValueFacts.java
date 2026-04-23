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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Lightweight facts extracted from a date candidate string.
 */
public final class DateValueFacts
{
    private final String text;
    private final int size;
    private final boolean onlyDigits;
    private final boolean isDecimal;
    private final int alphaCount;
    private final int periodCount;
    private final int commaCount;

    private DateValueFacts(String stripped)
    {
        this.text = stripped;
        this.size = stripped.length();

        int alphaCountLocal = 0;
        int commaCountLocal = 0;
        int periodCountLocal = 0;
        boolean onlyDigitsLocal = this.size > 0;
        boolean hasDigit = false;
        boolean seenPeriod = false;
        boolean isDecimalLocal = true;
        int start = (stripped.charAt(0) == '-' || stripped.charAt(0) == '+') ? 1 : 0;

        for (int i = 0; i < this.size; ++i)
        {
            char c = stripped.charAt(i);
            boolean digit = c >= '0' && c <= '9';
            if (!digit)
            {
                onlyDigitsLocal = false;
            }
            if (digit)
            {
                hasDigit = true;
            }
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
            {
                ++alphaCountLocal;
            }
            if (c == '.')
            {
                ++periodCountLocal;
            }
            if (c == ',')
            {
                ++commaCountLocal;
            }

            if (i < start)
            {
                continue;
            }
            if (digit)
            {
                continue;
            }
            if (c == '.')
            {
                if (seenPeriod)
                {
                    isDecimalLocal = false;
                }
                seenPeriod = true;
                continue;
            }
            isDecimalLocal = false;
        }
        if (!hasDigit)
        {
            isDecimalLocal = false;
        }

        this.alphaCount = alphaCountLocal;
        this.commaCount = commaCountLocal;
        this.periodCount = periodCountLocal;
        this.onlyDigits = onlyDigitsLocal;
        this.isDecimal = isDecimalLocal;
    }

    /**
     * Builds date facts from the supplied text.
     *
     * @param value
     *            text to inspect
     * @return extracted facts or {@code null}
     */
    public static DateValueFacts from(CharSequence value)
    {
        if (value == null)
        {
            return null;
        }
        String stripped = value.toString().strip();
        if (stripped.isEmpty())
        {
            return null;
        }
        return new DateValueFacts(stripped);
    }

    /**
     * Returns the stripped source text.
     */
    public String text()
    {
        return this.text;
    }

    /**
     * Returns the stripped text length.
     */
    public int size()
    {
        return this.size;
    }

    /**
     * Returns whether the stripped text contains digits only.
     */
    public boolean onlyDigits()
    {
        return this.onlyDigits;
    }

    /**
     * Returns whether the text also looks like a decimal number.
     */
    public boolean isDecimal()
    {
        return this.isDecimal;
    }

    /**
     * Returns the number of alphabetic characters.
     */
    public int alphaCount()
    {
        return this.alphaCount;
    }

    /**
     * Returns whether the text contains commas.
     */
    public boolean hasCommas()
    {
        return this.commaCount > 0;
    }

    /**
     * Returns whether the text contains periods.
     */
    public boolean hasPeriods()
    {
        return this.periodCount > 0;
    }

    /**
     * Returns whether the text has token shapes that disqualify it from the date
     * heuristics.
     */
    public boolean invalidForDateTokens()
    {
        if (this.commaCount > 0 || this.periodCount > 0)
        {
            return true;
        }
        return this.alphaCount > 3;
    }

    /**
     * Returns whether the value looks like an Excel serial date.
     */
    public boolean isExcelLocalDate()
    {
        if (this.size != 5 || !this.onlyDigits)
        {
            return false;
        }
        long dateValue = Long.parseLong(this.text);
        return dateValue >= 25569L && dateValue <= 109575L;
    }

    /**
     * Returns whether the value looks like an Excel serial date/time.
     */
    public boolean isExcelLocalDateTime()
    {
        return this.isDecimal;
    }

    /**
     * Splits a natural-language date into three chronological fields.
     */
    public String[] naturalDateSplit()
    {
        String s = this.text;

        String[] chronoFields = new String[3];
        StringBuilder sb = new StringBuilder();

        int i = 0;
        char c = s.charAt(i++);
        while (isDigit(c))
        {
            sb.append(c);
            if (i < s.length())
            {
                c = s.charAt(i++);
            }
            else
            {
                break;
            }
        }
        chronoFields[0] = sb.toString();
        while (!isDigit(c) && !isAlpha(c))
        {
            if (i < s.length())
            {
                c = s.charAt(i++);
            }
            else
            {
                break;
            }
        }
        sb = new StringBuilder();
        if (isAlpha(c))
        {
            while (isAlpha(c))
            {
                sb.append(c);
                if (i < s.length())
                {
                    c = s.charAt(i++);
                }
                else
                {
                    break;
                }
            }
        }
        else
        {
            while (isDigit(c))
            {
                sb.append(c);
                if (i < s.length())
                {
                    c = s.charAt(i++);
                }
                else
                {
                    break;
                }
            }
        }
        chronoFields[1] = sb.toString();
        while (!isDigit(c) && !isAlpha(c))
        {
            if (i < s.length())
            {
                c = s.charAt(i++);
            }
            else
            {
                break;
            }
        }

        sb = new StringBuilder();
        while (isDigit(c))
        {
            sb.append(c);
            if (i < s.length())
            {
                c = s.charAt(i++);
            }
            else
            {
                break;
            }
        }
        chronoFields[2] = sb.toString();
        return chronoFields;
    }

    /**
     * Parses three numeric groups from the source text.
     */
    public int[] parseThreeNumberGroups()
    {
        String s = this.text;
        int len = s.length();
        int i = 0;

        int a = 0;
        int aDigits = 0;
        while (i < len)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                break;
            }
            a = (a * 10) + (c - '0');
            ++aDigits;
            ++i;
        }
        if (aDigits == 0)
        {
            return null;
        }

        while (i < len)
        {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9')
            {
                break;
            }
            ++i;
        }
        if (i >= len)
        {
            return null;
        }

        int b = 0;
        int bDigits = 0;
        while (i < len)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                break;
            }
            b = (b * 10) + (c - '0');
            ++bDigits;
            ++i;
        }
        if (bDigits == 0)
        {
            return null;
        }

        while (i < len)
        {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9')
            {
                break;
            }
            ++i;
        }
        if (i >= len)
        {
            return null;
        }

        int c = 0;
        int cDigits = 0;
        while (i < len)
        {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9')
            {
                break;
            }
            c = (c * 10) + (ch - '0');
            ++cDigits;
            ++i;
        }
        if (cDigits == 0)
        {
            return null;
        }
        return new int[]
        {a, b, c};
    }

    private static boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private int[] toYearMonthDay(DateFormat format)
    {
        String[] chronoFields;
        DateFormat effectiveFormat = format;
        if (this.size() == 8 && this.onlyDigits())
        {
            chronoFields = new String[]
            {this.text().substring(0, 4), this.text().substring(4, 6), this.text().substring(6, 8)};
            effectiveFormat = DateFormat.YMD;
        }
        else
        {
            if (effectiveFormat == null)
            {
                return null;
            }
            chronoFields = this.naturalDateSplit();
        }

        int[] ymd = toYearMonthDay(chronoFields, effectiveFormat);

        return ymd;
    }

    private static Integer parseInt(String x)
    {
        if (x == null || x.isEmpty())
        {
            return null;
        }

        int start = 0;
        boolean negative = false;
        char first = x.charAt(0);
        if (first == '-')
        {
            negative = true;
            start = 1;
            if (x.length() == 1)
            {
                return null;
            }
        }

        int value = 0;
        for (int i = start; i < x.length(); ++i)
        {
            char c = x.charAt(i);
            if (c < '0' || c > '9')
            {
                return null;
            }
            value = value * 10 + (c - '0');
        }
        return Integer.valueOf(negative ? -value : value);
    }

    /**
     * Converts the captured text to a local date using the supplied format.
     *
     * @param format
     *            date token order
     * @return parsed local date or {@code null}
     */
    public LocalDate toLocalDate(DateFormat format)
    {

        if (this.isExcelLocalDate())
        {
            long dateValue = Long.parseLong(this.text());
            return LocalDate.ofEpochDay(dateValue + (LocalDate.EPOCH.toEpochDay() - 25569L));
        }
        if (this.isExcelLocalDateTime())
        {
            try
            {
                BigDecimal bd = new BigDecimal(this.text());
                if (bd.doubleValue() >= 25569d && bd.doubleValue() <= 109575d)
                {
                    long dateValue = (long) bd.doubleValue();
                    return LocalDate.ofEpochDay(dateValue + (LocalDate.EPOCH.toEpochDay() - 25569L));
                }
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }

        if (this.size() < 8 || this.size() > 11)
        {
            return null;
        }
        if (this.hasCommas() || this.hasPeriods() || this.alphaCount() > 3)
        {
            return null;
        }
        if (this.size() != 8 && this.onlyDigits())
        {
            return null;
        }

        int[] ymd = toYearMonthDay(format);
        if (ymd == null || !isValidDate(ymd[0], ymd[1], ymd[2]))
        {
            return null;
        }
        return LocalDate.of(ymd[0], ymd[1], ymd[2]);
    }

    private static boolean isValidDate(int year, int month, int day)
    {
        if (year < 1 || year > 9999)
        {
            return false;
        }
        if (month < 1 || month > 12)
        {
            return false;
        }
        if (day < 1)
        {
            return false;
        }
        int maxDay = switch (month)
        {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11 -> 30;
            case 2 -> isLeapYear(year) ? 29 : 28;
            default -> 0;
        };
        return day <= maxDay;
    }

    private static boolean isLeapYear(int year)
    {
        return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }

    private static int monthNumber(String chronoField)
    {
        switch (chronoField)
        {
            case "JAN" -> {
                return 1;
            }
            case "FEB" -> {
                return 2;
            }
            case "MAR" -> {
                return 3;
            }
            case "APR" -> {
                return 4;
            }
            case "MAY" -> {
                return 5;
            }
            case "JUN" -> {
                return 6;
            }
            case "JUL" -> {
                return 7;
            }
            case "AUG" -> {
                return 8;
            }
            case "SEP" -> {
                return 9;
            }
            case "OCT" -> {
                return 10;
            }
            case "NOV" -> {
                return 11;
            }
            case "DEC" -> {
                return 12;
            }
            default -> {
                return -1;
            }
        }
    }

    private int[] toYearMonthDay(String[] chronoFields, DateFormat format)
    {
        if (chronoFields == null || chronoFields.length < 3 || format == null)
        {
            return null;
        }

        Integer c0 = parseInt(chronoFields[0]);
        Integer c2 = parseInt(chronoFields[2]);
        if (c0 == null || c2 == null)
        {
            return null;
        }

        Integer c1 = null;
        if (this.alphaCount() > 0)
        {
            int month = monthNumber(chronoFields[1].toUpperCase(Locale.UK));
            if (month > 0)
            {
                c1 = Integer.valueOf(month);
            }
        }
        else
        {
            c1 = parseInt(chronoFields[1]);
        }
        if (c1 == null)
        {
            return null;
        }

        int year;
        int month;
        int day;
        switch (format)
        {
            case YMD -> {
                year = c0.intValue();
                month = c1.intValue();
                day = c2.intValue();
            }
            case DMY -> {
                day = c0.intValue();
                month = c1.intValue();
                year = c2.intValue();
            }
            case MDY -> {
                month = c0.intValue();
                day = c1.intValue();
                year = c2.intValue();
            }
            default -> {
                return null;
            }
        }
        if (year < 100)
        {
            year += 2000;
        }
        return new int[]
        {year, month, day};
    }

}
