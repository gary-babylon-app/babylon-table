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

public final class Sentence
{

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
        if (sentence == null)
        {
            return null;
        }
        BitSet separators = Strings.trace(sentence, ' ');
        int start = 0;
        for (int separator = separators.nextSetBit(0); separator >= 0; separator = separators.nextSetBit(separator + 1))
        {
            T value = parse(parser, sentence, start, separator);
            if (value != null)
            {
                return value;
            }
            start = separator + 1;
        }
        return parse(parser, sentence, start, sentence.length());
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
    public static <T> T lastIn(SliceParser<T> parser, String sentence)
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
    public static <T> T lastIn(SliceParser<T> parser, String sentence, char separator)
    {
        if (sentence == null)
        {
            return null;
        }
        BitSet separators = Strings.trace(sentence, separator);
        int end = sentence.length();
        for (int i = separators.previousSetBit(sentence.length() - 1); i >= 0; i = separators.previousSetBit(i - 1))
        {
            T value = parse(parser, sentence, i + 1, end);
            if (value != null)
            {
                return value;
            }
            end = i;
        }
        return parse(parser, sentence, 0, end);
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
        if (sentence == null)
        {
            return null;
        }
        BitSet separators = Strings.trace(sentence, ' ');
        T only = null;
        int start = 0;
        for (int separator = separators.nextSetBit(0); separator >= 0; separator = separators.nextSetBit(separator + 1))
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
        T value = parse(parser, sentence, start, sentence.length());
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
