/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

public record TableDescription(String value) implements CharSequence
{
    public TableDescription()
    {
        this("");
    }

    public TableDescription
    {
        value = normalize(value);
    }

    public String getValue()
    {
        return value();
    }

    @Override
    public String toString()
    {
        return this.value;
    }

    @Override
    public int length()
    {
        return this.value.length();
    }

    @Override
    public char charAt(int index)
    {
        return this.value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return this.value.subSequence(start, end);
    }

    private static String normalize(String value)
    {
        if (value == null)
        {
            return "";
        }

        StringBuilder cleaned = new StringBuilder(value.length());
        boolean lastWasSpace = true;

        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c))
            {
                if (!lastWasSpace)
                {
                    cleaned.append(' ');
                    lastWasSpace = true;
                }
            }
            else
            {
                cleaned.append(c);
                lastWasSpace = false;
            }
        }

        int length = cleaned.length();
        if (length > 0 && cleaned.charAt(length - 1) == ' ')
        {
            cleaned.setLength(length - 1);
        }

        return cleaned.toString();
    }
}
