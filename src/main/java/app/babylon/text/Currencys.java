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
 * Fast currency parsing helpers optimized for common major three-letter codes.
 *
 * <p>
 * The hot path avoids intermediate {@link String} allocation for the built-in
 * fast set of uppercase currency codes, then falls back to a trimmed and
 * normalized parse for less common inputs.
 */
public final class Currencys
{
    private Currencys()
    {
    }

    public static Currency parse(CharSequence s)
    {
        return s == null ? null : parse(s, 0, s.length());
    }

    /**
     * Parses a character slice into a {@link Currency}.
     *
     * <p>
     * The fast path is optimised for common major currencies already presented as
     * three uppercase ASCII letters. Less common or messier inputs fall back to a
     * normalised parse using {@link Currency#getInstance(String)}.
     *
     * @param s
     *            the source text
     * @param offset
     *            the start offset
     * @param length
     *            the slice length
     * @return the parsed currency, or {@code null} when parsing fails
     */
    public static Currency parse(CharSequence s, int offset, int length)
    {
        if (s == null)
        {
            return null;
        }
        if (length < 3)
        {
            return null;
        }

        if (length == 3)
        {
            final int exactKey = packedKey(s.charAt(offset), s.charAt(offset + 1), s.charAt(offset + 2));
            Currency fast = switch (exactKey)
            {
                case PackedCurrencyKeys.EUR -> PackedCurrencies.EUR;
                case PackedCurrencyKeys.USD -> PackedCurrencies.USD;
                case PackedCurrencyKeys.GBP -> PackedCurrencies.GBP;
                case PackedCurrencyKeys.CAD -> PackedCurrencies.CAD;
                case PackedCurrencyKeys.AUD -> PackedCurrencies.AUD;
                case PackedCurrencyKeys.NZD -> PackedCurrencies.NZD;
                case PackedCurrencyKeys.JPY -> PackedCurrencies.JPY;
                case PackedCurrencyKeys.CHF -> PackedCurrencies.CHF;
                case PackedCurrencyKeys.SEK -> PackedCurrencies.SEK;
                case PackedCurrencyKeys.NOK -> PackedCurrencies.NOK;
                case PackedCurrencyKeys.DKK -> PackedCurrencies.DKK;
                case PackedCurrencyKeys.SGD -> PackedCurrencies.SGD;
                case PackedCurrencyKeys.HKD -> PackedCurrencies.HKD;
                case PackedCurrencyKeys.CNY -> PackedCurrencies.CNY;
                case PackedCurrencyKeys.ZAR -> PackedCurrencies.ZAR;
                default -> null;
            };
            if (fast != null)
            {
                return fast;
            }

            char c0 = s.charAt(offset);
            char c1 = s.charAt(offset + 1);
            char c2 = s.charAt(offset + 2);
            if (!Character.isWhitespace(c0) && !Character.isWhitespace(c2))
            {
                c0 = asciiUpper(c0);
                c1 = asciiUpper(c1);
                c2 = asciiUpper(c2);
                final int upperKey = packedKey(c0, c1, c2);
                return switch (upperKey)
                {
                    case PackedCurrencyKeys.EUR -> PackedCurrencies.EUR;
                    case PackedCurrencyKeys.USD -> PackedCurrencies.USD;
                    case PackedCurrencyKeys.GBP -> PackedCurrencies.GBP;
                    case PackedCurrencyKeys.CAD -> PackedCurrencies.CAD;
                    case PackedCurrencyKeys.AUD -> PackedCurrencies.AUD;
                    case PackedCurrencyKeys.NZD -> PackedCurrencies.NZD;
                    case PackedCurrencyKeys.JPY -> PackedCurrencies.JPY;
                    case PackedCurrencyKeys.CHF -> PackedCurrencies.CHF;
                    case PackedCurrencyKeys.SEK -> PackedCurrencies.SEK;
                    case PackedCurrencyKeys.NOK -> PackedCurrencies.NOK;
                    case PackedCurrencyKeys.DKK -> PackedCurrencies.DKK;
                    case PackedCurrencyKeys.SGD -> PackedCurrencies.SGD;
                    case PackedCurrencyKeys.HKD -> PackedCurrencies.HKD;
                    case PackedCurrencyKeys.CNY -> PackedCurrencies.CNY;
                    case PackedCurrencyKeys.ZAR -> PackedCurrencies.ZAR;
                    default -> parseCurrencyFallback(c0, c1, c2);
                };
            }
        }

        int trimmedOffset = offset;
        int trimmedLength = length;
        while (trimmedLength > 0 && Character.isWhitespace(s.charAt(trimmedOffset)))
        {
            ++trimmedOffset;
            --trimmedLength;
        }
        while (trimmedLength > 0 && Character.isWhitespace(s.charAt(trimmedOffset + trimmedLength - 1)))
        {
            --trimmedLength;
        }
        if (trimmedLength == 0)
        {
            return null;
        }

        if (trimmedLength == 3)
        {
            char c0 = asciiUpper(s.charAt(trimmedOffset));
            char c1 = asciiUpper(s.charAt(trimmedOffset + 1));
            char c2 = asciiUpper(s.charAt(trimmedOffset + 2));
            final int normalisedKey = packedKey(c0, c1, c2);
            Currency normalised = switch (normalisedKey)
            {
                case PackedCurrencyKeys.EUR -> PackedCurrencies.EUR;
                case PackedCurrencyKeys.USD -> PackedCurrencies.USD;
                case PackedCurrencyKeys.GBP -> PackedCurrencies.GBP;
                case PackedCurrencyKeys.CAD -> PackedCurrencies.CAD;
                case PackedCurrencyKeys.AUD -> PackedCurrencies.AUD;
                case PackedCurrencyKeys.NZD -> PackedCurrencies.NZD;
                case PackedCurrencyKeys.JPY -> PackedCurrencies.JPY;
                case PackedCurrencyKeys.CHF -> PackedCurrencies.CHF;
                case PackedCurrencyKeys.SEK -> PackedCurrencies.SEK;
                case PackedCurrencyKeys.NOK -> PackedCurrencies.NOK;
                case PackedCurrencyKeys.DKK -> PackedCurrencies.DKK;
                case PackedCurrencyKeys.SGD -> PackedCurrencies.SGD;
                case PackedCurrencyKeys.HKD -> PackedCurrencies.HKD;
                case PackedCurrencyKeys.CNY -> PackedCurrencies.CNY;
                case PackedCurrencyKeys.ZAR -> PackedCurrencies.ZAR;
                default -> parseCurrencyFallback(c0, c1, c2);
            };
            if (normalised != null)
            {
                return normalised;
            }
        }
        return null;
    }

    private static Currency parseCurrencyFallback(char c0, char c1, char c2)
    {
        try
        {
            return Currency.getInstance(new String(new char[]
            {c0, c1, c2}));
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    private static int packedKey(char c0, char c1, char c2)
    {
        return (c0 << 16) | (c1 << 8) | c2;
    }

    private static char asciiUpper(char c)
    {
        return c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c;
    }

    private static final class PackedCurrencies
    {
        private static final Currency EUR = Currency.getInstance("EUR");
        private static final Currency USD = Currency.getInstance("USD");
        private static final Currency GBP = Currency.getInstance("GBP");
        private static final Currency CAD = Currency.getInstance("CAD");
        private static final Currency AUD = Currency.getInstance("AUD");
        private static final Currency NZD = Currency.getInstance("NZD");
        private static final Currency JPY = Currency.getInstance("JPY");
        private static final Currency CHF = Currency.getInstance("CHF");
        private static final Currency SEK = Currency.getInstance("SEK");
        private static final Currency NOK = Currency.getInstance("NOK");
        private static final Currency DKK = Currency.getInstance("DKK");
        private static final Currency SGD = Currency.getInstance("SGD");
        private static final Currency HKD = Currency.getInstance("HKD");
        private static final Currency CNY = Currency.getInstance("CNY");
        private static final Currency ZAR = Currency.getInstance("ZAR");

        private PackedCurrencies()
        {
        }
    }

    private static final class PackedCurrencyKeys
    {
        private static final int EUR = ('E' << 16) | ('U' << 8) | 'R';
        private static final int USD = ('U' << 16) | ('S' << 8) | 'D';
        private static final int GBP = ('G' << 16) | ('B' << 8) | 'P';
        private static final int CAD = ('C' << 16) | ('A' << 8) | 'D';
        private static final int AUD = ('A' << 16) | ('U' << 8) | 'D';
        private static final int NZD = ('N' << 16) | ('Z' << 8) | 'D';
        private static final int JPY = ('J' << 16) | ('P' << 8) | 'Y';
        private static final int CHF = ('C' << 16) | ('H' << 8) | 'F';
        private static final int SEK = ('S' << 16) | ('E' << 8) | 'K';
        private static final int NOK = ('N' << 16) | ('O' << 8) | 'K';
        private static final int DKK = ('D' << 16) | ('K' << 8) | 'K';
        private static final int SGD = ('S' << 16) | ('G' << 8) | 'D';
        private static final int HKD = ('H' << 16) | ('K' << 8) | 'D';
        private static final int CNY = ('C' << 16) | ('N' << 8) | 'Y';
        private static final int ZAR = ('Z' << 16) | ('A' << 8) | 'R';

        private PackedCurrencyKeys()
        {
        }
    }
}
