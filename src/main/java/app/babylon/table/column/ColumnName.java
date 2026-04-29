/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.text.Strings;

/**
 * Canonical, case-insensitive table column name.
 */
public final class ColumnName implements Comparable<ColumnName>
{
    /** Human-facing/exported heading (always CamelUpper). */
    private final String value;

    /**
     * Canonical identity key (ASCII [a-z0-9]*; underscores/dashes removed by
     * design).
     */
    private final String canonical;

    // ---------- Factories ----------

    /**
     * Creates a column name from raw text.
     *
     * @param s
     *            source text
     * @return column name
     */
    public static ColumnName of(CharSequence s)
    {
        if (s == null)
        {
            throw new RuntimeException("Empty string for column name.");
        }
        return of(s, 0, s.length());
    }

    public static ColumnName of(CharSequence s, int start, int length)
    {
        if (Strings.isStripxEmpty(s, start, length))
        {
            throw new RuntimeException("Empty string for column name.");
        }
        return new ColumnName(s, start, length);
    }

    /**
     * Parses a column name, returning {@code null} for empty text.
     *
     * @param s
     *            source text
     * @return parsed column name or {@code null}
     */
    public static ColumnName parse(CharSequence s)
    {
        return s == null ? null : parse(s, 0, s.length());
    }

    public static ColumnName parse(CharSequence s, int start, int length)
    {
        return Strings.isStripxEmpty(s, start, length) ? null : of(s, start, length);
    }

    /**
     * Converts a collection of strings into column names.
     *
     * @param v
     *            source names
     * @return column-name array
     */
    public static ColumnName[] of(Collection<String> v)
    {
        if (Is.empty(v))
        {
            return new ColumnName[0];
        }
        ColumnName[] c = new ColumnName[v.size()];
        int i = 0;
        for (String s : v)
        {
            c[i++] = ColumnName.of(s);
        }
        return c;
    }

    /**
     * Converts an array of strings into column names.
     *
     * @param v
     *            source names
     * @return column-name array
     */
    public static ColumnName[] of(String[] v)
    {
        return of(v, 0);
    }

    /**
     * Converts a tail slice of a string array into column names.
     *
     * @param v
     *            source names
     * @param startIndex
     *            first index to include
     * @return column-name array
     */
    public static ColumnName[] of(String[] v, int startIndex)
    {
        if (Is.empty(v) || (v.length - startIndex) <= 0)
        {
            return new ColumnName[0];
        }
        ColumnName[] c = new ColumnName[v.length - startIndex];
        for (int i = startIndex; i < v.length; ++i)
        {
            c[i - startIndex] = ColumnName.of(v[i]);
        }
        return c;
    }

    private ColumnName(CharSequence input, int start, int length)
    {
        String tokens = normalizeForTokens(input, start, length);
        this.value = ArgumentCheck.nonEmpty(Strings.toCamelUpperPreserve(tokens).toString());
        this.canonical = ArgumentCheck.nonEmpty(buildCanonicalKey(input, start, length));
    }

    private static String normalizeForTokens(CharSequence s, int start, int length)
    {
        if (s == null)
        {
            return "";
        }
        return Strings.removeDiacritics(Strings.stripx(s, start, length)).toString();
    }

    private static String buildCanonicalKey(CharSequence s, int start, int length)
    {
        if (Strings.isStripxEmpty(s, start, length))
        {
            return "";
        }
        CharSequence n = Strings.removeDiacritics(s, start, length);
        StringBuilder out = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++)
        {
            char c = Character.toLowerCase(n.charAt(i));
            if (Character.isLetterOrDigit(c))
            {
                out.append(c);
            }
        }
        return out.toString();
    }

    @Override
    public String toString()
    {
        return value;
    }

    /**
     * Returns the exported display value of the column name.
     *
     * @return display value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Returns the canonical comparison key.
     *
     * @return canonical key
     */
    public String getCanonical()
    {
        return canonical;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof ColumnName that)
        {
            return this.canonical.equals(that.canonical);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(canonical);
    }

    @Override
    public int compareTo(ColumnName o)
    {
        return this.canonical.compareTo(o.canonical);
    }

    /**
     * Returns a snake-case representation of the name.
     *
     * @return snake-case text
     */
    public String toSnake()
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        char previous = 0;
        for (char c : value.toCharArray())
        {
            boolean letterToDigitBoundary = !first && Character.isLetter(previous) && Character.isDigit(c);
            if (Character.isUpperCase(c))
            {
                if (!first)
                {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            }
            else if (letterToDigitBoundary)
            {
                result.append('_').append(c);
            }
            else
            {
                result.append(c);
            }
            first = false;
            previous = c;
        }
        return result.toString();
    }

    /**
     * Splits the name into camel-case words.
     *
     * @param words
     *            destination collection or {@code null}
     * @return destination collection containing the words
     */
    public Collection<String> toWords(Collection<String> words)
    {
        if (words == null)
        {
            words = new ArrayList<>();
        }

        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (char c : value.toCharArray())
        {
            if (Character.isUpperCase(c))
            {
                if (!first)
                {
                    words.add(result.toString());
                    result.setLength(0);
                }
                result.append(c);
            }
            else
            {
                result.append(c);
            }
            first = false;
        }
        if (result.length() > 0)
        {
            words.add(result.toString());
        }
        return words;
    }

    /**
     * Returns a SQL-friendly identifier for the name.
     *
     * @return quoted SQL identifier
     */
    public String toSqlIdentifier()
    {
        String snake = toSnake();
        if (snake.length() == 0 || snake.length() > 64)
        {
            throw new IllegalStateException(snake + " invalid length for sql column.");
        }

        char c = snake.charAt(0);
        if (!Strings.isAlpha(c))
        {
            throw new IllegalStateException(snake + " must start with an alpha.");
        }
        for (int i = 1; i < snake.length(); ++i)
        {
            c = snake.charAt(i);
            if (!Strings.isAlphaNumeric(c) && c != '_')
            {
                throw new IllegalStateException(snake + " " + c + " must be alphaNumeric.");
            }
        }

        return "`" + snake + "`";
    }

    /**
     * Returns the CamelUpper representation of the name.
     *
     * @return CamelUpper text
     */
    public String toCamelCaseUpper()
    {
        return value;
    }

    /**
     * Returns the camelCase representation of the name.
     *
     * @return camelCase text
     */
    public String toCamelCase()
    {
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1, value.length());
    }

    /**
     * Returns the canonical cleaned form of arbitrary column-name text.
     *
     * @param s
     *            source text
     * @return cleaned canonical key
     */
    public static String clean(String s)
    {
        return Strings.isEmpty(s) ? "" : buildCanonicalKey(s, 0, s.length());
    }

}
