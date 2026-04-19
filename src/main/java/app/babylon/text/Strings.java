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

    public static boolean isEmpty(CharSequence s)
    {
        return s == null || s.length() == 0;
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

    public static boolean isLong(String s)
    {
        return s != null && isLong(s, 0, s.length());
    }

    public static boolean isLong(CharSequence s, int start, int length)
    {
        return isBoundedWholeNumber(s, start, length, "9223372036854775807", "9223372036854775808");
    }

    /**
     * Unicode-aware edge stripping equivalent to {@link String#strip()} for
     * {@link CharSequence}.
     */
    public static CharSequence strip(CharSequence s)
    {
        return s == null ? null : strip(s, 0, s.length());
    }

    public static CharSequence strip(CharSequence s, int start, int length)
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
        if (!Character.isWhitespace(s.charAt(start)) && !Character.isWhitespace(s.charAt(end)))
        {
            return start == 0 && length == s.length() ? s : s.subSequence(start, start + length);
        }

        int actualStart = start;
        int actualEnd = end;

        while (actualStart <= actualEnd && Character.isWhitespace(s.charAt(actualStart)))
        {
            actualStart++;
        }

        while (actualEnd >= actualStart && Character.isWhitespace(s.charAt(actualEnd)))
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
     * Unicode-aware edge stripping (like {@link String#strip()}) plus additional
     * ingestion cleanup characters.
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

    private static boolean isStrippable(char c)
    {
        return Character.isWhitespace(c) || c == '\u00A0' || c == '\u200B' || c == '\u200C' || c == '\u200D'
                || c == '\uFEFF' || c == '\uFFFD';
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
