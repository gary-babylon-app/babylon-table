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

import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.DateValueFacts;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DateFormatInference
{
    private static final long EXCEL_MIN = 25569L;
    private static final long EXCEL_MAX = 109575L;
    private static final double MIN_CONFIDENCE = 0.60d;
    private static final int SAMPLE_SIZE = 128;
    private static final double MAX_FAILURE_RATE = 0.02d;
    private static final double MIN_MARGIN = 0.05d;
    private static final double MAX_CONFIDENCE_DROP = 0.25d;

    private DateFormatInference()
    {
    }

    public static DateFormat[] inferFormats(ColumnObject<String>[] columns)
    {
        ColumnObject<String>[] cols = Objects.requireNonNull(columns, "columns must not be null");
        DateFormat[] inferred = new DateFormat[cols.length];
        double[] confidence = new double[cols.length];

        for (int i = 0; i < cols.length; ++i)
        {
            ColumnObject<String> column = cols[i];
            if (column == null)
            {
                inferred[i] = DateFormat.Unknown;
                confidence[i] = 0.0d;
                continue;
            }
            ColumnInference result = inferColumn(column);
            inferred[i] = result.format();
            confidence[i] = result.confidence();
        }

        DateFormat dominant = dominantFormat(inferred, confidence);
        if (dominant != null)
        {
            for (int i = 0; i < inferred.length; ++i)
            {
                if (inferred[i] == null || inferred[i] == DateFormat.Unknown)
                {
                    inferred[i] = dominant;
                }
            }
        }

        for (int i = 0; i < inferred.length; ++i)
        {
            if (inferred[i] == null)
            {
                inferred[i] = DateFormat.Unknown;
            }
        }

        return inferred;
    }

    private static ColumnInference inferColumn(ColumnObject<String> column)
    {
        boolean isDateName = column.getName().getCanonical().contains("date");
        int sampledNonEmpty = 0;
        int sampledIncompatible = 0;
        Map<DateFormat, Integer> sampleVotes = new EnumMap<>(DateFormat.class);

        for (int i = 0; i < column.size() && sampledNonEmpty < SAMPLE_SIZE; ++i)
        {
            if (!column.isSet(i))
            {
                continue;
            }
            DateValueFacts valueFacts = DateValueFacts.from(column.get(i));
            if (valueFacts == null)
            {
                continue;
            }
            EnumSet<DateFormat> candidates = candidatesForValue(valueFacts, isDateName);
            ++sampledNonEmpty;
            if (candidates.isEmpty())
            {
                ++sampledIncompatible;
                continue;
            }
            for (DateFormat format : candidates)
            {
                sampleVotes.merge(format, Integer.valueOf(1), Integer::sum);
            }
        }

        if (sampledNonEmpty == 0 || sampledIncompatible > sampledNonEmpty / 3)
        {
            return new ColumnInference(DateFormat.Unknown, 0.0d);
        }

        CandidateRanking ranking = rank(sampleVotes, sampledNonEmpty);
        if (ranking.best() == null || ranking.bestConfidence() < MIN_CONFIDENCE || ranking.margin() < MIN_MARGIN)
        {
            return new ColumnInference(DateFormat.Unknown, ranking.bestConfidence());
        }

        Verification bestVerification = verifyColumn(column, ranking.best());
        double bestFullConfidence = 1.0d - bestVerification.failureRate();
        if (bestVerification.failureRate() <= MAX_FAILURE_RATE && bestFullConfidence >= MIN_CONFIDENCE
                && (ranking.bestConfidence() - bestFullConfidence) <= MAX_CONFIDENCE_DROP)
        {
            return new ColumnInference(ranking.best(), bestFullConfidence);
        }

        DateFormat fallbackFormat = null;
        Verification fallbackVerification = null;
        for (DateFormat candidate : rankAll(sampleVotes))
        {
            if (candidate == ranking.best())
            {
                continue;
            }
            Verification verification = verifyColumn(column, candidate);
            double confidence = 1.0d - verification.failureRate();
            if (verification.failureRate() <= MAX_FAILURE_RATE && confidence >= MIN_CONFIDENCE)
            {
                if (fallbackVerification == null || verification.failureRate() < fallbackVerification.failureRate())
                {
                    fallbackFormat = candidate;
                    fallbackVerification = verification;
                }
            }
        }
        if (fallbackFormat != null)
        {
            return new ColumnInference(fallbackFormat, 1.0d - fallbackVerification.failureRate());
        }

        return new ColumnInference(DateFormat.Unknown, 0.0d);
    }

    public static DateFormat inferFormat(ColumnObject<String> column)
    {
        return inferColumn(column).format();
    }

    public static boolean isLikelyDate(CharSequence value)
    {
        return isLikelyDate(value, false);
    }

    public static boolean isLikelyDate(CharSequence value, boolean isDateName)
    {
        DateValueFacts valueFacts = DateValueFacts.from(value);
        if (valueFacts == null)
        {
            return false;
        }
        return !candidatesForValue(valueFacts, isDateName).isEmpty();
    }

    public static boolean isStrictInteger(CharSequence value)
    {
        if (value == null)
        {
            return false;
        }
        String s = value.toString().strip();
        int len = s.length();
        if (len == 0)
        {
            return false;
        }
        int i = 0;
        char c0 = s.charAt(0);
        if (c0 == '+' || c0 == '-')
        {
            i = 1;
            if (len == 1)
            {
                return false;
            }
        }
        for (; i < len; ++i)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                return false;
            }
        }
        return true;
    }

    public static boolean isStrictDecimal(CharSequence value)
    {
        if (value == null)
        {
            return false;
        }
        String s = value.toString().strip();
        int len = s.length();
        if (len == 0)
        {
            return false;
        }
        int i = 0;
        char c0 = s.charAt(0);
        if (c0 == '+' || c0 == '-')
        {
            i = 1;
            if (len == 1)
            {
                return false;
            }
        }

        boolean seenDot = false;
        boolean hasDigit = false;
        boolean hasDigitAfterDot = false;

        for (; i < len; ++i)
        {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9')
            {
                hasDigit = true;
                if (seenDot)
                {
                    hasDigitAfterDot = true;
                }
                continue;
            }
            if (c == '.')
            {
                if (seenDot)
                {
                    return false;
                }
                seenDot = true;
                continue;
            }
            return false;
        }

        if (!hasDigit)
        {
            return false;
        }
        if (!seenDot)
        {
            return true;
        }
        return hasDigitAfterDot;
    }

    private static DateFormat dominantFormat(DateFormat[] inferred, double[] confidence)
    {
        Map<DateFormat, Double> weight = new EnumMap<>(DateFormat.class);
        for (int i = 0; i < inferred.length; ++i)
        {
            DateFormat format = inferred[i];
            if (format == null || format == DateFormat.Unknown)
            {
                continue;
            }
            weight.merge(format, Double.valueOf(confidence[i]), Double::sum);
        }

        DateFormat best = null;
        double bestWeight = 0.0d;
        for (Map.Entry<DateFormat, Double> entry : weight.entrySet())
        {
            if (entry.getValue().doubleValue() > bestWeight)
            {
                bestWeight = entry.getValue().doubleValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static EnumSet<DateFormat> candidatesForValue(DateValueFacts valueFacts, boolean isDateName)
    {
        if (isExcelLocalDate(valueFacts))
        {
            return EnumSet.of(DateFormat.ExcelLocalDate);
        }
        if (isDateName && isExcelLocalDateTime(valueFacts))
        {
            return EnumSet.of(DateFormat.ExcelLocalDateTime);
        }
        if (valueFacts.invalidForDateTokens())
        {
            return EnumSet.noneOf(DateFormat.class);
        }

        EnumSet<DateFormat> candidates = EnumSet.noneOf(DateFormat.class);
        if (valueFacts.size() >= 8 && valueFacts.size() <= 11)
        {
            if (valueFacts.onlyDigits() && valueFacts.size() == 8)
            {
                if (isValidYyyyMmDd8(valueFacts))
                {
                    candidates.add(DateFormat.YMD);
                }
                return candidates;
            }
            if (valueFacts.alphaCount() == 0)
            {
                return numericCandidatesForSplitDate(valueFacts);
            }
            if (valueFacts.alphaCount() > 0)
            {
                return alphaMonthCandidatesForSplitDate(valueFacts);
            }
        }
        return candidates;
    }

    private static EnumSet<DateFormat> numericCandidatesForSplitDate(DateValueFacts facts)
    {
        String s = facts.text();
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
            return EnumSet.noneOf(DateFormat.class);
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
            return EnumSet.noneOf(DateFormat.class);
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
            return EnumSet.noneOf(DateFormat.class);
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
            return EnumSet.noneOf(DateFormat.class);
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
            return EnumSet.noneOf(DateFormat.class);
        }

        EnumSet<DateFormat> out = EnumSet.noneOf(DateFormat.class);

        if (isValidDate(normalizeYear(a), b, c))
        {
            out.add(DateFormat.YMD);
        }
        if (isValidDate(normalizeYear(c), b, a))
        {
            out.add(DateFormat.DMY);
        }
        if (isValidDate(normalizeYear(c), a, b))
        {
            out.add(DateFormat.MDY);
        }
        return out;
    }

    private static EnumSet<DateFormat> alphaMonthCandidatesForSplitDate(DateValueFacts facts)
    {
        String[] fields = facts.naturalDateSplit();
        if (fields == null || fields.length < 3)
        {
            return EnumSet.noneOf(DateFormat.class);
        }

        Integer day = parseUnsignedInt(fields[0]);
        Integer year = parseUnsignedInt(fields[2]);
        if (day == null || year == null)
        {
            return EnumSet.noneOf(DateFormat.class);
        }

        int month = parseMonthText(fields[1]);
        if (month <= 0)
        {
            return EnumSet.noneOf(DateFormat.class);
        }

        EnumSet<DateFormat> out = EnumSet.noneOf(DateFormat.class);
        if (isValidDate(normalizeYear(year.intValue()), month, day.intValue()))
        {
            out.add(DateFormat.DMY);
        }
        return out;
    }

    private static Integer parseUnsignedInt(String x)
    {
        if (x == null || x.isEmpty())
        {
            return null;
        }
        int value = 0;
        for (int i = 0; i < x.length(); ++i)
        {
            char c = x.charAt(i);
            if (c < '0' || c > '9')
            {
                return null;
            }
            value = (value * 10) + (c - '0');
        }
        return Integer.valueOf(value);
    }

    private static int parseMonthText(String token)
    {
        if (token == null || token.length() != 3)
        {
            return -1;
        }
        return switch (token.toUpperCase(Locale.UK))
        {
            case "JAN" -> 1;
            case "FEB" -> 2;
            case "MAR" -> 3;
            case "APR" -> 4;
            case "MAY" -> 5;
            case "JUN" -> 6;
            case "JUL" -> 7;
            case "AUG" -> 8;
            case "SEP" -> 9;
            case "OCT" -> 10;
            case "NOV" -> 11;
            case "DEC" -> 12;
            default -> -1;
        };
    }

    private static int normalizeYear(int year)
    {
        return year < 100 ? year + 2000 : year;
    }

    private static boolean isValidDate(int year, int month, int day)
    {
        if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > 31)
        {
            return false;
        }
        return day <= maxDayInMonth(year, month);
    }

    private static boolean isExcelLocalDate(DateValueFacts facts)
    {
        if (facts.size() != 5 || !facts.onlyDigits())
        {
            return false;
        }
        long serial = Long.parseLong(facts.text());
        return serial >= EXCEL_MIN && serial <= EXCEL_MAX;
    }

    private static boolean isExcelLocalDateTime(DateValueFacts facts)
    {
        if (!facts.isDecimal())
        {
            return false;
        }
        try
        {
            BigDecimal bd = new BigDecimal(facts.text());
            double d = bd.doubleValue();
            return d >= EXCEL_MIN && d <= EXCEL_MAX;
        } catch (NumberFormatException e)
        {
            return false;
        }
    }

    private static boolean isValidYyyyMmDd8(DateValueFacts facts)
    {
        String s = facts.text();
        int y = ((s.charAt(0) - '0') * 1000) + ((s.charAt(1) - '0') * 100) + ((s.charAt(2) - '0') * 10)
                + (s.charAt(3) - '0');
        int m = ((s.charAt(4) - '0') * 10) + (s.charAt(5) - '0');
        int d = ((s.charAt(6) - '0') * 10) + (s.charAt(7) - '0');
        if (m < 1 || m > 12 || d < 1 || d > 31)
        {
            return false;
        }
        int maxDay = maxDayInMonth(y, m);
        return d <= maxDay;
    }

    private static int maxDayInMonth(int year, int month)
    {
        return switch (month)
        {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11 -> 30;
            case 2 -> isLeapYear(year) ? 29 : 28;
            default -> 0;
        };
    }

    private static boolean isLeapYear(int year)
    {
        if ((year & 3) != 0)
        {
            return false;
        }
        if ((year % 100) != 0)
        {
            return true;
        }
        return (year % 400) == 0;
    }

    private record ColumnInference(DateFormat format, double confidence) {
    }

    private record CandidateRanking(DateFormat best, DateFormat second, double bestConfidence, double margin) {
    }

    private record Verification(int checked, int failed) {
        double failureRate()
        {
            if (checked <= 0)
            {
                return 1.0d;
            }
            return (double) failed / (double) checked;
        }
    }

    private static CandidateRanking rank(Map<DateFormat, Integer> votes, int sampledNonEmpty)
    {
        DateFormat best = null;
        DateFormat second = null;
        int bestVotes = -1;
        int secondVotes = -1;
        for (Map.Entry<DateFormat, Integer> entry : votes.entrySet())
        {
            int v = entry.getValue().intValue();
            if (v > bestVotes)
            {
                second = best;
                secondVotes = bestVotes;
                best = entry.getKey();
                bestVotes = v;
            } else if (v > secondVotes)
            {
                second = entry.getKey();
                secondVotes = v;
            }
        }

        if (best == null || bestVotes <= 0)
        {
            return new CandidateRanking(null, null, 0.0d, 0.0d);
        }
        double bestConfidence = (double) bestVotes / (double) sampledNonEmpty;
        double secondConfidence = secondVotes <= 0 ? 0.0d : (double) secondVotes / (double) sampledNonEmpty;
        return new CandidateRanking(best, second, bestConfidence, bestConfidence - secondConfidence);
    }

    private static List<DateFormat> rankAll(Map<DateFormat, Integer> votes)
    {
        return votes.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().intValue(), a.getValue().intValue()))
                .map(Map.Entry::getKey).toList();
    }

    private static Verification verifyColumn(ColumnObject<String> column, DateFormat format)
    {
        int checked = 0;
        int failed = 0;
        for (int i = 0; i < column.size(); ++i)
        {
            if (!column.isSet(i))
            {
                continue;
            }
            DateValueFacts valueFacts = DateValueFacts.from(column.get(i));
            if (valueFacts == null)
            {
                continue;
            }
            ++checked;
            if (!matchesFormat(valueFacts, format))
            {
                ++failed;
            }
        }
        return new Verification(checked, failed);
    }

    private static boolean matchesFormat(DateValueFacts valueFacts, DateFormat format)
    {
        if (format == null || format == DateFormat.Unknown || valueFacts == null)
        {
            return false;
        }

        if (format == DateFormat.ExcelLocalDate)
        {
            return isExcelLocalDate(valueFacts);
        }
        if (format == DateFormat.ExcelLocalDateTime)
        {
            return isExcelLocalDateTime(valueFacts);
        }
        if (valueFacts.invalidForDateTokens() || valueFacts.size() < 8 || valueFacts.size() > 11)
        {
            return false;
        }
        if (valueFacts.onlyDigits() && valueFacts.size() == 8)
        {
            return format == DateFormat.YMD && isValidYyyyMmDd8(valueFacts);
        }

        if (valueFacts.alphaCount() > 0)
        {
            String[] fields = valueFacts.naturalDateSplit();
            if (fields == null || fields.length < 3)
            {
                return false;
            }
            Integer first = parseUnsignedInt(fields[0]);
            Integer last = parseUnsignedInt(fields[2]);
            if (first == null || last == null)
            {
                return false;
            }
            int month = parseMonthText(fields[1]);
            if (month <= 0)
            {
                return false;
            }
            return switch (format)
            {
                case DMY -> isValidDate(normalizeYear(last.intValue()), month, first.intValue());
                case YMD -> isValidDate(normalizeYear(first.intValue()), month, last.intValue());
                default -> false;
            };
        }

        int[] abc = valueFacts.parseThreeNumberGroups();
        if (abc == null)
        {
            return false;
        }
        int a = abc[0];
        int b = abc[1];
        int c = abc[2];

        return switch (format)
        {
            case YMD -> isValidDate(normalizeYear(a), b, c);
            case DMY -> isValidDate(normalizeYear(c), b, a);
            case MDY -> isValidDate(normalizeYear(c), a, b);
            default -> false;
        };
    }

}
