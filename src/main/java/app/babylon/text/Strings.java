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

import java.text.Normalizer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Predicate;

public final class Strings
{
    public static final Predicate<String> EMPTY_OR_NULL = s -> s == null || s.isEmpty();
    private static final Splitter DEFAULT_SPLITTER = new Splitter();

    public static CharSequence toCamelUpperPreserve(CharSequence x)
    {
        if (x == null)
        {
            return null;
        }

        boolean hasSeparator = false;
        for (int i = 0; i < x.length(); ++i)
        {
            if (!Character.isLetterOrDigit(x.charAt(i)))
            {
                hasSeparator = true;
                break;
            }
        }
        if (!hasSeparator)
        {
            if (x.length() == 0)
            {
                return x;
            }
            return Character.toUpperCase(x.charAt(0)) + x.toString().substring(1);
        }

        StringBuilder result = new StringBuilder(x.length());
        boolean newWord = true;
        for (int i = 0; i < x.length(); i++)
        {
            char currentChar = x.charAt(i);
            if (Character.isLetterOrDigit(currentChar))
            {
                if (newWord)
                {
                    result.append(Character.toUpperCase(currentChar));
                    newWord = false;
                }
                else
                {
                    result.append(Character.toLowerCase(currentChar));
                }
            }
            else
            {
                newWord = true;
            }
        }
        return result.toString();
    }

    public static CharSequence leftPad(CharSequence s, int size, char padChar)
    {
        if (s == null)
        {
            return null;
        }
        int pads = size - s.length();
        if (pads <= 0)
        {
            return s;
        }
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < pads; ++i)
        {
            builder.append(padChar);
        }
        builder.append(s);
        return builder;
    }

    public static CharSequence rightPad(CharSequence s, int size, char padChar)
    {
        if (s == null)
        {
            return null;
        }
        int pads = size - s.length();
        if (pads <= 0)
        {
            return s;
        }
        StringBuilder builder = new StringBuilder(size);
        builder.append(s);
        for (int i = 0; i < pads; ++i)
        {
            builder.append(padChar);
        }
        return builder.toString();
    }

    public static boolean isAlpha(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static boolean isAlphaNumeric(char c)
    {
        return isAlpha(c) || (c >= '0' && c <= '9');
    }

    private static boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    public static boolean equals(CharSequence s, int start, int length, CharSequence expected)
    {
        return equals(s, start, length, expected, false);
    }

    public static boolean equalsIgnoreCase(CharSequence s, int start, int length, CharSequence expected)
    {
        return equals(s, start, length, expected, true);
    }

    private static boolean equals(CharSequence s, int start, int length, CharSequence expected, boolean ignoreCase)
    {
        if (s == null || expected == null || length != expected.length())
        {
            return false;
        }
        for (int i = 0; i < length; ++i)
        {
            char left = s.charAt(start + i);
            char right = expected.charAt(i);
            if (left == right)
            {
                continue;
            }
            if (!ignoreCase || Character.toLowerCase(left) != Character.toLowerCase(right))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(CharSequence s)
    {
        return s == null || s.length() == 0;
    }

    public static boolean isEmpty(CharSequence s, int start, int length)
    {
        return s == null || length <= 0;
    }

    /**
     * Returns true when the supplied sequence is empty after applying
     * {@link #stripx(CharSequence)} rules.
     */
    public static boolean isStripxEmpty(CharSequence s)
    {
        return s == null || isStripxEmpty(s, 0, s.length());
    }

    /**
     * Returns true when the supplied slice is empty after applying the same edge
     * stripping rules as {@link #stripx(CharSequence, int, int)}. Those rules
     * remove normal Unicode whitespace and the additional ingestion artifacts
     * documented on {@code stripx}.
     */
    public static boolean isStripxEmpty(CharSequence s, int start, int length)
    {
        if (s == null || length <= 0)
        {
            return true;
        }
        int actualStart = start;
        int actualEnd = start + length - 1;
        while (actualStart <= actualEnd && isStrippable(s.charAt(actualStart)))
        {
            actualStart++;
        }
        return actualStart > actualEnd;
    }

    public static int indexOf(CharSequence s, char c)
    {
        return s == null ? -1 : indexOf(s, 0, s.length(), c);
    }

    public static int indexOf(CharSequence s, int start, int length, char c)
    {
        if (s == null || length <= 0)
        {
            return -1;
        }

        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            if (s.charAt(i) == c)
            {
                return i;
            }
        }
        return -1;
    }

    public static int indexOfAny(CharSequence s, char c, char... additional)
    {
        return s == null ? -1 : indexOfAny(s, 0, s.length(), c, additional);
    }

    public static int indexOfAny(CharSequence s, int start, int length, char c, char... additional)
    {
        if (additional == null || additional.length == 0)
        {
            return indexOf(s, start, length, c);
        }
        if (additional.length == 1)
        {
            return indexOfAny2(s, start, length, c, additional[0]);
        }
        if (additional.length == 2)
        {
            return indexOfAny3(s, start, length, c, additional[0], additional[1]);
        }
        if (additional.length == 3)
        {
            return indexOfAny4(s, start, length, c, additional[0], additional[1], additional[2]);
        }

        if (s == null || length <= 0)
        {
            return -1;
        }

        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            char current = s.charAt(i);
            if (current == c || isAny(current, additional))
            {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(CharSequence s, char c)
    {
        return s == null ? -1 : lastIndexOf(s, 0, s.length(), c);
    }

    public static int lastIndexOf(CharSequence s, int start, int length, char c)
    {
        if (s == null || length <= 0)
        {
            return -1;
        }

        for (int i = start + length - 1; i >= start; --i)
        {
            if (s.charAt(i) == c)
            {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOfAny(CharSequence s, char c, char... additional)
    {
        return s == null ? -1 : lastIndexOfAny(s, 0, s.length(), c, additional);
    }

    public static int lastIndexOfAny(CharSequence s, int start, int length, char c, char... additional)
    {
        if (additional == null || additional.length == 0)
        {
            return lastIndexOf(s, start, length, c);
        }
        if (s == null || length <= 0)
        {
            return -1;
        }

        for (int i = start + length - 1; i >= start; --i)
        {
            char current = s.charAt(i);
            if (current == c || isAny(current, additional))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds every occurrence of {@code splitter} and returns the source indexes as
     * marked bits.
     * <p>
     * This is intentionally a raw delimiter trace: it does not strip text, remove
     * empty fields, or otherwise apply {@link Splitter} policy. Use it when code
     * needs delimiter positions directly, for example to size arrays or plan
     * low-level parsing.
     */
    public static BitSet trace(CharSequence s, char splitter)
    {
        return s == null ? new BitSet() : trace(s, 0, s.length(), splitter);
    }

    /**
     * Finds every occurrence of {@code splitter} in a slice and returns their
     * indexes as marked bits.
     * <p>
     * The returned {@link BitSet} is indexed against the original source sequence,
     * not against the supplied slice. For example, a splitter at {@code start} sets
     * bit {@code start}.
     */
    public static BitSet trace(CharSequence s, int start, int length, char splitter)
    {
        BitSet indexes = new BitSet();
        if (s == null || length <= 0)
        {
            return indexes;
        }

        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            if (s.charAt(i) == splitter)
            {
                indexes.set(i);
            }
        }
        return indexes;
    }

    /**
     * Finds every occurrence of any supplied splitter and returns the source
     * indexes as marked bits.
     * <p>
     * This is the multi-delimiter form of {@link #trace(CharSequence, char)}. The
     * single-character trace overload remains the fast path for the common case and
     * avoids varargs array creation.
     */
    public static BitSet traceAny(CharSequence s, char splitter, char... additionalSplitters)
    {
        return s == null ? new BitSet() : traceAny(s, 0, s.length(), splitter, additionalSplitters);
    }

    /**
     * Finds every occurrence of any supplied splitter in a slice and returns their
     * indexes as marked bits.
     * <p>
     * The returned {@link BitSet} is indexed against the original source sequence,
     * not against the supplied slice.
     */
    public static BitSet traceAny(CharSequence s, int start, int length, char splitter, char... additionalSplitters)
    {
        if (additionalSplitters == null || additionalSplitters.length == 0)
        {
            return trace(s, start, length, splitter);
        }

        BitSet indexes = new BitSet();
        if (s == null || length <= 0)
        {
            return indexes;
        }

        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            char c = s.charAt(i);
            if (c == splitter || isAny(c, additionalSplitters))
            {
                indexes.set(i);
            }
        }
        return indexes;
    }

    /**
     * Splits using {@code splitter}, with the same defaults as {@link #splitter()}:
     * {@code withStripping(true)} and {@code withRemoveEmpty(true)}.
     * <p>
     * This is convenient for one-off calls. For repeated use with the same
     * delimiter, prefer a reusable immutable {@link Splitter}:
     * 
     * <pre>{@code
     * private static final Strings.Splitter PIPE = Strings.splitter().withSplitter('|');
     * }</pre>
     */
    public static String[] split(CharSequence s, char splitter)
    {
        return splitter().withSplitter(splitter).split(s);
    }

    /**
     * Splits a slice using {@code splitter}, with the same defaults as
     * {@link #splitter()}: {@code withStripping(true)} and
     * {@code withRemoveEmpty(true)}.
     * <p>
     * This is convenient for one-off calls. For repeated use with the same
     * delimiter, prefer a reusable immutable {@link Splitter}.
     */
    public static String[] split(CharSequence s, int start, int length, char splitter)
    {
        return splitter().withSplitter(splitter).split(s, start, length);
    }

    /**
     * Splits comma-separated text using the default {@link Splitter} settings.
     */
    public static String[] split(CharSequence s)
    {
        return DEFAULT_SPLITTER.split(s);
    }

    /**
     * Returns a copy of {@code values} with {@code null} entries removed while
     * preserving order.
     * <p>
     * If {@code values} is already compact, the same array instance is returned. If
     * {@code values} is null, an empty array is returned.
     */
    public static String[] compact(String[] values)
    {
        return compact(values, s -> s == null);
    }

    /**
     * Returns a copy of {@code values} with entries matching {@code remove} omitted
     * while preserving order.
     * <p>
     * If no entries match, the same array instance is returned. If {@code values}
     * is null, an empty array is returned. If {@code remove} is null, no entries
     * are removed.
     */
    public static String[] compact(String[] values, Predicate<String> remove)
    {
        if (values == null)
        {
            return new String[0];
        }
        if (remove == null)
        {
            return values;
        }

        BitSet removed = new BitSet();
        for (int i = 0; i < values.length; ++i)
        {
            if (remove.test(values[i]))
            {
                removed.set(i);
            }
        }
        int removedCount = removed.cardinality();
        if (removedCount == 0)
        {
            return values;
        }

        String[] compacted = new String[values.length - removedCount];
        int size = 0;
        for (int i = 0; i < values.length; ++i)
        {
            if (!removed.get(i))
            {
                compacted[size++] = values[i];
            }
        }
        return compacted;
    }

    /**
     * Creates a reusable splitter configuration.
     * <p>
     * Defaults are comma delimiter, {@code withStripping(true)}, and
     * {@code withRemoveEmpty(true)}. Stripping uses the same boundary rules as
     * {@link #stripx(CharSequence)}, so it removes Unicode whitespace and common
     * ingestion artifacts such as BOMs, non-breaking spaces, zero-width characters,
     * and replacement characters.
     * <p>
     * Because splitter configurations are immutable, configured instances can be
     * stored as constants for repeated use:
     * 
     * <pre>{@code
     * private static final Strings.Splitter PIPE = Strings.splitter().withSplitter('|');
     * }</pre>
     */
    public static Splitter splitter()
    {
        return DEFAULT_SPLITTER;
    }

    /**
     * Configurable string splitter.
     * <p>
     * Splitters are immutable. Fluent configuration methods return a new splitter,
     * so configured instances can be safely reused as constants, for example:
     * 
     * <pre>{@code
     * static final Strings.Splitter PIPE_SPLITTER = Strings.splitter().withSplitter('|');
     * static final Strings.Splitter DATE_SPLITTER = Strings.splitter().withSplitters('-', '/');
     * String[] fields = PIPE_SPLITTER.withRemoveEmpty(false).split(source);
     * }</pre>
     */
    public static final class Splitter
    {
        private final char splitter;
        private final char[] additionalSplitters;
        private final boolean strip;
        private final boolean removeEmpty;

        private Splitter()
        {
            this(',', null, true, true);
        }

        private Splitter(char splitter, char[] additionalSplitters, boolean strip, boolean removeEmpty)
        {
            this.splitter = splitter;
            this.additionalSplitters = additionalSplitters;
            this.strip = strip;
            this.removeEmpty = removeEmpty;
        }

        /**
         * Returns a splitter with the delimiter character used to split input. The
         * default delimiter is comma.
         */
        public Splitter withSplitter(char splitter)
        {
            return new Splitter(splitter, null, this.strip, this.removeEmpty);
        }

        /**
         * Returns a splitter that treats any of the supplied delimiter characters as a
         * field boundary.
         * <p>
         * This is intended for reusable splitter constants. The additional delimiter
         * array is defensively copied so the returned splitter remains immutable.
         */
        public Splitter withSplitters(char splitter, char... additionalSplitters)
        {
            char[] copy = additionalSplitters == null || additionalSplitters.length == 0
                    ? null
                    : Arrays.copyOf(additionalSplitters, additionalSplitters.length);
            return new Splitter(splitter, copy, this.strip, this.removeEmpty);
        }

        /**
         * Returns a splitter that controls whether each field is stripped before it is
         * returned or tested for emptiness.
         * <p>
         * When enabled, stripping uses {@link #stripx(CharSequence)} rules at field
         * boundaries. The default is {@code true}.
         */
        public Splitter withStripping(boolean strip)
        {
            return new Splitter(this.splitter, this.additionalSplitters, strip, this.removeEmpty);
        }

        /**
         * Returns a splitter that controls whether empty fields are omitted from the
         * result.
         * <p>
         * When {@code withStripping(true)} is enabled, fields containing only
         * stripx-removable boundary characters are considered empty. The default is
         * {@code true}.
         */
        public Splitter withRemoveEmpty(boolean removeEmpty)
        {
            return new Splitter(this.splitter, this.additionalSplitters, this.strip, removeEmpty);
        }

        /**
         * Splits the whole source using this splitter configuration.
         */
        public String[] split(CharSequence source)
        {
            return source == null ? new String[0] : split(source, 0, source.length());
        }

        /**
         * Splits a slice using this splitter configuration.
         * <p>
         * The implementation traces delimiter positions once, allocates for the maximum
         * possible number of fields, and only compacts the result if empty fields are
         * removed.
         */
        public String[] split(CharSequence source, int start, int length)
        {
            if (source == null || length <= 0)
            {
                return new String[0];
            }

            int end = start + length;
            BitSet splitters = this.additionalSplitters == null
                    ? trace(source, start, length, this.splitter)
                    : traceAny(source, start, length, this.splitter, this.additionalSplitters);
            String[] result = new String[splitters.cardinality() + 1];
            int resultIndex = 0;
            int tokenStart = start;
            for (int splitterIndex = splitters.nextSetBit(start); splitterIndex >= 0
                    && splitterIndex < end; splitterIndex = splitters.nextSetBit(splitterIndex + 1))
            {
                String token = this.stringOrNull(source, tokenStart, splitterIndex - tokenStart);
                if (token != null)
                {
                    result[resultIndex++] = token;
                }
                tokenStart = splitterIndex + 1;
            }
            String token = this.stringOrNull(source, tokenStart, end - tokenStart);
            if (token != null)
            {
                result[resultIndex++] = token;
            }
            return resultIndex == result.length ? result : compact(result);
        }

        private String stringOrNull(CharSequence source, int start, int length)
        {
            if (!this.strip)
            {
                return this.removeEmpty && length <= 0 ? null : source.subSequence(start, start + length).toString();
            }
            int strippedStart = stripxStart(source, start, length);
            int strippedEnd = stripxEnd(source, start, length);
            if (this.removeEmpty && strippedStart >= strippedEnd)
            {
                return null;
            }
            return strippedStart >= strippedEnd ? "" : source.subSequence(strippedStart, strippedEnd).toString();
        }
    }

    public static boolean isWholeNumber(CharSequence s)
    {
        if (isEmpty(s))
        {
            return false;
        }
        return isWholeNumber(s, 0, s.length());
    }

    public static boolean isWholeNumber(CharSequence s, int start, int length)
    {
        if (s == null || length <= 0)
        {
            return false;
        }

        int end = start + length;
        int digitsStart = (s.charAt(start) == '-' || s.charAt(start) == '+') ? start + 1 : start;

        boolean hasDigit = false;

        for (int i = digitsStart; i < end; ++i)
        {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9')
            {
                hasDigit = true;
            }
            else
            {
                return false;
            }
        }
        return hasDigit;
    }

    public static boolean isInt(CharSequence s)
    {
        return s != null && isInt(s, 0, s.length());
    }

    public static boolean isInt(CharSequence s, int start, int length)
    {
        return isBoundedWholeNumber(s, start, length, "2147483647", "2147483648");
    }

    public static boolean isLong(CharSequence s)
    {
        return s != null && isLong(s, 0, s.length());
    }

    public static boolean isLong(CharSequence s, int start, int length)
    {
        return isBoundedWholeNumber(s, start, length, "9223372036854775807", "9223372036854775808");
    }

    public static boolean isDouble(CharSequence s)
    {
        return s != null && isDouble(s, 0, s.length());
    }

    public static boolean isDouble(CharSequence s, int start, int length)
    {
        if (s == null || length <= 0)
        {
            return false;
        }

        int end = start + length;
        int i = start;
        char first = s.charAt(i);
        if (first == '-' || first == '+')
        {
            ++i;
            if (i >= end)
            {
                return false;
            }
        }

        boolean hasDigit = false;
        while (i < end && isDigit(s.charAt(i)))
        {
            hasDigit = true;
            ++i;
        }

        if (i < end && s.charAt(i) == '.')
        {
            ++i;
            while (i < end && isDigit(s.charAt(i)))
            {
                hasDigit = true;
                ++i;
            }
        }

        if (!hasDigit)
        {
            return false;
        }

        if (i < end && (s.charAt(i) == 'e' || s.charAt(i) == 'E'))
        {
            ++i;
            if (i < end && (s.charAt(i) == '-' || s.charAt(i) == '+'))
            {
                ++i;
            }
            int exponentStart = i;
            while (i < end && isDigit(s.charAt(i)))
            {
                ++i;
            }
            if (i == exponentStart)
            {
                return false;
            }
        }

        return i == end;
    }

    /**
     * Unicode-aware edge stripping equivalent to {@link String#strip()} for
     * {@link CharSequence}.
     */
    public static CharSequence strip(CharSequence s)
    {
        if (s == null)
        {
            return null;
        }
        int length = s.length();
        if (length <= 0)
        {
            return "";
        }
        int strippedStart = stripStart(s, 0, length);
        int strippedEnd = stripEnd(s, 0, length);
        if (strippedStart >= strippedEnd)
        {
            return "";
        }
        if (strippedStart == 0 && strippedEnd == length)
        {
            return s;
        }
        return s.subSequence(strippedStart, strippedEnd);
    }

    /**
     * Returns the inclusive start index of the trimmed slice after Unicode
     * whitespace stripping.
     * <p>
     * This is intended for slice-oriented parsing code that wants trimmed bounds
     * without first creating a {@link CharSequence#subSequence(int, int)} view.
     * Paired with {@link #stripEnd(CharSequence, int, int)}, callers can decide
     * whether a slice changed and only materialize a subsequence if the downstream
     * parser requires one.
     * <p>
     * Typical usage:
     * 
     * <pre>{@code
     * int strippedOffset = Strings.stripStart(s, offset, length);
     * int strippedEnd = Strings.stripEnd(s, offset, length);
     * if (strippedOffset < strippedEnd)
     * {
     *     CharSequence candidate = s.subSequence(strippedOffset, strippedEnd);
     * }
     * }</pre>
     * <p>
     * This is generally preferable to a hypothetical
     * {@code strip(CharSequence, int, int)} helper for low-level parsing, because
     * it lets the caller keep control of whether a new view object is created.
     */
    public static int stripStart(CharSequence s, int start, int length)
    {
        if (s == null || length <= 0)
        {
            return start;
        }
        int strippedStart = start;
        int end = start + length;
        while (strippedStart < end && Character.isWhitespace(s.charAt(strippedStart)))
        {
            ++strippedStart;
        }
        return strippedStart;
    }

    /**
     * Returns the exclusive end index of the trimmed slice after Unicode whitespace
     * stripping.
     * <p>
     * The returned value follows the normal Java {@code start}/{@code end}
     * convention used by {@link CharSequence#subSequence(int, int)} and
     * {@link String#substring(int, int)}:
     * <p>
     * - start is inclusive
     * <p>
     * - end is exclusive
     *
     * @see #stripStart(CharSequence, int, int)
     */
    public static int stripEnd(CharSequence s, int start, int length)
    {
        if (s == null || length <= 0)
        {
            return start;
        }
        int strippedEnd = start + length;
        while (strippedEnd > start && Character.isWhitespace(s.charAt(strippedEnd - 1)))
        {
            --strippedEnd;
        }
        return strippedEnd;
    }

    /**
     * Unicode-aware edge stripping for external text. This behaves like
     * {@link String#strip()} for normal whitespace, and also strips common
     * ingestion artifacts that can appear at text boundaries:
     * <ul>
     * <li>non-breaking space ({@code U+00A0}), often copied from HTML, PDFs, and
     * spreadsheets</li>
     * <li>zero-width space/non-joiner/joiner ({@code U+200B}, {@code U+200C},
     * {@code U+200D}), often introduced by copy/paste, web text, PDFs, or rich text
     * editors</li>
     * <li>byte order mark ({@code U+FEFF}), often found at file or field
     * boundaries</li>
     * <li>replacement character ({@code U+FFFD}), commonly produced by encoding
     * damage</li>
     * </ul>
     */
    public static CharSequence stripx(CharSequence s)
    {
        return s == null ? null : stripx(s, 0, s.length());
    }

    public static CharSequence stripx(CharSequence s, int start, int length)
    {
        if (s == null)
        {
            return null;
        }
        if (length <= 0)
        {
            return "";
        }

        int end = start + length - 1;
        if (!isStrippable(s.charAt(start)) && !isStrippable(s.charAt(end)))
        {
            return start == 0 && length == s.length() ? s : s.subSequence(start, start + length);
        }

        int actualStart = start;
        int actualEnd = end;

        while (actualStart <= actualEnd && isStrippable(s.charAt(actualStart)))
        {
            actualStart++;
        }

        while (actualEnd >= actualStart && isStrippable(s.charAt(actualEnd)))
        {
            actualEnd--;
        }

        if (actualStart > actualEnd)
        {
            return "";
        }
        if (actualStart == start && actualEnd == end)
        {
            return start == 0 && length == s.length() ? s : s.subSequence(start, start + length);
        }
        return s.subSequence(actualStart, actualEnd + 1);
    }

    /**
     * Strips external text with {@link #stripx(CharSequence)}, normalises any
     * remaining whitespace run to a single space, and removes selected characters.
     * <p>
     * Passing {@code ' '} as one of the removable characters removes all normalised
     * whitespace, including tabs and other whitespace characters. This is useful
     * for cleaning human-readable tokens such as {@code half-up}, {@code half_up},
     * and {@code half up} to the same key before case conversion.
     *
     * @param s
     *            source text
     * @param removeCharacters
     *            characters to remove after whitespace normalisation
     * @return normalised text or {@code null}
     */
    public static String clean(CharSequence s, char... removeCharacters)
    {
        CharSequence stripped = stripx(s);
        if (stripped == null)
        {
            return null;
        }
        StringBuilder out = null;
        boolean previousWhitespace = false;
        for (int i = 0; i < stripped.length(); ++i)
        {
            char original = stripped.charAt(i);
            char c = original;
            if (Character.isWhitespace(c) || c == '\u00A0')
            {
                c = ' ';
            }
            if (removeCharacters != null && isAny(c, removeCharacters))
            {
                if (out == null)
                {
                    out = new StringBuilder(stripped.length());
                    out.append(stripped, 0, i);
                }
                previousWhitespace = false;
                continue;
            }
            if (c == ' ')
            {
                if (previousWhitespace)
                {
                    if (out == null)
                    {
                        out = new StringBuilder(stripped.length());
                        out.append(stripped, 0, i);
                    }
                    continue;
                }
                previousWhitespace = true;
            }
            else
            {
                previousWhitespace = false;
            }
            if (out == null && c != original)
            {
                out = new StringBuilder(stripped.length());
                out.append(stripped, 0, i);
            }
            if (out != null)
            {
                out.append(c);
            }
        }
        return out == null ? stripped.toString() : out.toString();
    }

    public static CharSequence removeDiacritics(CharSequence s)
    {
        return s == null ? null : removeDiacritics(s, 0, s.length());
    }

    public static CharSequence removeDiacritics(CharSequence s, int start, int length)
    {
        if (s == null)
        {
            return null;
        }
        if (length <= 0)
        {
            return "";
        }
        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            if (s.charAt(i) > 127)
            {
                return removeDiacriticsSlow(s, start, length);
            }
        }
        return start == 0 && length == s.length() ? s : s.subSequence(start, end);
    }

    private static String removeDiacriticsSlow(CharSequence s, int start, int length)
    {
        CharSequence source = start == 0 && length == s.length() ? s : s.subSequence(start, start + length);
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFKD);
        StringBuilder out = null;
        for (int i = 0; i < normalized.length(); ++i)
        {
            char c = normalized.charAt(i);
            if (isDiacriticMark(c))
            {
                if (out == null)
                {
                    out = new StringBuilder(normalized.length());
                    out.append(normalized, 0, i);
                }
                continue;
            }
            if (out != null)
            {
                out.append(c);
            }
        }
        return out == null ? normalized : out.toString();
    }

    private static boolean isDiacriticMark(char c)
    {
        int type = Character.getType(c);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    private static int stripxStart(CharSequence s, int start, int length)
    {
        if (s == null || length <= 0)
        {
            return start;
        }
        int strippedStart = start;
        int end = start + length;
        while (strippedStart < end && isStrippable(s.charAt(strippedStart)))
        {
            ++strippedStart;
        }
        return strippedStart;
    }

    private static int stripxEnd(CharSequence s, int start, int length)
    {
        if (s == null || length <= 0)
        {
            return start;
        }
        int strippedEnd = start + length;
        while (strippedEnd > start && isStrippable(s.charAt(strippedEnd - 1)))
        {
            --strippedEnd;
        }
        return strippedEnd;
    }

    private static boolean isStrippable(char c)
    {
        return Character.isWhitespace(c) || c == '\u00A0' || c == '\u200B' || c == '\u200C' || c == '\u200D'
                || c == '\uFEFF' || c == '\uFFFD';
    }

    private static int indexOfAny2(CharSequence s, int start, int length, char c1, char c2)
    {
        if (s == null || length <= 0)
        {
            return -1;
        }

        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            char c = s.charAt(i);
            if (c == c1 || c == c2)
            {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfAny3(CharSequence s, int start, int length, char c1, char c2, char c3)
    {
        if (s == null || length <= 0)
        {
            return -1;
        }

        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            char c = s.charAt(i);
            if (c == c1 || c == c2 || c == c3)
            {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfAny4(CharSequence s, int start, int length, char c1, char c2, char c3, char c4)
    {
        if (s == null || length <= 0)
        {
            return -1;
        }

        int end = start + length;
        for (int i = start; i < end; ++i)
        {
            char c = s.charAt(i);
            if (c == c1 || c == c2 || c == c3 || c == c4)
            {
                return i;
            }
        }
        return -1;
    }

    private static boolean isAny(char c, char[] values)
    {
        for (char value : values)
        {
            if (c == value)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isBoundedWholeNumber(CharSequence s, int start, int length, String positiveBound,
            String negativeMagnitudeBound)
    {
        if (!isWholeNumber(s, start, length))
        {
            return false;
        }

        boolean negative = s.charAt(start) == '-';
        int digitsStart = (negative || s.charAt(start) == '+') ? start + 1 : start;
        int digits = length - (digitsStart - start);
        String bound = negative ? negativeMagnitudeBound : positiveBound;

        if (digits < bound.length())
        {
            return true;
        }
        if (digits > bound.length())
        {
            return false;
        }

        for (int i = 0; i < digits; ++i)
        {
            char a = s.charAt(digitsStart + i);
            char b = bound.charAt(i);
            if (a < b)
            {
                return true;
            }
            if (a > b)
            {
                return false;
            }
        }
        return true;
    }
}
