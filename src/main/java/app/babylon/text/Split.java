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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Split
{

    public static List<String> anyChars(CharSequence input, String separators)
    {
        if (input == null)
        {
            return List.of();
        }
        if (Strings.isEmpty(separators))
        {
            separators = " ";
        }
        List<String> words = new ArrayList<>();
        String s = input.toString();
        int start = -1;
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (isSeparator(c, separators))
            {
                if (start >= 0)
                {
                    words.add(s.substring(start, i));
                    start = -1;
                }
            }
            else if (start < 0)
            {
                start = i;
            }
        }
        if (start >= 0)
        {
            words.add(s.substring(start));
        }
        return words;
    }

    public static String[] literal(String input, String delimiter, boolean preserveEmpty)
    {
        if (input == null)
        {
            return new String[0];
        }
        if (delimiter == null || delimiter.isEmpty())
        {
            return new String[]
            {input};
        }
        return input.split(Pattern.quote(delimiter), preserveEmpty ? -1 : 0);
    }

    public static String[] commaSeparatedParams(String input)
    {
        if (input == null)
        {
            return new String[0];
        }
        return input.split("\\s*,\\s*");
    }

    public static String[] whitespace(String input)
    {
        if (input == null)
        {
            return new String[0];
        }
        String stripped = input.strip();
        if (stripped.isEmpty())
        {
            return new String[0];
        }
        return stripped.split("\\s+");
    }

    private static boolean isSeparator(char c, String separators)
    {
        for (int i = 0; i < separators.length(); ++i)
        {
            if (separators.charAt(i) == c)
            {
                return true;
            }
        }
        return false;
    }
}
