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
import java.math.MathContext;

/**
 * Parsing and extraction helpers for decimal text values.
 */
public final class BigDecimals
{
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
     */
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
     */
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
