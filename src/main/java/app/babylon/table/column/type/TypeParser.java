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

import java.util.Objects;

import app.babylon.text.ByteSequence;
import app.babylon.text.ByteString;
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

    default T parse(ByteString s, int start, int end)
    {
        return s == null ? null : parse(s.decode(start, end));
    }

    default T parse(ByteSequence s, int start, int end)
    {
        return s instanceof ByteString byteString
                ? parse(byteString, start, end)
                : s == null ? null : parse(s.decode(start, end));
    }

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

    default byte parseByte(ByteSequence s, int start, int end)
    {
        int parsed = parseInt(s, start, end);
        if (parsed < Byte.MIN_VALUE || parsed > Byte.MAX_VALUE)
        {
            throw new NumberFormatException("Value out of range for byte.");
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

    default int parseInt(ByteSequence s, int start, int end)
    {
        Objects.requireNonNull(s);
        Objects.checkFromToIndex(start, end, s.length());

        boolean negative = false;
        int i = start;
        int limit = -Integer.MAX_VALUE;

        if (i < end)
        {
            byte firstByte = s.byteAt(i);
            if (firstByte < '0')
            {
                if (firstByte == '-')
                {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                }
                else if (firstByte != '+')
                {
                    throw new NumberFormatException();
                }
                ++i;
                if (i == end)
                {
                    throw new NumberFormatException();
                }
            }

            int multmin = limit / 10;
            int result = 0;
            while (i < end)
            {
                int digit = s.byteAt(i) - '0';
                if (digit < 0 || digit > 9 || result < multmin)
                {
                    throw new NumberFormatException();
                }
                result *= 10;
                if (result < limit + digit)
                {
                    throw new NumberFormatException();
                }
                ++i;
                result -= digit;
            }
            return negative ? result : -result;
        }
        throw new NumberFormatException();
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

    default long parseLong(ByteSequence s, int start, int end)
    {
        Objects.requireNonNull(s);
        Objects.checkFromToIndex(start, end, s.length());

        boolean negative = false;
        int i = start;
        long limit = -Long.MAX_VALUE;

        if (i < end)
        {
            byte firstByte = s.byteAt(i);
            if (firstByte < '0')
            {
                if (firstByte == '-')
                {
                    negative = true;
                    limit = Long.MIN_VALUE;
                }
                else if (firstByte != '+')
                {
                    throw new NumberFormatException();
                }
                ++i;
            }
            if (i >= end)
            {
                throw new NumberFormatException();
            }

            long multmin = limit / 10;
            long result = 0;
            while (i < end)
            {
                int digit = s.byteAt(i) - '0';
                if (digit < 0 || digit > 9 || result < multmin)
                {
                    throw new NumberFormatException();
                }
                result *= 10;
                if (result < limit + digit)
                {
                    throw new NumberFormatException();
                }
                ++i;
                result -= digit;
            }
            return negative ? result : -result;
        }
        throw new NumberFormatException();
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
