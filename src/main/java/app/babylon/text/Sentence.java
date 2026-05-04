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

import java.util.BitSet;
import java.util.Locale;
import java.util.function.Function;

public final class Sentence
{
    public enum ParseMode
    {
        EXACT, FIRST_IN, LAST_IN, ONLY_IN;

        public static ParseMode parse(CharSequence s)
        {
            if (s == null)
            {
                return EXACT;
            }
            String normalised = Strings.clean(s, ' ', '_', '-').toUpperCase(Locale.ROOT);
            return switch (normalised)
            {
                case "EXACT" -> EXACT;
                case "FIRSTIN" -> FIRST_IN;
                case "LASTIN" -> LAST_IN;
                case "ONLYIN" -> ONLY_IN;
                default -> throw new IllegalArgumentException("Unknown sentence parse mode: " + s);
            };
        }

        public <T> T apply(Function<CharSequence, T> parser, CharSequence value)
        {
            return apply(SliceParser.from(parser), value);
        }

        public <T> T apply(SliceParser<T> parser, CharSequence value)
        {
            return value == null ? null : apply(parser, value, 0, value.length());
        }

        public <T> T apply(SliceParser<T> parser, CharSequence value, int start, int length)
        {
            if (value == null)
            {
                return null;
            }
            return switch (this)
            {
                case EXACT -> parser.parse(value, start, length);
                case FIRST_IN -> Sentence.firstIn(parser, value, start, length);
                case LAST_IN -> Sentence.lastIn(parser, value, start, length);
                case ONLY_IN -> Sentence.onlyIn(parser, value, start, length);
            };
        }
    }

    /**
     * Finds the <em>first</em> parsed value <em>in</em> a space-separated sentence.
     * <p>
     * Parsers with a {@code parse(CharSequence, int, int)} shape bind directly to
     * {@link SliceParser}, so callers can scan sentence words without materialising
     * intermediate strings:
     * 
     * <pre>
     * Currency currency = Sentence.firstIn(Currencys::parse, "pay USD 120 tomorrow");
     * Currency typed = Sentence.firstIn(TypeParsers.CURRENCY, "pay USD 120 tomorrow");
     * </pre>
     * <p>
     * {@code TypeParser} implementations can be used here too because they extend
     * {@link SliceParser}.
     */
    public static <T> T firstIn(SliceParser<T> parser, CharSequence sentence)
    {
        return sentence == null ? null : firstIn(parser, sentence, 0, sentence.length());
    }

    public static <T> T firstIn(SliceParser<T> parser, CharSequence sentence, int offset, int length)
    {
        if (sentence == null)
        {
            return null;
        }
        BitSet separators = Strings.trace(sentence, offset, length, ' ');
        int start = offset;
        int end = offset + length;
        for (int separator = separators.nextSetBit(offset); separator >= 0
                && separator < end; separator = separators.nextSetBit(separator + 1))
        {
            T value = parse(parser, sentence, start, separator);
            if (value != null)
            {
                return value;
            }
            start = separator + 1;
        }
        return parse(parser, sentence, start, end);
    }

    /**
     * Finds the <em>last</em> parsed value <em>in</em> a space-separated sentence.
     * <p>
     * For example, this finds the last currency in a sentence:
     * 
     * <pre>
     * Currency currency = Sentence.lastIn(Currencys::parse, "pay USD or EUR tomorrow");
     * Currency typed = Sentence.lastIn(TypeParsers.CURRENCY, "pay USD or EUR tomorrow");
     * </pre>
     * <p>
     * The parser receives the original sentence plus each word's start and length,
     * avoiding intermediate strings.
     */
    public static <T> T lastIn(SliceParser<T> parser, CharSequence sentence)
    {
        return lastIn(parser, sentence, ' ');
    }

    /**
     * Finds the <em>last</em> parsed value <em>in</em> a sentence separated by
     * {@code separator}.
     * <p>
     * The parser receives the original sentence plus each field's start and length,
     * avoiding intermediate strings.
     */
    public static <T> T lastIn(SliceParser<T> parser, CharSequence sentence, char separator)
    {
        return sentence == null ? null : lastIn(parser, sentence, 0, sentence.length(), separator);
    }

    public static <T> T lastIn(SliceParser<T> parser, CharSequence sentence, int offset, int length)
    {
        return lastIn(parser, sentence, offset, length, ' ');
    }

    public static <T> T lastIn(SliceParser<T> parser, CharSequence sentence, int offset, int length, char separator)
    {
        if (sentence == null)
        {
            return null;
        }
        BitSet separators = Strings.trace(sentence, offset, length, separator);
        int end = offset + length;
        for (int i = separators.previousSetBit(end - 1); i >= offset; i = separators.previousSetBit(i - 1))
        {
            T value = parse(parser, sentence, i + 1, end);
            if (value != null)
            {
                return value;
            }
            end = i;
        }
        return parse(parser, sentence, offset, end);
    }

    /**
     * Finds the <em>only</em> parsed value <em>in</em> a space-separated sentence,
     * or {@code null} when no value or multiple values are found.
     * <p>
     * The name is short for "find the only currency in this sentence":
     * 
     * <pre>
     * Currency currency = Sentence.onlyIn(Currencys::parse, "pay USD tomorrow");
     * Currency typed = Sentence.onlyIn(TypeParsers.CURRENCY, "pay USD tomorrow");
     * </pre>
     * <p>
     * The parser receives the original sentence plus each word's start and length,
     * avoiding intermediate strings.
     */
    public static <T> T onlyIn(SliceParser<T> parser, CharSequence sentence)
    {
        return sentence == null ? null : onlyIn(parser, sentence, 0, sentence.length());
    }

    public static <T> T onlyIn(SliceParser<T> parser, CharSequence sentence, int offset, int length)
    {
        if (sentence == null)
        {
            return null;
        }
        BitSet separators = Strings.trace(sentence, offset, length, ' ');
        T only = null;
        int start = offset;
        int end = offset + length;
        for (int separator = separators.nextSetBit(offset); separator >= 0
                && separator < end; separator = separators.nextSetBit(separator + 1))
        {
            T value = parse(parser, sentence, start, separator);
            if (value != null)
            {
                if (only != null)
                {
                    return null;
                }
                only = value;
            }
            start = separator + 1;
        }
        T value = parse(parser, sentence, start, end);
        if (value != null)
        {
            if (only != null)
            {
                return null;
            }
            only = value;
        }
        return only;
    }

    private static <T> T parse(SliceParser<T> parser, CharSequence sentence, int start, int end)
    {
        if (start >= end)
        {
            return null;
        }
        return parser.parse(sentence, start, end - start);
    }
}
