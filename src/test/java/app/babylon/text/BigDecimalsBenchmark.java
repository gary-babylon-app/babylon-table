/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Small manual benchmark for decimal parsing. This is intentionally simple and
 * is meant for local comparison, not as a rigorous replacement for JMH.
 */
public final class BigDecimalsBenchmark
{
    private static final int VALUE_COUNT = 500_000;
    private static final int RUNS = 6;
    private static final int WARMUP = 20;
    private static final int ITERATIONS = 40;

    private BigDecimalsBenchmark()
    {
    }

    public static void main(String[] args)
    {
        List<String> decimals = decimals();
        List<String> positiveDecimals = positiveDecimals(decimals);
        List<String> positiveCommaThousandsDecimals = commaThousandsDecimals(positiveDecimals);
        List<String> positiveCommaLengthDecimals = replaceCommasWithDigits(positiveCommaThousandsDecimals);
        List<String> commaThousandsDecimals = commaThousandsDecimals(decimals);
        List<String> noTrailingZeroDecimals = noTrailingZeroDecimals();
        List<String> trailingZeroDecimals = trailingZeroDecimals();
        List<String> bracketedDecimals = bracketedDecimals(decimals);
        List<String> leadingPoundDecimals = leadingPoundDecimals(decimals);
        List<String> percentDecimals = percentDecimals(decimals);
        List<String> commonDecimals = commonDecimals(decimals);

        benchmark("Plain decimals", decimals, RUNS);
        benchmark("No trailing zero decimals", noTrailingZeroDecimals, RUNS);
        benchmarkDirectStrip("No trailing zero decimals with stripTrailingZeros", noTrailingZeroDecimals, RUNS);
        benchmarkDirectStrip("Positive plain decimals with stripTrailingZeros", positiveDecimals, RUNS);
        benchmarkDirectStrip("Trailing zero decimals with stripTrailingZeros", trailingZeroDecimals, RUNS);
        benchmarkCommon("Positive plain decimals", positiveDecimals, RUNS);
        benchmarkCommon("Positive comma-length digit decimals", positiveCommaLengthDecimals, RUNS);
        benchmarkCommon("Positive comma thousands decimals", positiveCommaThousandsDecimals, RUNS);
        benchmarkCommon("Comma thousands decimals", commaThousandsDecimals, RUNS);
        benchmarkBracketed("Bracketed decimals", bracketedDecimals, RUNS);
        benchmarkCommon("Leading pound decimals", leadingPoundDecimals, RUNS);
        benchmarkCommon("Percent decimals", percentDecimals, RUNS);
        benchmarkCommon("Common wrapped decimals", commonDecimals, RUNS);
    }

    private static void benchmark(String label, List<String> decimals, int runs)
    {
        long directWarmup = runDirect(decimals, WARMUP);
        long isDecimalWarmup = runIsDecimalThenDirect(decimals, WARMUP);
        long parse2Warmup = runParse2(decimals, WARMUP);
        long parseSliceWarmup = runParseSlice(decimals, WARMUP);

        System.out.printf("%s%n", label);
        System.out.printf("  warmup direct=%d isDecimal=%d parse2=%d parseSlice=%d%n", directWarmup, isDecimalWarmup,
                parse2Warmup, parseSliceWarmup);
        for (int run = 1; run <= runs; ++run)
        {
            runMeasuredRound(run, decimals);
        }
        System.out.println();
    }

    private static void runMeasuredRound(int run, List<String> decimals)
    {
        long direct;
        long isDecimal;
        long parse2;
        long parseSlice;
        if ((run & 1) == 0)
        {
            parseSlice = runParseSlice(decimals, ITERATIONS);
            parse2 = runParse2(decimals, ITERATIONS);
            isDecimal = runIsDecimalThenDirect(decimals, ITERATIONS);
            direct = runDirect(decimals, ITERATIONS);
        }
        else
        {
            direct = runDirect(decimals, ITERATIONS);
            isDecimal = runIsDecimalThenDirect(decimals, ITERATIONS);
            parse2 = runParse2(decimals, ITERATIONS);
            parseSlice = runParseSlice(decimals, ITERATIONS);
        }

        long operations = (long) decimals.size() * ITERATIONS;
        double directNanos = (double) direct / operations;
        double isDecimalNanos = (double) isDecimal / operations;
        double parse2Nanos = (double) parse2 / operations;
        double parseSliceNanos = (double) parseSlice / operations;

        System.out.printf("  run %d direct=%.2f ns/op isDecimal=%.2f ns/op parse2=%.2f ns/op parseSlice=%.2f ns/op",
                run, directNanos, isDecimalNanos, parse2Nanos, parseSliceNanos);
        System.out.printf(" ratios isDecimal=%.2fx parse2=%.2fx parseSlice=%.2fx%n", isDecimalNanos / directNanos,
                parse2Nanos / directNanos, parseSliceNanos / directNanos);
    }

    private static List<String> decimals()
    {
        List<String> values = new ArrayList<>(VALUE_COUNT);
        for (int i = 0; i < VALUE_COUNT; ++i)
        {
            long whole = 10_000L + i * 17L;
            int decimal = i % 100_000;
            switch (i & 7)
            {
                case 0 -> values.add("%d.%02d".formatted(whole, decimal % 100));
                case 1 -> values.add("-%d.%02d".formatted(whole, decimal % 100));
                case 2 -> values.add("%d.%04d".formatted(whole, decimal % 10_000));
                case 3 -> values.add("-%d.%04d".formatted(whole, decimal % 10_000));
                case 4 -> values.add("%d".formatted(whole));
                case 5 -> values.add("-%d".formatted(whole));
                case 6 -> values.add("%d.%05d".formatted(whole, decimal));
                default -> values.add("-%d.%05d".formatted(whole, decimal));
            }
        }
        return List.copyOf(values);
    }

    private static List<String> noTrailingZeroDecimals()
    {
        List<String> values = new ArrayList<>(VALUE_COUNT);
        for (int i = 0; i < VALUE_COUNT; ++i)
        {
            long whole = 10_000L + i * 17L;
            int decimal = i % 10_000;
            decimal = decimal / 10 * 10 + (i % 9 + 1);
            values.add("%d.%04d".formatted(whole, decimal));
        }
        return List.copyOf(values);
    }

    private static List<String> trailingZeroDecimals()
    {
        List<String> values = new ArrayList<>(VALUE_COUNT);
        for (int i = 0; i < VALUE_COUNT; ++i)
        {
            long whole = 10_000L + i * 17L;
            int decimal = i % 10_000;
            values.add("%d.%04d0000".formatted(whole, decimal));
        }
        return List.copyOf(values);
    }

    private static List<String> bracketedDecimals(List<String> decimals)
    {
        List<String> values = new ArrayList<>(decimals.size());
        for (String value : decimals)
        {
            values.add("(" + stripLeadingHyphen(value) + ")");
        }
        return List.copyOf(values);
    }

    private static List<String> positiveDecimals(List<String> decimals)
    {
        List<String> values = new ArrayList<>(decimals.size());
        for (String value : decimals)
        {
            values.add(stripLeadingHyphen(value));
        }
        return List.copyOf(values);
    }

    private static List<String> commaThousandsDecimals(List<String> decimals)
    {
        List<String> values = new ArrayList<>(decimals.size());
        for (String value : decimals)
        {
            String unsigned = stripLeadingHyphen(value);
            int dot = unsigned.indexOf('.');
            String whole = dot < 0 ? unsigned : unsigned.substring(0, dot);
            String decimal = dot < 0 ? "" : unsigned.substring(dot);
            values.add(groupThousands(whole) + decimal);
        }
        return List.copyOf(values);
    }

    private static List<String> replaceCommasWithDigits(List<String> decimals)
    {
        List<String> values = new ArrayList<>(decimals.size());
        for (String value : decimals)
        {
            values.add(value.replace(',', '1'));
        }
        return List.copyOf(values);
    }

    private static String groupThousands(String value)
    {
        int firstGroup = value.length() % 3;
        if (firstGroup == 0)
        {
            firstGroup = 3;
        }
        StringBuilder grouped = new StringBuilder(value.length() + value.length() / 3);
        grouped.append(value, 0, firstGroup);
        for (int i = firstGroup; i < value.length(); i += 3)
        {
            grouped.append(',');
            grouped.append(value, i, i + 3);
        }
        return grouped.toString();
    }

    private static List<String> leadingPoundDecimals(List<String> decimals)
    {
        List<String> values = new ArrayList<>(decimals.size());
        for (String value : decimals)
        {
            values.add("£" + stripLeadingHyphen(value));
        }
        return List.copyOf(values);
    }

    private static List<String> percentDecimals(List<String> decimals)
    {
        List<String> values = new ArrayList<>(decimals.size());
        for (String value : decimals)
        {
            values.add(stripLeadingHyphen(value) + "%");
        }
        return List.copyOf(values);
    }

    private static List<String> commonDecimals(List<String> decimals)
    {
        List<String> values = new ArrayList<>(decimals.size());
        for (int i = 0; i < decimals.size(); ++i)
        {
            String value = stripLeadingHyphen(decimals.get(i));
            switch (i % 8)
            {
                case 0 -> values.add("R" + value);
                case 1 -> values.add(value + "R");
                case 2 -> values.add(value + "%");
                case 3 -> values.add("R" + value + "%");
                case 4 -> values.add("(" + value + ")");
                case 5 -> values.add("R(" + value + ")");
                case 6 -> values.add("(R" + value + ")");
                default -> values.add("(" + value + ")R");
            }
        }
        return List.copyOf(values);
    }

    private static String stripLeadingHyphen(String value)
    {
        return value.charAt(0) == '-' ? value.substring(1) : value;
    }

    private static void benchmarkBracketed(String label, List<String> decimals, int runs)
    {
        long exceptionWarmup = runExceptionThenInnerDirect(decimals, WARMUP);
        long parse2Warmup = runParse2(decimals, WARMUP);
        long parseSliceWarmup = runParseSlice(decimals, WARMUP);

        System.out.printf("%s%n", label);
        System.out.printf("  warmup exception=%d parse2=%d parseSlice=%d%n", exceptionWarmup, parse2Warmup,
                parseSliceWarmup);
        for (int run = 1; run <= runs; ++run)
        {
            runBracketedMeasuredRound(run, decimals);
        }
        System.out.println();
    }

    private static void benchmarkDirectStrip(String label, List<String> decimals, int runs)
    {
        long directStripWarmup = runDirectStripTrailingZeros(decimals, WARMUP);
        long parseSliceWarmup = runParseSlice(decimals, WARMUP);

        System.out.printf("%s%n", label);
        System.out.printf("  warmup directStrip=%d parseSlice=%d%n", directStripWarmup, parseSliceWarmup);
        for (int run = 1; run <= runs; ++run)
        {
            runDirectStripMeasuredRound(run, decimals);
        }
        System.out.println();
    }

    private static void runDirectStripMeasuredRound(int run, List<String> decimals)
    {
        long directStrip;
        long parseSlice;
        if ((run & 1) == 0)
        {
            parseSlice = runParseSlice(decimals, ITERATIONS);
            directStrip = runDirectStripTrailingZeros(decimals, ITERATIONS);
        }
        else
        {
            directStrip = runDirectStripTrailingZeros(decimals, ITERATIONS);
            parseSlice = runParseSlice(decimals, ITERATIONS);
        }

        long operations = (long) decimals.size() * ITERATIONS;
        double directStripNanos = (double) directStrip / operations;
        double parseSliceNanos = (double) parseSlice / operations;

        System.out.printf("  run %d directStrip=%.2f ns/op parseSlice=%.2f ns/op", run, directStripNanos,
                parseSliceNanos);
        System.out.printf(" ratio parseSlice=%.2fx%n", parseSliceNanos / directStripNanos);
    }

    private static void benchmarkCommon(String label, List<String> decimals, int runs)
    {
        long parse2Warmup = runParse2(decimals, WARMUP);
        long parseSliceWarmup = runParseSlice(decimals, WARMUP);

        System.out.printf("%s%n", label);
        System.out.printf("  warmup parse2=%d parseSlice=%d%n", parse2Warmup, parseSliceWarmup);
        for (int run = 1; run <= runs; ++run)
        {
            runCommonMeasuredRound(run, decimals);
        }
        System.out.println();
    }

    private static void runCommonMeasuredRound(int run, List<String> decimals)
    {
        long parse2;
        long parseSlice;
        if ((run & 1) == 0)
        {
            parseSlice = runParseSlice(decimals, ITERATIONS);
            parse2 = runParse2(decimals, ITERATIONS);
        }
        else
        {
            parse2 = runParse2(decimals, ITERATIONS);
            parseSlice = runParseSlice(decimals, ITERATIONS);
        }

        long operations = (long) decimals.size() * ITERATIONS;
        double parse2Nanos = (double) parse2 / operations;
        double parseSliceNanos = (double) parseSlice / operations;

        System.out.printf("  run %d parse2=%.2f ns/op parseSlice=%.2f ns/op", run, parse2Nanos, parseSliceNanos);
        System.out.printf(" ratio parseSlice=%.2fx%n", parseSliceNanos / parse2Nanos);
    }

    private static void runBracketedMeasuredRound(int run, List<String> decimals)
    {
        long exception;
        long parse2;
        long parseSlice;
        if ((run & 1) == 0)
        {
            parseSlice = runParseSlice(decimals, ITERATIONS);
            parse2 = runParse2(decimals, ITERATIONS);
            exception = runExceptionThenInnerDirect(decimals, ITERATIONS);
        }
        else
        {
            exception = runExceptionThenInnerDirect(decimals, ITERATIONS);
            parse2 = runParse2(decimals, ITERATIONS);
            parseSlice = runParseSlice(decimals, ITERATIONS);
        }

        long operations = (long) decimals.size() * ITERATIONS;
        double exceptionNanos = (double) exception / operations;
        double parse2Nanos = (double) parse2 / operations;
        double parseSliceNanos = (double) parseSlice / operations;

        System.out.printf("  run %d exception+inner=%.2f ns/op parse2=%.2f ns/op parseSlice=%.2f ns/op", run,
                exceptionNanos, parse2Nanos, parseSliceNanos);
        System.out.printf(" ratios parse2=%.2fx parseSlice=%.2fx%n", parse2Nanos / exceptionNanos,
                parseSliceNanos / exceptionNanos);
    }

    private static long runDirect(List<String> decimals, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            for (String value : decimals)
            {
                sink += new BigDecimal(value).scale();
            }
        }
        return elapsed(start, sink);
    }

    private static long runDirectStripTrailingZeros(List<String> decimals, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            for (String value : decimals)
            {
                sink += new BigDecimal(value).stripTrailingZeros().scale();
            }
        }
        return elapsed(start, sink);
    }

    private static long runParse2(List<String> decimals, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            for (String value : decimals)
            {
                BigDecimal decimal = BigDecimals.parse2(value, 0, value.length());
                sink += decimal == null ? 0 : decimal.scale();
            }
        }
        return elapsed(start, sink);
    }

    private static long runParseSlice(List<String> decimals, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            for (String value : decimals)
            {
                BigDecimal decimal = BigDecimals.parse(value, 0, value.length());
                sink += decimal == null ? 0 : decimal.scale();
            }
        }
        return elapsed(start, sink);
    }

    private static long runIsDecimalThenDirect(List<String> decimals, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            for (String value : decimals)
            {
                BigDecimal decimal = BigDecimals.isDecimal(value) ? new BigDecimal(value) : null;
                sink += decimal == null ? 0 : decimal.scale();
            }
        }
        return elapsed(start, sink);
    }

    private static long runExceptionThenInnerDirect(List<String> decimals, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            for (String value : decimals)
            {
                BigDecimal decimal;
                try
                {
                    decimal = new BigDecimal(value);
                }
                catch (NumberFormatException e)
                {
                    char[] chars = value.toCharArray();
                    decimal = new BigDecimal(chars, 1, chars.length - 2);
                }
                sink += decimal.scale();
            }
        }
        return elapsed(start, sink);
    }

    private static long elapsed(long startNanos, long sink)
    {
        long elapsed = System.nanoTime() - startNanos;
        if (sink == Long.MIN_VALUE)
        {
            System.out.println("Impossible sink: " + sink);
        }
        return elapsed;
    }
}
