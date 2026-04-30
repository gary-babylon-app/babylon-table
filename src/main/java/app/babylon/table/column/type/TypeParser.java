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
     * @param offset
     *            the start index
     * @param length
     *            the slice length
     * @return the parsed value, or {@code null} when parsing fails
     */
    @Override
    T parse(CharSequence s, int offset, int length);

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
     * @param offset
     *            the start index
     * @param length
     *            the slice length
     * @return the parsed byte
     */
    default byte parseByte(CharSequence s, int offset, int length)
    {
        int parsed = parseInt(s, offset, length);
        if (parsed < Byte.MIN_VALUE || parsed > Byte.MAX_VALUE)
        {
            throw new NumberFormatException("Value out of range for byte: " + s.subSequence(offset, offset + length));
        }
        return (byte) parsed;
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
     * @param offset
     *            the start index
     * @param length
     *            the slice length
     * @return the parsed int
     */
    default int parseInt(CharSequence s, int offset, int length)
    {
        return Integer.parseInt(s, offset, offset + length, 10);
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
     * @param offset
     *            the start index
     * @param length
     *            the slice length
     * @return the parsed long
     */
    default long parseLong(CharSequence s, int offset, int length)
    {
        return Long.parseLong(s, offset, offset + length, 10);
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
     * @param offset
     *            the start index
     * @param length
     *            the slice length
     * @return the parsed double
     */
    default double parseDouble(CharSequence s, int offset, int length)
    {
        return Double.parseDouble(s.subSequence(offset, offset + length).toString());
    }
}
