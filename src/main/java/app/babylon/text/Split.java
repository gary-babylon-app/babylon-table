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

    public static List<String> character(CharSequence input, char separator)
    {
        if (input == null)
        {
            return List.of();
        }
        return list(Strings.splitter().withSplitter(separator).withStripping(false).split(input));
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

    public static String[] whitespace(String input)
    {
        if (input == null)
        {
            return new String[0];
        }
        List<String> words = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < input.length(); ++i)
        {
            if (Character.isWhitespace(input.charAt(i)))
            {
                if (start >= 0)
                {
                    words.add(input.substring(start, i));
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
            words.add(input.substring(start));
        }
        return words.toArray(new String[0]);
    }

    private static List<String> list(String[] values)
    {
        List<String> list = new ArrayList<>(values.length);
        for (String value : values)
        {
            list.add(value);
        }
        return list;
    }
}
