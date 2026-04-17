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

import java.util.Currency;

/**
 * Small manual benchmark for currency parsing. This is intentionally simple and
 * is meant for local comparison, not as a rigorous replacement for JMH.
 */
public final class CurrencysBenchmark
{
    private static final int WARMUP = 2_000_000;
    private static final int ITERATIONS = 10_000_000;

    private CurrencysBenchmark()
    {
    }

    public static void main(String[] args)
    {
        benchmark("Fast exact USD", "xxUSDyy", 2, 3);
        benchmark("Fast lowercase zar", "xxzaryy", 2, 3);
        benchmark("Trimmed usd", "xx usd yy", 2, 5);
        benchmark("Fallback SEK", "xxSEKyy", 2, 3);
        benchmark("Fallback ISK", "xxISKyy", 2, 3);
    }

    private static void benchmark(String label, String source, int offset, int length)
    {
        long fastWarmup = runFast(source, offset, length, WARMUP);
        long naiveWarmup = runNaive(source, offset, length, WARMUP);
        long fast = runFast(source, offset, length, ITERATIONS);
        long naive = runNaive(source, offset, length, ITERATIONS);

        double fastNanos = (double) fast / ITERATIONS;
        double naiveNanos = (double) naive / ITERATIONS;

        System.out.printf("%s%n", label);
        System.out.printf("  fast  : %.2f ns/op (warmup=%d)%n", fastNanos, fastWarmup);
        System.out.printf("  naive : %.2f ns/op (warmup=%d)%n", naiveNanos, naiveWarmup);
        System.out.printf("  ratio : %.2fx%n%n", naiveNanos / fastNanos);
    }

    private static long runFast(String source, int offset, int length, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            Currency currency = Currencys.parse(source, offset, length);
            sink += currency == null ? 0 : currency.getCurrencyCode().charAt(0);
        }
        return elapsed(start, sink);
    }

    private static long runNaive(String source, int offset, int length, int iterations)
    {
        long sink = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i)
        {
            Currency currency = parseNaive(source, offset, length);
            sink += currency == null ? 0 : currency.getCurrencyCode().charAt(0);
        }
        return elapsed(start, sink);
    }

    private static Currency parseNaive(String source, int offset, int length)
    {
        if (source == null || length < 3)
        {
            return null;
        }

        String text = source.substring(offset, offset + length).strip().toUpperCase();
        if (text.length() != 3)
        {
            return null;
        }
        try
        {
            return Currency.getInstance(text);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
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
