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

import java.util.Collection;

public final class ArgumentChecks
{
    private ArgumentChecks()
    {
    }

    public static int nonNegative(int i)
    {
        if (i < 0)
        {
            throw new RuntimeException("Expected a non-negative value.");
        }
        return i;
    }

    public static int[] nonEmpty(int[] x)
    {
        if (x == null || x.length == 0)
        {
            throw new RuntimeException("Require non-empty array.");
        }
        return x;
    }

    public static <T> T[] nonEmpty(T[] x)
    {
        if (x == null || x.length == 0)
        {
            throw new RuntimeException("Require non-empty array.");
        }
        return x;
    }

    public static <T extends Collection<?>> T nonEmpty(T c)
    {
        if (c == null || c.isEmpty())
        {
            throw new RuntimeException("Require non-empty collection.");
        }
        return c;
    }

    public static String nonEmpty(String s)
    {
        if (s == null || s.isEmpty())
        {
            throw new RuntimeException("Require non-empty String.");
        }
        return s;
    }

    public static String nonEmptyAsString(CharSequence s)
    {
        if (s == null || s.length() == 0)
        {
            throw new RuntimeException("Require non-empty String.");
        }
        return s.toString();
    }

    public static <T> T nonNull(T o)
    {
        if (o == null)
        {
            throw new RuntimeException("Require non-null value.");
        }
        return o;
    }
}
