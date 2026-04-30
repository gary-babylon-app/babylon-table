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

import java.util.function.Function;

/**
 * Parses a slice of character data without requiring callers to materialize a
 * {@link String} or create a {@link CharSequence} view first.
 * <p>
 * Any method with the same shape can bind directly as a method reference. For
 * example, {@code Currencys.parse(CharSequence, int, int)} can be used wherever
 * a {@code SliceParser<Currency>} is expected:
 * 
 * <pre>
 * Currency currency = Sentence.firstIn(Currencys::parse, "pay USD 120 tomorrow");
 * </pre>
 *
 * @param <T>
 *            the parsed value type
 */
@FunctionalInterface
public interface SliceParser<T> extends Function<CharSequence, T>
{
    /**
     * Parses a slice of {@code s}.
     *
     * @param s
     *            the source characters
     * @param start
     *            the first character in the slice
     * @param length
     *            the number of characters in the slice
     * @return the parsed value, or {@code null} when parsing fails
     */
    T parse(CharSequence s, int start, int length);

    /**
     * Parses the whole character sequence.
     */
    default T parse(CharSequence s)
    {
        return s == null ? null : parse(s, 0, s.length());
    }

    @Override
    default T apply(CharSequence s)
    {
        return parse(s);
    }

    /**
     * Adapts an existing whole-sequence parser. This compatibility path may create
     * a {@link CharSequence#subSequence(int, int)} view for sliced inputs; parsers
     * that can consume offsets directly should implement
     * {@link #parse(CharSequence, int, int)} instead.
     */
    static <T> SliceParser<T> from(Function<CharSequence, T> parser)
    {
        if (parser == null)
        {
            return (s, start, length) -> null;
        }
        return (s, start, length) -> parser.apply(s == null ? null : s.subSequence(start, start + length));
    }
}
