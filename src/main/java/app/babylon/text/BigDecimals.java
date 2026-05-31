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
import java.math.BigInteger;
import java.math.MathContext;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Parsing and extraction helpers for decimal text values.
 */
public final class BigDecimals
{
    private static final int INVALID_INDEX = -2;

    /**
     * Policy used to resolve ambiguous decimal separators.
     * <p>
     * A decimal is ambiguous when the text contains exactly one comma or period,
     * that separator is followed by exactly three digits, and no other separator
     * signal is present. For example {@code 100,379} may mean either {@code 100379}
     * or {@code 100.379}. A grouping space, non-breaking space, narrow non-breaking
     * space, or a second separator removes the ambiguity.
     */
    public enum DecimalPolicy
    {
        /**
         * Infer separators from the text. When a single comma is ambiguous, treats it
         * as a grouping separator. A single ambiguous period is treated as a decimal
         * separator.
         */
        AUTO,

        /**
         * Treat period as the decimal separator and comma as a grouping separator.
         */
        PERIOD_DECIMAL,

        /**
         * Treat comma as the decimal separator and period as a grouping separator.
         */
        COMMA_DECIMAL;

        /**
         * Returns a decimal policy from the decimal separator used by the supplied
         * locale.
         *
         * @param locale
         *            locale to inspect
         * @return locale-derived decimal policy, or {@link #AUTO} when no comma or
         *         period decimal separator is defined
         */
        public static DecimalPolicy fromLocale(Locale locale)
        {
            if (locale == null)
            {
                return AUTO;
            }
            char decimalSeparator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
            if (decimalSeparator == ',')
            {
                return COMMA_DECIMAL;
            }
            if (decimalSeparator == '.')
            {
                return PERIOD_DECIMAL;
            }
            return AUTO;
        }
    }

    public static final class ParsedDecimal
    {
        private final boolean negative;
        private final long high;
        private final long low;
        private final int scale;
        private BigDecimal bigDecimal;

        private ParsedDecimal(boolean negative, long high, long low, int scale)
        {
            this.negative = negative;
            this.high = high;
            this.low = low;
            this.scale = scale;
        }

        public int scale()
        {
            return scale;
        }

        public boolean isNegative()
        {
            return negative;
        }

        public long high()
        {
            return high;
        }

        public long low()
        {
            return low;
        }

        public boolean isCompact()
        {
            return high == 0L && low >= 0L;
        }

        public BigDecimal toBigDecimal()
        {
            BigDecimal bd = bigDecimal;
            if (bd == null)
            {
                bd = isCompact()
                        ? BigDecimal.valueOf(negative ? -low : low, scale)
                        : new BigDecimal(toBigInteger(), scale);
                bigDecimal = bd;
            }
            return bd;
        }

        private BigInteger toBigInteger()
        {
            byte[] bytes = new byte[16];
            putLong(bytes, 0, high);
            putLong(bytes, 8, low);
            BigInteger value = new BigInteger(1, bytes);
            return negative ? value.negate() : value;
        }

        private static void putLong(byte[] bytes, int offset, long value)
        {
            for (int i = 7; i >= 0; --i)
            {
                bytes[offset + i] = (byte) value;
                value >>>= 8;
            }
        }
    }

    /**
     * Prepared decimal text together with formatting flags discovered during
     * normalisation.
     */
    public static final class PreparedDecimal
    {
        private final String normalizedNumberText;
        private final boolean percent;
        private final boolean negativeBracket;

        private PreparedDecimal(String normalizedNumberText, boolean percent, boolean negativeBracket)
        {
            this.normalizedNumberText = normalizedNumberText;
            this.percent = percent;
            this.negativeBracket = negativeBracket;
        }

        /**
         * Returns the normalised decimal text ready for parsing.
         *
         * @return normalised decimal text
         */
        public String normalizedNumberText()
        {
            return this.normalizedNumberText;
        }

        /**
         * Returns whether the original text used a percent sign.
         *
         * @return {@code true} when the original value was a percentage
         */
        public boolean isPercent()
        {
            return this.percent;
        }

        /**
         * Returns whether the original text used bracketed negatives.
         *
         * @return {@code true} when the original value was wrapped in brackets
         */
        public boolean isNegativeBracket()
        {
            return this.negativeBracket;
        }
    }

    private BigDecimals()
    {
    }

    /**
     * Returns whether the supplied text could represent a decimal.
     *
     * @param s
     *            text to inspect
     * @return {@code true} when the text can be normalised as a decimal
     */
    public static boolean isDecimal(CharSequence s)
    {
        if (s == null)
        {
            return false;
        }
        Classifier raw = new Classifier(s);
        if (raw.hasSpaces())
        {
            s = Strings.stripx(s);
        }
        if (Strings.isEmpty(s))
        {
            return false;
        }
        Classifier cc = new Classifier(s);
        if (cc.cannotBeDecimal())
        {
            return false;
        }
        s = cc.cleanUp();
        return !Strings.isEmpty(s);
    }

    /**
     * Removes commas from a simple numeric string, leaving other text unchanged.
     *
     * @param s
     *            text to normalise
     * @return comma-free text or the original value when no safe cleanup applies
     */
    public static String removeCommas(String s)
    {
        if (s == null)
        {
            return null;
        }
        int countCleanChars = 0;
        char[] clean = new char[s.length()];
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (c == ',')
            {
                continue;
            }
            if (isSupportedDecimalChar(c))
            {
                clean[countCleanChars++] = c;
            }
            else
            {
                return s;
            }
        }
        if (countCleanChars == s.length())
        {
            return s;
        }
        return new String(clean, 0, countCleanChars);
    }

    /**
     * Parses a decimal value from the supplied text.
     *
     * @param s
     *            text to parse
     * @return parsed decimal or {@code null}
     */
    public static BigDecimal parse(CharSequence s)
    {
        if (s == null)
        {
            return null;
        }
        return parsePrepared(prepare(s.toString()));
    }

    /**
     * Parses a decimal value from a character slice.
     *
     * @param s
     *            text to parse
     * @param start
     *            inclusive start index
     * @param end
     *            exclusive end index
     * @return parsed decimal or {@code null}
     */
    public static BigDecimal parse(CharSequence s, int start, int end)
    {
        if (s == null || start < 0 || end > s.length() || start >= end)
        {
            return null;
        }
        char first = s.charAt(start);
        char last = s.charAt(end - 1);
        if ((isDigit(first) || first == '-') && isDigit(last))
        {
            return parsePlainDecimalFast(s, start, end, false, 0);
        }
        boolean negative = false;
        boolean percent = false;
        if (last == '%')
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            percent = true;
            first = s.charAt(start);
            last = s.charAt(end - 1);
        }
        if (isCurrencySymbol(first))
        {
            ++start;
            if (start >= end)
            {
                return null;
            }
            first = s.charAt(start);
            last = s.charAt(end - 1);
        }
        else if (isCurrencySymbol(last))
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            first = s.charAt(start);
            last = s.charAt(end - 1);
        }
        if (first == '(' && last == ')')
        {
            ++start;
            --end;
            if (start >= end)
            {
                return null;
            }
            negative = true;
            first = s.charAt(start);
            last = s.charAt(end - 1);
        }
        if (isCurrencySymbol(first))
        {
            ++start;
            if (start >= end)
            {
                return null;
            }
            first = s.charAt(start);
        }
        else if (isCurrencySymbol(last))
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            last = s.charAt(end - 1);
        }
        char coreLast = s.charAt(end - 1);
        if ((isDigit(first) || first == '-' || first == '.') && (isDigit(coreLast) || coreLast == '.'))
        {
            return parsePlainDecimalFast(s, start, end, negative, percent ? 2 : 0);
        }
        return null;
    }

    public static BigDecimal parse(char[] chars, int start, int end)
    {
        if (chars == null || start < 0 || end > chars.length || start >= end)
        {
            return null;
        }
        char first = chars[start];
        char last = chars[end - 1];
        if ((isDigit(first) || first == '-') && isDigit(last))
        {
            return parsePlainDecimalFast(chars, start, end, false, 0);
        }
        boolean negative = false;
        boolean percent = false;
        if (last == '%')
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            percent = true;
            first = chars[start];
            last = chars[end - 1];
        }
        if (isCurrencySymbol(first))
        {
            ++start;
            if (start >= end)
            {
                return null;
            }
            first = chars[start];
            last = chars[end - 1];
        }
        else if (isCurrencySymbol(last))
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            first = chars[start];
            last = chars[end - 1];
        }
        if (first == '(' && last == ')')
        {
            ++start;
            --end;
            if (start >= end)
            {
                return null;
            }
            negative = true;
            first = chars[start];
            last = chars[end - 1];
        }
        if (isCurrencySymbol(first))
        {
            ++start;
            if (start >= end)
            {
                return null;
            }
            first = chars[start];
        }
        else if (isCurrencySymbol(last))
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            last = chars[end - 1];
        }
        char coreLast = chars[end - 1];
        if ((isDigit(first) || first == '-' || first == '.') && (isDigit(coreLast) || coreLast == '.'))
        {
            return parsePlainDecimalFast(chars, start, end, negative, percent ? 2 : 0);
        }
        return null;
    }

    /**
     * Parses a decimal value directly from a byte sequence slice.
     * <p>
     * This method is intended for high-throughput tabular input, especially CSV
     * parsing, where a numeric field may already be available as bytes and should
     * not need to be decoded into a {@link String} before parsing. It is
     * deliberately flexible about common financial text formats while still
     * rejecting malformed grouping or misplaced formatting characters.
     * <p>
     * Accepted styles include:
     * <ul>
     * <li>plain decimal: {@code 1234.56}</li>
     * <li>leading minus: {@code -1234.56}</li>
     * <li>leading decimal point: {@code .56}</li>
     * <li>trailing decimal point: {@code 1234.}</li>
     * <li>currency prefix: {@code R1234.56}</li>
     * <li>currency suffix: {@code 1234.56R}</li>
     * <li>bracket negative: {@code (1234.56)}</li>
     * <li>currency plus bracket negative: {@code R(1234.56)}, {@code (R1234.56)},
     * {@code (1234.56)R}</li>
     * <li>percent: {@code 1234.56%}</li>
     * <li>period decimal with comma grouping: {@code 1,234.56},
     * {@code 1,234,567.89}</li>
     * <li>comma decimal with period grouping: {@code 1.234,56},
     * {@code 1.234.567,89}</li>
     * <li>comma decimal with regular-space grouping: {@code 1 234,56},
     * {@code 1 234 567,89}</li>
     * <li>period decimal with regular-space grouping: {@code 1 234.56},
     * {@code 1 234 567.89}</li>
     * <li>integer with comma grouping: {@code 1,234}, {@code 1,234,567}</li>
     * <li>integer with period grouping: {@code 1.234}, {@code 1.234.567}</li>
     * <li>integer with regular-space grouping: {@code 1 234},
     * {@code 1 234 567}</li>
     * <li>Latin-1 non-breaking-space grouping: {@code 1&nbsp;234.56}</li>
     * <li>UTF-8 non-breaking-space grouping: {@code 1&nbsp;234.56}</li>
     * <li>UTF-8 narrow non-breaking-space grouping: {@code 1&#8239;234.56}</li>
     * </ul>
     * Spaces and non-breaking spaces are grouping separators only; only comma and
     * period may act as decimal separators. Ambiguous single comma values such as
     * {@code 123,456} are treated as grouped thousands, while non-ambiguous values
     * such as {@code 123,45} are treated as comma-decimal values.
     *
     * @param bytes
     *            byte sequence containing the decimal text
     * @param start
     *            inclusive start index
     * @param end
     *            exclusive end index
     * @return parsed decimal or {@code null}
     */
    public static BigDecimal parse(ByteSequence bytes, int start, int end)
    {
        if (bytes == null || start < 0 || end > bytes.length() || start >= end)
        {
            return null;
        }
        return bytes instanceof ByteString byteString
                ? byteString.parseDecimal(start, end)
                : ByteString.of(bytes).parseDecimal(start, end);
    }

    /**
     * Parses a decimal value from a slice of the supplied text.
     *
     * @param s
     *            text to parse
     * @param start
     *            inclusive start index
     * @param end
     *            exclusive end index
     * @return parsed decimal or {@code null}
     */
    public static BigDecimal parse2(CharSequence s, int start, int end)
    {
        return parse2(s, start, end, DecimalPolicy.AUTO);
    }

    public static ParsedDecimal parseLazy(CharSequence s, int start, int end)
    {
        if (s == null || start < 0 || end > s.length() || start >= end)
        {
            return null;
        }
        boolean negative = false;
        if (s.charAt(start) == '-')
        {
            negative = true;
            ++start;
            if (start == end)
            {
                return null;
            }
        }

        Decimal128Accumulator accumulator = new Decimal128Accumulator();
        int scale = 0;
        boolean hasDigit = false;
        boolean seenDot = false;
        for (int i = start; i < end; ++i)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '.', ',' ->
                {
                    if (seenDot)
                    {
                        return null;
                    }
                    seenDot = c == '.';
                }
                case '0' ->
                {
                    if (seenDot)
                    {
                        int fractionalStart = i;
                        int fractionalEnd = end;
                        while (fractionalEnd > fractionalStart && s.charAt(fractionalEnd - 1) == '0')
                        {
                            --fractionalEnd;
                        }
                        for (int j = fractionalStart; j < fractionalEnd; ++j)
                        {
                            char fractional = s.charAt(j);
                            switch (fractional)
                            {
                                case '.', ',' ->
                                {
                                    return null;
                                }
                                default ->
                                {
                                    if (!isDigit(fractional) || !accumulator.multiply10Add(fractional - '0'))
                                    {
                                        return null;
                                    }
                                    hasDigit = true;
                                    ++scale;
                                }
                            }
                        }
                        return hasDigit ? new ParsedDecimal(negative, accumulator.high, accumulator.low, scale) : null;
                    }
                    if (!accumulator.multiply10Add(0))
                    {
                        return null;
                    }
                    hasDigit = true;
                }
                default ->
                {
                    if (!isDigit(c) || !accumulator.multiply10Add(c - '0'))
                    {
                        return null;
                    }
                    hasDigit = true;
                    if (seenDot)
                    {
                        ++scale;
                    }
                }
            }
        }
        return hasDigit ? new ParsedDecimal(negative, accumulator.high, accumulator.low, scale) : null;
    }

    /**
     * Parses a decimal value from a slice of the supplied text.
     *
     * @param s
     *            text to parse
     * @param start
     *            inclusive start index
     * @param end
     *            exclusive end index
     * @param policy
     *            decimal separator policy
     * @return parsed decimal or {@code null}
     */
    public static BigDecimal parse2(CharSequence s, int start, int end, DecimalPolicy policy)
    {
        if (s == null || start < 0 || end > s.length() || start >= end)
        {
            return null;
        }
        try
        {
            int strippedStart = -1;
            int strippedEnd = -1;
            int firstDigitIndex = -1;
            int lastDigitIndex = -1;
            int digitCount = 0;
            int commaCount = 0;
            int dotCount = 0;
            int groupingSpaceCount = 0;
            boolean hasHyphen = false;
            boolean hasPercent = false;
            boolean hasLeftBracket = false;
            boolean hasRightBracket = false;
            int leftBracketIndex = -1;
            int rightBracketIndex = -1;
            int currencyIndex = -1;
            int hyphenIndex = -1;
            int percentIndex = -1;
            char rightmostSeparator = 0;
            int digitsAfterRightmostSeparator = 0;
            boolean afterNumericStructure = false;

            for (int i = start; i < end; ++i)
            {
                char c = s.charAt(i);
                if (isDigit(c))
                {
                    if (afterNumericStructure)
                    {
                        return null;
                    }
                    if (strippedStart < 0)
                    {
                        strippedStart = i;
                    }
                    strippedEnd = i + 1;
                    ++digitCount;
                    if (firstDigitIndex < 0)
                    {
                        firstDigitIndex = i;
                    }
                    lastDigitIndex = i;
                    if (rightmostSeparator != 0)
                    {
                        ++digitsAfterRightmostSeparator;
                    }
                    continue;
                }
                if (c == ',' || c == '.')
                {
                    if (afterNumericStructure)
                    {
                        return null;
                    }
                    if (strippedStart < 0)
                    {
                        strippedStart = i;
                    }
                    strippedEnd = i + 1;
                    rightmostSeparator = c;
                    digitsAfterRightmostSeparator = 0;
                    if (c == ',')
                    {
                        ++commaCount;
                    }
                    else
                    {
                        ++dotCount;
                    }
                    continue;
                }
                if (isStructuralCharacter(c))
                {
                    if (!isStripCharacter(c))
                    {
                        if (strippedStart < 0)
                        {
                            strippedStart = i;
                        }
                        strippedEnd = i + 1;
                    }
                    if (isGroupingSpace(c) && i > start && i + 1 < end && isDigit(s.charAt(i - 1))
                            && isDigit(s.charAt(i + 1)))
                    {
                        if (afterNumericStructure)
                        {
                            return null;
                        }
                        ++groupingSpaceCount;
                    }
                    else if (c == '-')
                    {
                        if (firstDigitIndex >= 0 || afterNumericStructure)
                        {
                            return null;
                        }
                        if (hasHyphen)
                        {
                            return null;
                        }
                        hasHyphen = true;
                        hyphenIndex = i;
                    }
                    else if (c == '%')
                    {
                        if (firstDigitIndex < 0)
                        {
                            return null;
                        }
                        if (hasPercent)
                        {
                            return null;
                        }
                        hasPercent = true;
                        percentIndex = i;
                        afterNumericStructure = true;
                    }
                    else if (c == '(')
                    {
                        if (firstDigitIndex >= 0 || afterNumericStructure)
                        {
                            return null;
                        }
                        if (hasLeftBracket)
                        {
                            return null;
                        }
                        hasLeftBracket = true;
                        leftBracketIndex = i;
                    }
                    else if (c == ')')
                    {
                        if (firstDigitIndex < 0 || !hasLeftBracket)
                        {
                            return null;
                        }
                        if (hasRightBracket)
                        {
                            return null;
                        }
                        hasRightBracket = true;
                        rightBracketIndex = i;
                        afterNumericStructure = true;
                    }
                    else if (isCurrencySymbol(c))
                    {
                        if (currencyIndex >= 0)
                        {
                            return null;
                        }
                        currencyIndex = i;
                        if (firstDigitIndex >= 0)
                        {
                            afterNumericStructure = true;
                        }
                    }
                    else if (isStripCharacter(c) && firstDigitIndex >= 0)
                    {
                        afterNumericStructure = true;
                    }
                    continue;
                }
                return null;
            }

            if (strippedStart >= strippedEnd || digitCount == 0)
            {
                return null;
            }
            char decimalSeparator = rightmostSeparator;
            if (isAmbiguousSeparator(commaCount, dotCount, groupingSpaceCount, digitsAfterRightmostSeparator))
            {
                decimalSeparator = determineSeparatorByPolicy(policy, rightmostSeparator);
            }
            if (!hasPercent && !hasLeftBracket && !hasRightBracket && currencyIndex < 0 && isBigDecimalCompatibleSlice(
                    strippedStart, hasHyphen, hyphenIndex, commaCount, dotCount, groupingSpaceCount, decimalSeparator))
            {
                return parseBigDecimal(s, strippedStart, strippedEnd);
            }
            int decimalSeparatorCount = decimalSeparator == ',' ? commaCount : decimalSeparator == '.' ? dotCount : 0;
            int groupingCommaCount = decimalSeparator == ',' ? 0 : commaCount;
            int groupingDotCount = decimalSeparator == '.' ? 0 : dotCount;

            boolean negativeBracket = hasLeftBracket || hasRightBracket;
            int numericStart = hasHyphen ? hyphenIndex : firstDigitIndex;
            int numericEnd = lastDigitIndex + 1;
            if (isBigDecimalCompatibleSlice(numericStart, hasHyphen, hyphenIndex, groupingCommaCount, groupingDotCount,
                    groupingSpaceCount, decimalSeparator)
                    && validLightweightStructure(strippedStart, strippedEnd, numericStart, numericEnd, hasHyphen,
                            hyphenIndex, hasPercent, percentIndex, negativeBracket, hasLeftBracket, leftBracketIndex,
                            hasRightBracket, rightBracketIndex, currencyIndex))
            {
                BigDecimal bd = parseBigDecimal(s, numericStart, numericEnd);
                if (bd == null)
                {
                    return null;
                }
                if (hasPercent)
                {
                    bd = bd.movePointLeft(2);
                }
                if (negativeBracket)
                {
                    bd = bd.negate();
                }
                return bd.stripTrailingZeros();
            }
            if (negativeBracket && (!hasLeftBracket || !hasRightBracket || leftBracketIndex > rightBracketIndex))
            {
                return null;
            }
            if (hasHyphen && hyphenIndex != strippedStart)
            {
                return null;
            }
            if (hasPercent && percentIndex != strippedEnd - 1)
            {
                return null;
            }
            if (negativeBracket && hasHyphen)
            {
                return null;
            }
            if (numericStart >= numericEnd)
            {
                return null;
            }
            if (!validStructuralCharacters(s, strippedStart, numericStart, numericEnd, strippedEnd, hyphenIndex,
                    percentIndex, leftBracketIndex, rightBracketIndex, currencyIndex))
            {
                return null;
            }
            if (currencyIndex > numericStart && currencyIndex < numericEnd)
            {
                return null;
            }
            if (decimalSeparatorCount > 1)
            {
                return null;
            }
            if (groupingSpaceCount > 0 && commaCount + dotCount > 1)
            {
                return null;
            }
            if (!validGroupingSeparators(s, numericStart, numericEnd, decimalSeparator))
            {
                return null;
            }

            boolean requiresNormalisation = decimalSeparator == ',' || groupingSpaceCount > 0 || groupingCommaCount > 0
                    || groupingDotCount > 0;
            BigDecimal bd = parseNumericSlice(s, numericStart, numericEnd, decimalSeparator, requiresNormalisation);
            if (bd == null)
            {
                return null;
            }
            if (hasPercent)
            {
                bd = bd.movePointLeft(2);
            }
            if (negativeBracket)
            {
                bd = bd.negate();
            }
            return bd.stripTrailingZeros();
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private static BigDecimal parsePlainDecimalFast(CharSequence chars, int start, int end, boolean negative,
            int scaleAdjustment)
    {
        int i = start;
        if (chars.charAt(i) == '-')
        {
            negative = true;
            ++i;
            if (i == end)
            {
                return null;
            }
        }

        long unscaled = 0L;
        int scale = 0;
        boolean seenDot = false;
        for (; i < end; ++i)
        {
            char c = chars.charAt(i);
            switch (c)
            {
                case '.', ',' ->
                {
                    if (seenDot)
                    {
                        return null;
                    }
                    seenDot = c == '.';
                }
                case '0' ->
                {
                    if (seenDot)
                    {
                        int fractionalStart = i;
                        int fractionalEnd = end;
                        while (fractionalEnd > fractionalStart && chars.charAt(fractionalEnd - 1) == '0')
                        {
                            --fractionalEnd;
                        }
                        for (int j = fractionalStart; j < fractionalEnd; ++j)
                        {
                            char fractional = chars.charAt(j);
                            switch (fractional)
                            {
                                case '.', ',' ->
                                {
                                    return null;
                                }
                                default ->
                                {
                                    int digit = digitValue(fractional);
                                    if (digit < 0)
                                    {
                                        return null;
                                    }
                                    unscaled = unscaled * 10L + digit;
                                    ++scale;
                                }
                            }
                        }
                        if (negative)
                        {
                            unscaled = -unscaled;
                        }
                        scale += scaleAdjustment;
                        return BigDecimal.valueOf(unscaled, scale);
                    }
                    unscaled *= 10L;
                }
                default ->
                {
                    int digit = digitValue(c);
                    if (digit < 0)
                    {
                        return null;
                    }
                    unscaled = unscaled * 10L + digit;
                    if (seenDot)
                    {
                        ++scale;
                    }
                }
            }
        }
        if (negative)
        {
            unscaled = -unscaled;
        }
        return BigDecimal.valueOf(unscaled, scale + scaleAdjustment);
    }

    private static BigDecimal parsePlainDecimalFast(char[] chars, int start, int end, boolean negative,
            int scaleAdjustment)
    {
        int i = start;
        if (chars[i] == '-')
        {
            negative = true;
            ++i;
            if (i == end)
            {
                return null;
            }
        }

        long unscaled = 0L;
        int scale = 0;
        boolean seenDot = false;
        for (; i < end; ++i)
        {
            char c = chars[i];
            switch (c)
            {
                case '.', ',' ->
                {
                    if (seenDot)
                    {
                        return null;
                    }
                    seenDot = c == '.';
                }
                case '0' ->
                {
                    if (seenDot)
                    {
                        int fractionalStart = i;
                        int fractionalEnd = end;
                        while (fractionalEnd > fractionalStart && chars[fractionalEnd - 1] == '0')
                        {
                            --fractionalEnd;
                        }
                        for (int j = fractionalStart; j < fractionalEnd; ++j)
                        {
                            char fractional = chars[j];
                            switch (fractional)
                            {
                                case '.', ',' ->
                                {
                                    return null;
                                }
                                default ->
                                {
                                    int digit = digitValue(fractional);
                                    if (digit < 0)
                                    {
                                        return null;
                                    }
                                    unscaled = unscaled * 10L + digit;
                                    ++scale;
                                }
                            }
                        }
                        if (negative)
                        {
                            unscaled = -unscaled;
                        }
                        scale += scaleAdjustment;
                        return BigDecimal.valueOf(unscaled, scale);
                    }
                    unscaled *= 10L;
                }
                default ->
                {
                    int digit = digitValue(c);
                    if (digit < 0)
                    {
                        return null;
                    }
                    unscaled = unscaled * 10L + digit;
                    if (seenDot)
                    {
                        ++scale;
                    }
                }
            }
        }
        if (negative)
        {
            unscaled = -unscaled;
        }
        return BigDecimal.valueOf(unscaled, scale + scaleAdjustment);
    }

    private static BigDecimal parsePlainDecimalFast(ByteSequence bytes, int start, int end, boolean negative,
            int scaleAdjustment)
    {
        int i = start;
        if (bytes.byteAt(i) == '-')
        {
            negative = true;
            ++i;
            if (i == end)
            {
                return null;
            }
        }

        long unscaled = 0L;
        boolean hasDigit = false;
        int commaCount = 0;
        int dotCount = 0;
        int lastCommaIndex = -1;
        int lastDotIndex = -1;
        int lastGroupingSpaceIndex = -1;
        for (; i < end; ++i)
        {
            byte c = bytes.byteAt(i);
            int digit = digitValue(c);
            if (digit >= 0)
            {
                unscaled = unscaled * 10L + digit;
                hasDigit = true;
                continue;
            }

            switch (c)
            {
                case '.' ->
                {
                    ++dotCount;
                    lastDotIndex = i;
                }
                case ',' ->
                {
                    ++commaCount;
                    lastCommaIndex = i;
                }
                default ->
                {
                    int consumed = groupingSeparatorLength(bytes, i, end);
                    if (consumed > 0)
                    {
                        lastGroupingSpaceIndex = i;
                        i += consumed - 1;
                        continue;
                    }
                    return null;
                }
            }
        }
        if (!hasDigit)
        {
            return null;
        }
        int decimalIndex = decimalSeparatorIndex(bytes, end, commaCount, lastCommaIndex, dotCount, lastDotIndex,
                lastGroupingSpaceIndex >= 0);
        if (decimalIndex == INVALID_INDEX || lastGroupingSpaceIndex > decimalIndex && decimalIndex >= 0
                || !validGroupingSeparators(bytes, start, end, decimalIndex))
        {
            return null;
        }
        int scale = decimalIndex >= 0 ? digitCount(bytes, decimalIndex + 1, end) : 0;
        if (decimalIndex >= 0 && !validDecimalDigits(bytes, decimalIndex + 1, end))
        {
            return null;
        }
        if (negative)
        {
            unscaled = -unscaled;
        }
        return BigDecimal.valueOf(unscaled, scale + scaleAdjustment);
    }

    private static int digitValue(char c)
    {
        int digit = c - '0';
        return digit >= 0 && digit <= 9 ? digit : -1;
    }

    private static int digitValue(byte c)
    {
        int digit = c - '0';
        return digit >= 0 && digit <= 9 ? digit : -1;
    }

    private static int decimalSeparatorIndex(ByteSequence bytes, int end, int commaCount, int lastCommaIndex,
            int dotCount, int lastDotIndex, boolean hasGroupingSpace)
    {
        if (commaCount > 0 && dotCount > 0)
        {
            if (lastCommaIndex > lastDotIndex && commaCount == 1)
            {
                return lastCommaIndex;
            }
            if (lastDotIndex > lastCommaIndex && dotCount == 1)
            {
                return lastDotIndex;
            }
            return INVALID_INDEX;
        }
        if (commaCount == 1)
        {
            int digitsAfterComma = digitCount(bytes, lastCommaIndex + 1, end);
            return !hasGroupingSpace && digitsAfterComma == 3 ? -1 : lastCommaIndex;
        }
        if (dotCount == 1)
        {
            return lastDotIndex;
        }
        return commaCount == 0 || dotCount == 0 ? -1 : INVALID_INDEX;
    }

    private static boolean validGroupingSeparators(ByteSequence bytes, int start, int end, int decimalIndex)
    {
        int integerEnd = decimalIndex >= 0 ? decimalIndex : end;
        int digitsInGroup = 0;
        boolean seenGrouping = false;
        for (int i = start; i < integerEnd; ++i)
        {
            byte c = bytes.byteAt(i);
            if (isDigit(c))
            {
                ++digitsInGroup;
                continue;
            }
            int consumed = c == ',' || c == '.' ? 1 : groupingSeparatorLength(bytes, i, integerEnd);
            if (consumed == 0)
            {
                return false;
            }
            if (digitsInGroup == 0 || seenGrouping && digitsInGroup != 3 || !seenGrouping && digitsInGroup > 3)
            {
                return false;
            }
            seenGrouping = true;
            digitsInGroup = 0;
            i += consumed - 1;
        }
        return !seenGrouping || digitsInGroup == 3;
    }

    private static boolean validDecimalDigits(ByteSequence bytes, int start, int end)
    {
        for (int i = start; i < end; ++i)
        {
            if (!isDigit(bytes.byteAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    private static int digitCount(ByteSequence bytes, int start, int end)
    {
        int count = 0;
        for (int i = start; i < end; ++i)
        {
            if (isDigit(bytes.byteAt(i)))
            {
                ++count;
            }
        }
        return count;
    }

    private static int groupingSeparatorLength(ByteSequence bytes, int index, int end)
    {
        if (bytes.byteAt(index) == ' ')
        {
            return 1;
        }
        if (bytes.byteAt(index) == (byte) 0xA0)
        {
            return 1;
        }
        if (index + 2 <= end && bytes.byteAt(index) == (byte) 0xC2 && bytes.byteAt(index + 1) == (byte) 0xA0)
        {
            return 2;
        }
        if (index + 3 <= end && bytes.byteAt(index) == (byte) 0xE2 && bytes.byteAt(index + 1) == (byte) 0x80
                && bytes.byteAt(index + 2) == (byte) 0xAF)
        {
            return 3;
        }
        return 0;
    }

    private static final class Decimal128Accumulator
    {
        private long high;
        private long low;

        private boolean multiply10Add(int digit)
        {
            long p0 = (low & 0xFFFF_FFFFL) * 10L + digit;
            long newLowLow = p0 & 0xFFFF_FFFFL;
            long carry = p0 >>> 32;

            long p1 = (low >>> 32) * 10L + carry;
            long newLowHigh = p1 & 0xFFFF_FFFFL;
            carry = p1 >>> 32;

            long p2 = (high & 0xFFFF_FFFFL) * 10L + carry;
            long newHighLow = p2 & 0xFFFF_FFFFL;
            carry = p2 >>> 32;

            long p3 = (high >>> 32) * 10L + carry;
            if ((p3 >>> 32) != 0L)
            {
                return false;
            }

            high = (p3 << 32) | newHighLow;
            low = (newLowHigh << 32) | newLowLow;
            return true;
        }
    }

    private static boolean isAmbiguousSeparator(int commaCount, int dotCount, int groupingSpaceCount,
            int digitsAfterRightmostSeparator)
    {
        return groupingSpaceCount == 0 && digitsAfterRightmostSeparator == 3
                && (commaCount == 1 && dotCount == 0 || commaCount == 0 && dotCount == 1);
    }

    private static boolean isBigDecimalCompatibleSlice(int numericStart, boolean hasHyphen, int hyphenIndex,
            int groupingCommaCount, int groupingDotCount, int groupingSpaceCount, char decimalSeparator)
    {
        return (!hasHyphen || hyphenIndex == numericStart) && groupingCommaCount == 0 && groupingDotCount == 0
                && groupingSpaceCount == 0 && decimalSeparator != ',';
    }

    /**
     * Parses a {@link Double} from the supplied text.
     *
     * @param s
     *            text to parse
     * @return parsed double or {@code null}
     */
    public static Double parseDouble(CharSequence s)
    {
        if (s == null)
        {
            return null;
        }
        return parseDoublePrepared(prepare(s.toString()));
    }

    /**
     * Extracts a single decimal value from a sentence-like string.
     *
     * @param sentence
     *            source text
     * @return parsed decimal or {@code null} if none or more than one are found
     * @deprecated use {@code Sentence.ParseMode.ONLY_IN} with
     *             {@link BigDecimals#parse(CharSequence)} instead
     */
    @Deprecated(since = "0.3.25", forRemoval = true)
    public static BigDecimal extract(CharSequence sentence)
    {
        if (Strings.isEmpty(sentence))
        {
            return null;
        }
        String[] words = sentence.toString().strip().split("\\s+");
        BigDecimal extracted = null;
        for (String word : words)
        {
            BigDecimal value = parse(word);
            if (value != null)
            {
                if (extracted != null)
                {
                    return null;
                }
                extracted = value;
            }
        }
        return extracted;
    }

    /**
     * Extracts a single double value from a sentence-like string.
     *
     * @param sentence
     *            source text
     * @return parsed double or {@code null} if none or more than one are found
     * @deprecated use {@code Sentence.ParseMode.ONLY_IN} with
     *             {@link BigDecimals#parseDouble(CharSequence)} instead
     */
    @Deprecated(since = "0.3.25", forRemoval = true)
    public static Double extractDouble(CharSequence sentence)
    {
        if (Strings.isEmpty(sentence))
        {
            return null;
        }
        String[] words = sentence.toString().strip().split("\\s+");
        Double extracted = null;
        for (String word : words)
        {
            Double value = parseDouble(word);
            if (value != null)
            {
                if (extracted != null)
                {
                    return null;
                }
                extracted = value;
            }
        }
        return extracted;
    }

    /**
     * Normalises a decimal candidate before final parsing.
     *
     * @param s
     *            text to prepare
     * @return prepared decimal metadata or {@code null}
     */
    public static PreparedDecimal prepare(CharSequence s)
    {
        if (s == null)
        {
            return null;
        }
        try
        {
            String text = s.toString();
            Classifier cc = new Classifier(text);
            if (cc.hasSpaces())
            {
                text = text.strip();
                cc = new Classifier(text);
            }

            if (cc.cannotBeDecimal())
            {
                return null;
            }

            String normalized = cc.cleanUp();
            if (Strings.isEmpty(normalized))
            {
                return null;
            }

            return new PreparedDecimal(normalized, cc.isPercent(), cc.isNegativeBracket());
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private static BigDecimal parsePrepared(PreparedDecimal prepared)
    {
        if (prepared == null)
        {
            return null;
        }
        try
        {
            String s = prepared.normalizedNumberText();
            BigDecimal bd = new BigDecimal(s, MathContext.DECIMAL64);
            if (prepared.isPercent())
            {
                bd = bd.movePointLeft(2);
            }
            if (prepared.isNegativeBracket())
            {
                bd = new BigDecimal(s).negate(MathContext.DECIMAL64);
            }
            return bd.stripTrailingZeros();
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private static Double parseDoublePrepared(PreparedDecimal prepared)
    {
        if (prepared == null)
        {
            return null;
        }
        try
        {
            double value = Double.parseDouble(prepared.normalizedNumberText());
            if (prepared.isPercent())
            {
                value = value / 100.0d;
            }
            if (prepared.isNegativeBracket())
            {
                value = -value;
            }
            return value;
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private static int stripLeft(CharSequence s, int start, int end)
    {
        while (start < end && isStripCharacter(s.charAt(start)))
        {
            ++start;
        }
        return start;
    }

    private static int stripRight(CharSequence s, int start, int end)
    {
        while (start < end && isStripCharacter(s.charAt(end - 1)))
        {
            --end;
        }
        return end;
    }

    private static boolean isStripCharacter(char c)
    {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    private static boolean isStructuralCharacter(char c)
    {
        return switch (c)
        {
            case '-', '%', '(', ')' -> true;
            default -> isStripCharacter(c) || isCurrencySymbol(c);
        };
    }

    private static BigDecimal parseBigDecimal(CharSequence s, int start, int end)
    {
        int length = end - start;
        char[] chars = new char[length];
        for (int i = 0; i < length; ++i)
        {
            chars[i] = s.charAt(start + i);
        }
        return new BigDecimal(chars, 0, length).stripTrailingZeros();
    }

    private static BigDecimal parseNumericSlice(CharSequence s, int start, int end, char decimalSeparator,
            boolean requiresNormalisation)
    {
        if (!requiresNormalisation)
        {
            return parseBigDecimal(s, start, end);
        }
        char[] chars = new char[end - start];
        int count = 0;
        for (int i = start; i < end; ++i)
        {
            char c = s.charAt(i);
            if (isDigit(c) || c == '-' && i == start)
            {
                chars[count++] = c;
            }
            else if (c == decimalSeparator)
            {
                chars[count++] = '.';
            }
        }
        return new BigDecimal(chars, 0, count).stripTrailingZeros();
    }

    private static boolean validLightweightStructure(int strippedStart, int strippedEnd, int numericStart,
            int numericEnd, boolean hasHyphen, int hyphenIndex, boolean hasPercent, int percentIndex,
            boolean negativeBracket, boolean hasLeftBracket, int leftBracketIndex, boolean hasRightBracket,
            int rightBracketIndex, int currencyIndex)
    {
        if (numericStart >= numericEnd)
        {
            return false;
        }
        if (hasHyphen && hyphenIndex != numericStart)
        {
            return false;
        }
        if (hasPercent && percentIndex != strippedEnd - 1)
        {
            return false;
        }
        if (negativeBracket)
        {
            if (!hasLeftBracket || !hasRightBracket || leftBracketIndex > rightBracketIndex)
            {
                return false;
            }
            if (hasHyphen)
            {
                return false;
            }
            if (leftBracketIndex < strippedStart || leftBracketIndex > numericStart)
            {
                return false;
            }
            if (rightBracketIndex < numericEnd || rightBracketIndex >= strippedEnd)
            {
                return false;
            }
        }
        if (currencyIndex >= 0 && currencyIndex > numericStart && currencyIndex < numericEnd)
        {
            return false;
        }
        return true;
    }

    private static boolean validStructuralCharacters(CharSequence s, int strippedStart, int numericStart,
            int numericEnd, int strippedEnd, int hyphenIndex, int percentIndex, int leftBracketIndex,
            int rightBracketIndex, int currencyIndex)
    {
        for (int i = strippedStart; i < numericStart; ++i)
        {
            char c = s.charAt(i);
            if (isStripCharacter(c) || i == currencyIndex || i == leftBracketIndex)
            {
                continue;
            }
            return false;
        }
        for (int i = numericStart; i < numericEnd; ++i)
        {
            char c = s.charAt(i);
            if (isDigit(c) || c == ',' || c == '.' || isGroupingSpace(c) || c == '-' && i == hyphenIndex)
            {
                continue;
            }
            return false;
        }
        for (int i = numericEnd; i < strippedEnd; ++i)
        {
            char c = s.charAt(i);
            if (isStripCharacter(c) || i == currencyIndex || i == rightBracketIndex || i == percentIndex)
            {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean validGroupingSeparators(CharSequence s, int start, int end, char decimalSeparator)
    {
        for (int i = start; i < end; ++i)
        {
            char c = s.charAt(i);
            if (c == decimalSeparator)
            {
                continue;
            }
            if (c != ',' && c != '.' && !isGroupingSpace(c))
            {
                continue;
            }
            if (i == start || i + 1 == end)
            {
                return false;
            }
            if (!isDigit(s.charAt(i - 1)) || !isDigit(s.charAt(i + 1)))
            {
                return false;
            }
        }
        return true;
    }

    private static char determineSeparatorByPolicy(DecimalPolicy policy, char separator)
    {
        policy = policy == null ? DecimalPolicy.AUTO : policy;
        return switch (policy)
        {
            case PERIOD_DECIMAL -> separator == '.' ? '.' : 0;
            case COMMA_DECIMAL -> separator == ',' ? ',' : 0;
            case AUTO -> separator == ',' ? 0 : '.';
        };
    }

    private static boolean isGroupingSpace(char c)
    {
        return c == ' ' || c == '\u00A0' || c == '\u202F';
    }

    private static boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    private static boolean isDigit(byte c)
    {
        return c >= '0' && c <= '9';
    }

    private static boolean isSupportedDecimalChar(char c)
    {
        return (c >= '0' && c <= '9') || c == '.' || c == '-';
    }

    private static boolean isScientificDecimal(String s)
    {
        if (Strings.isEmpty(s))
        {
            return false;
        }

        int len = s.length();
        int i = 0;
        if (s.charAt(i) == '-')
        {
            ++i;
        }
        if (i >= len)
        {
            return false;
        }

        boolean hasDigitBeforeExponent = false;
        boolean hasDot = false;
        boolean hasExponent = false;
        boolean hasExponentDigit = false;

        for (; i < len; ++i)
        {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9')
            {
                if (hasExponent)
                {
                    hasExponentDigit = true;
                }
                else
                {
                    hasDigitBeforeExponent = true;
                }
                continue;
            }
            if (c == '.')
            {
                if (hasDot || hasExponent)
                {
                    return false;
                }
                hasDot = true;
                continue;
            }
            if (c == 'e' || c == 'E')
            {
                if (hasExponent || !hasDigitBeforeExponent || i == len - 1)
                {
                    return false;
                }
                hasExponent = true;
                if (s.charAt(i + 1) == '-')
                {
                    ++i;
                    if (i == len - 1)
                    {
                        return false;
                    }
                }
                continue;
            }
            return false;
        }

        if (!hasDigitBeforeExponent)
        {
            return false;
        }
        return !hasExponent || hasExponentDigit;
    }

    private static String trimLeftOne(String s)
    {
        return s.length() <= 1 ? "" : s.substring(1);
    }

    private static String trimRightOne(String s)
    {
        return s.length() <= 1 ? "" : s.substring(0, s.length() - 1);
    }

    private static String trimOne(String s)
    {
        return s.length() <= 2 ? "" : s.substring(1, s.length() - 1);
    }

    private static String trimCurrencyOne(String s)
    {
        if (isCurrencySymbol(s.charAt(0)))
        {
            return trimLeftOne(s);
        }
        if (isCurrencySymbol(s.charAt(s.length() - 1)))
        {
            return trimRightOne(s);
        }
        return s;
    }

    private static boolean isCurrencySymbol(char c)
    {
        return switch (c)
        {
            case '$', '€', '£', '¥', 'R' -> true;
            default -> false;
        };
    }

    private static boolean isCurrencySymbol(byte c)
    {
        return switch (c)
        {
            case '$', 'R' -> true;
            default -> false;
        };
    }

    private static final class Classifier
    {
        private final String text;
        private final int invalidCharacterCount;
        private final int digitCount;
        private final int dotCount;
        private final boolean hasCommas;
        private final boolean hasSpaces;
        private final int hyphenCount;
        private final int scientificECount;
        private final boolean hasCurrencySymbol;

        private Classifier(CharSequence s)
        {
            this.text = s.toString();
            int invalid = 0;
            int digit = 0;
            int dot = 0;
            boolean commas = false;
            int hyphen = 0;
            int scientificE = 0;
            boolean currency = false;
            boolean spaces = false;

            for (int i = 0; i < s.length(); ++i)
            {
                char c = s.charAt(i);
                switch (c)
                {
                    case ' ' -> spaces = true;
                    case ',' -> commas = true;
                    case '.' ->
                    {
                        ++dot;
                    }
                    case '-' ->
                    {
                        ++hyphen;
                    }
                    case 'e', 'E' ->
                    {
                        ++scientificE;
                    }
                    case '%', '(', ')' ->
                    {
                        // deliberate, these are valid
                    }
                    case '$', '€', '£', '¥', 'R' -> currency = true;
                    default ->
                    {
                        if (c >= '0' && c <= '9')
                        {
                            ++digit;
                        }
                        else
                        {
                            ++invalid;
                        }
                    }
                }
            }

            this.invalidCharacterCount = invalid;
            this.digitCount = digit;
            this.dotCount = dot;
            this.hasCommas = commas;
            this.hasSpaces = spaces;
            this.hyphenCount = hyphen;
            this.scientificECount = scientificE;
            this.hasCurrencySymbol = currency;
        }

        private boolean hasInvalidCharacters()
        {
            return this.invalidCharacterCount > 0;
        }

        private boolean hasDigits()
        {
            return this.digitCount > 0;
        }

        private boolean hasSpaces()
        {
            return this.hasSpaces;
        }

        private int getDotCount()
        {
            return this.dotCount;
        }

        private int getHyphenCount()
        {
            return this.hyphenCount;
        }

        private int getScientificECount()
        {
            return this.scientificECount;
        }

        private boolean hasCommas()
        {
            return this.hasCommas;
        }

        private boolean hasCurrencySymbolAtEdge()
        {
            return this.hasCurrencySymbol && (isCurrencySymbol(this.text.charAt(0))
                    || isCurrencySymbol(this.text.charAt(this.text.length() - 1)));
        }

        private boolean isNegativeBracket()
        {
            return this.text.length() >= 2 && this.text.charAt(0) == '('
                    && this.text.charAt(this.text.length() - 1) == ')';
        }

        private boolean isPercent()
        {
            return !this.text.isEmpty() && this.text.charAt(this.text.length() - 1) == '%';
        }

        private boolean cannotBeDecimal()
        {
            int allowedHyphens = getScientificECount() == 1 ? 2 : 1;
            return Strings.isEmpty(text) || !hasDigits() || hasInvalidCharacters() || hasSpaces()
                    || getScientificECount() > 1 || getHyphenCount() > allowedHyphens || getDotCount() > 1;
        }

        private String cleanUp()
        {
            String s = this.text;
            if (isNegativeBracket())
            {
                s = trimOne(s);
            }
            if (hasCurrencySymbolAtEdge())
            {
                s = trimCurrencyOne(s);
            }
            if (isPercent())
            {
                s = trimRightOne(s);
            }
            if (hasCommas())
            {
                s = removeCommas(s);
            }
            return isScientificDecimal(s) ? s : null;
        }
    }

    public static boolean isWholeNumber(BigDecimal value)
    {
        return value.stripTrailingZeros().scale() <= 0;
    }
}
