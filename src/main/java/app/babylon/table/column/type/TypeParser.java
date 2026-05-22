/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column.type;

import app.babylon.text.SliceParser;

/**
 * Parses textual values into a target typed value.
 * <p>
 * The primary operation is slice parsing. Whole-sequence parsing delegates to
 * the slice method, so type parsers can be used anywhere a {@link SliceParser}
 * is expected without creating intermediate strings.
 *
 * @param <T>
 *            the object value type produced by
 *            {@link #parse(CharSequence, int, int)}
 */
@FunctionalInterface
public interface TypeParser<T> extends SliceParser<T>
{
    /**
     * Parses a slice of a character sequence into an object value.
     *
     * @param s
     *            the source text
     * @param start
     *            the start index
     * @param end
     *            one past the last character in the slice
     * @return the parsed value, or {@code null} when parsing fails
     */
    @Override
    T parse(CharSequence s, int start, int end);

    /**
     * Parses a whole character sequence into an object value.
     *
     * @param s
     *            the source text
     * @return the parsed value, or {@code null} when parsing fails
     */
    @Override
    default T parse(CharSequence s)
    {
        return s == null ? null : parse(s, 0, s.length());
    }

    /**
     * Parses a whole character sequence into a byte value.
     *
     * @param s
     *            the source text
     * @return the parsed byte
     */
    default byte parseByte(CharSequence s)
    {
        int parsed = parseInt(s, 0, s.length());
        if (parsed < Byte.MIN_VALUE || parsed > Byte.MAX_VALUE)
        {
            throw new NumberFormatException("Value out of range for byte: " + s);
        }
        return (byte) parsed;
    }

    /**
     * Parses a slice of a character sequence into a byte value.
     *
     * @param s
     *            the source characters
     * @param start
     *            the start index
     * @param end
     *            one past the last character in the slice
     * @return the parsed byte
     */
    default byte parseByte(CharSequence s, int start, int end)
    {
        int parsed = parseInt(s, start, end);
        if (parsed < Byte.MIN_VALUE || parsed > Byte.MAX_VALUE)
        {
            throw new NumberFormatException("Value out of range for byte: " + s.subSequence(start, end));
        }
        return (byte) parsed;
    }

    /**
     * Parses a whole character sequence into a boolean value.
     *
     * @param s
     *            the source text
     * @return the parsed boolean
     */
    default boolean parseBoolean(CharSequence s)
    {
        return parseBoolean(s, 0, s.length());
    }

    /**
     * Parses a slice of a character sequence into a boolean value.
     *
     * @param s
     *            the source text
     * @param start
     *            the start index
     * @param end
     *            one past the last character in the slice
     * @return the parsed boolean
     */
    default boolean parseBoolean(CharSequence s, int start, int end)
    {
        T parsed = parse(s, start, end);
        if (parsed instanceof Boolean value)
        {
            return value.booleanValue();
        }
        throw new IllegalArgumentException("Could not parse boolean: " + s.subSequence(start, end));
    }

    /**
     * Parses a whole character sequence into an int value.
     *
     * @param s
     *            the source text
     * @return the parsed int
     */
    default int parseInt(CharSequence s)
    {
        return parseInt(s, 0, s.length());
    }

    /**
     * Parses a slice of a character sequence into an int value.
     *
     * @param s
     *            the source text
     * @param start
     *            the start index
     * @param end
     *            one past the last character in the slice
     * @return the parsed int
     */
    default int parseInt(CharSequence s, int start, int end)
    {
        return Integer.parseInt(s, start, end, 10);
    }

    /**
     * Parses a whole character sequence into a long value.
     *
     * @param s
     *            the source text
     * @return the parsed long
     */
    default long parseLong(CharSequence s)
    {
        return parseLong(s, 0, s.length());
    }

    /**
     * Parses a slice of a character sequence into a long value.
     *
     * @param s
     *            the source text
     * @param start
     *            the start index
     * @param end
     *            one past the last character in the slice
     * @return the parsed long
     */
    default long parseLong(CharSequence s, int start, int end)
    {
        return Long.parseLong(s, start, end, 10);
    }

    /**
     * Parses a whole character sequence into a double value.
     *
     * @param s
     *            the source text
     * @return the parsed double
     */
    default double parseDouble(CharSequence s)
    {
        return parseDouble(s, 0, s.length());
    }

    /**
     * Parses a slice of a character sequence into a double value.
     *
     * @param s
     *            the source text
     * @param start
     *            the start index
     * @param end
     *            one past the last character in the slice
     * @return the parsed double
     */
    default double parseDouble(CharSequence s, int start, int end)
    {
        return Double.parseDouble(s.subSequence(start, end).toString());
    }
}
