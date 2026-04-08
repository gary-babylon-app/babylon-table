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

public final class Strings
{
    private Strings()
    {
    }

    public static String toCamelUpperPreserve(String x)
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
            return Character.toUpperCase(x.charAt(0)) + x.substring(1);
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

    public static String leftPad(String s, int size, char padChar)
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
        return builder.toString();
    }

    public static String rightPad(String s, int size, char padChar)
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

    public static boolean isEmpty(CharSequence s)
    {
        return s == null || s.length() == 0;
    }

    /**
     * Unicode-aware edge stripping (like {@link String#strip()}) plus
     * additional ingestion cleanup characters.
     */
    public static String stripx(CharSequence s)
    {
        if (s == null)
        {
            return null;
        }
        if (s.length() == 0)
        {
            return s.toString();
        }

        int start = 0;
        int end = s.length() - 1;

        while (start <= end && isStrippable(s.charAt(start)))
        {
            start++;
        }

        while (end >= start && isStrippable(s.charAt(end)))
        {
            end--;
        }

        if (start == 0 && end == s.length() - 1)
        {
            return s.toString();
        }
        return s.subSequence(start, end + 1).toString();
    }

    public static String stripx(char[] chars, int start, int length)
    {
        if (chars == null)
        {
            return null;
        }
        if (length <= 0)
        {
            return "";
        }

        int actualStart = start;
        int actualEnd = start + length - 1;

        while (actualStart <= actualEnd && isStrippable(chars[actualStart]))
        {
            actualStart++;
        }

        while (actualEnd >= actualStart && isStrippable(chars[actualEnd]))
        {
            actualEnd--;
        }

        if (actualStart > actualEnd)
        {
            return "";
        }
        return new String(chars, actualStart, actualEnd - actualStart + 1);
    }

    private static boolean isStrippable(char c)
    {
        return Character.isWhitespace(c)
            || c == '\u00A0'
            || c == '\u200B'
            || c == '\u200C'
            || c == '\u200D'
            || c == '\uFEFF'
            || c == '\uFFFD';
    }
}
