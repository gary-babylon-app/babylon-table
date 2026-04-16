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

import app.babylon.lang.ArgumentCheck;

import app.babylon.lang.Is;
import app.babylon.text.Strings;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ColumnName implements Comparable<ColumnName>
{
    private static final Pattern P_DIACRITICS = Pattern.compile("\\p{M}+");

    /** Human-facing/exported heading (always CamelUpper). */
    private final String value;

    /**
     * Canonical identity key (ASCII [a-z0-9]*; underscores/dashes removed by
     * design).
     */
    private final String canonical;

    // ---------- Factories ----------

    public static ColumnName of(String s)
    {
        if (Strings.isEmpty(s))
        {
            throw new RuntimeException("Empty string for column name.");
        }
        return new ColumnName(s);
    }

    public static ColumnName parse(CharSequence s)
    {
        return Strings.isEmpty(s) ? null : of(s.toString());
    }

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

    public static ColumnName[] of(String[] v)
    {
        return of(v, 0);
    }

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

    private ColumnName(String input)
    {
        String tokens = normalizeForTokens(input);
        this.value = ArgumentCheck.nonEmpty(Strings.toCamelUpperPreserve(tokens));
        this.canonical = ArgumentCheck.nonEmpty(buildCanonicalKey(input));
    }

    private static String normalizeForTokens(String s)
    {
        if (s == null)
        {
            return "";
        }
        return removeDiacritics(s.strip());
    }

    private static String buildCanonicalKey(String s)
    {
        if (Strings.isEmpty(s))
        {
            return "";
        }
        String n = removeDiacritics(s);
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

    private static String removeDiacritics(String s)
    {
        String n = Normalizer.normalize(s, Normalizer.Form.NFKD);
        return P_DIACRITICS.matcher(n).replaceAll("");
    }

    @Override
    public String toString()
    {
        return value;
    }

    public String getValue()
    {
        return value;
    }

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

    public String toCamelCaseUpper()
    {
        return value;
    }

    public String toCamelCase()
    {
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1, value.length());
    }

    public static String clean(String s)
    {
        return Strings.isEmpty(s) ? "" : buildCanonicalKey(s);
    }

    public static String[] toStringArray(Collection<ColumnName> x)
    {
        if (x == null)
        {
            return null;
        }
        String[] results = new String[x.size()];
        int i = 0;
        for (ColumnName cn : x)
        {
            results[i++] = cn.getValue();
        }
        return results;
    }
}
