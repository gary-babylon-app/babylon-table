/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.lang;

import java.util.Collection;
import java.util.Objects;

import app.babylon.text.Strings;

/**
 * Small argument validation helpers used throughout the library.
 */
public final class ArgumentCheck
{
    private ArgumentCheck()
    {
    }

    /**
     * Requires a non-negative integer.
     *
     * @param i
     *            value to validate
     * @return the supplied value
     */
    public static int nonNegative(int i)
    {
        if (i < 0)
        {
            throw new RuntimeException("Expected a non-negative value.");
        }
        return i;
    }

    /**
     * Requires a non-empty array.
     *
     * @param x
     *            array to validate
     * @param <T>
     *            array element type
     * @return the supplied array
     */
    public static <T> T[] nonEmpty(T[] x)
    {
        if (x == null || x.length == 0)
        {
            throw new RuntimeException("Require non-empty array.");
        }
        return x;
    }

    /**
     * Requires a non-empty collection.
     *
     * @param c
     *            collection to validate
     * @param <T>
     *            collection type
     * @return the supplied collection
     */
    public static <T extends Collection<?>> T nonEmpty(T c)
    {
        if (c == null || c.isEmpty())
        {
            throw new RuntimeException("Require non-empty collection.");
        }
        return c;
    }

    /**
     * Requires a non-empty string.
     *
     * @param s
     *            string to validate
     * @return the supplied string
     */
    public static String nonEmpty(String s)
    {
        if (Strings.isEmpty(s))
        {
            throw new RuntimeException("Require non-empty String.");
        }
        return s;
    }

    /**
     * Requires a non-null reference.
     *
     * @param o
     *            object to validate
     * @param <T>
     *            object type
     * @return the supplied object
     */
    public static <T> T nonNull(T o)
    {
        return Objects.requireNonNull(o);
    }

    /**
     * Requires a non-null reference with a custom error message.
     *
     * @param o
     *            object to validate
     * @param message
     *            null failure message
     * @param <T>
     *            object type
     * @return the supplied object
     */
    public static <T> T nonNull(T o, String message)
    {
        return Objects.requireNonNull(o, message);
    }

}
