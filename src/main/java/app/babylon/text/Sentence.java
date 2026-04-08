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

import java.util.List;
import java.util.function.Function;

public final class Sentence
{
    private Sentence()
    {
    }

    public static <T> T firstIn(Function<CharSequence, T> parser, CharSequence sentence)
    {
        List<String> words = Split.anyChars(sentence, " ");

        for (String word : words)
        {
            T value = parser.apply(word);
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }

    public static <T> T lastIn(Function<CharSequence, T> parser, String sentence)
    {
        return lastIn(parser, sentence, " ");
    }

    public static <T> T lastIn(Function<CharSequence, T> parser, String sentence, String separators)
    {
        List<String> words = Split.anyChars(sentence, separators);

        for (int i = words.size() - 1; i >= 0; --i)
        {
            T value = parser.apply(words.get(i));
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }

    public static <T> T onlyOneIn(Function<CharSequence, T> parser, CharSequence sentence)
    {
        List<String> words = Split.anyChars(sentence, " ");
        T only = null;

        for (String word : words)
        {
            T value = parser.apply(word);
            if (value != null)
            {
                if (only != null)
                {
                    return null;
                }
                only = value;
            }
        }
        return only;
    }

    public static <T> T onlyIn(Function<CharSequence, T> parser, CharSequence sentence)
    {
        return onlyOneIn(parser, sentence);
    }
}
