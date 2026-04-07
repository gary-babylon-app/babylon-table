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
import java.util.Map;

public class Is
{
    public static boolean empty(int[] x)
    {
        return (x==null || x.length==0);
    }

    public static <T> boolean empty(Collection<T> x)
    {
        return (x==null || x.size()==0);
    }

    public static <U,V> boolean empty(Map<U, V> x)
    {
        return (x==null || x.size()==0);
    }

    public static <T> boolean empty(T[] x)
    {
        return (x==null || x.length==0);
    }

    public static boolean empty(CharSequence s)
    {
        return (s==null || s.length()==0);
    }

    public static boolean alphaUpper(char c)
    {
        return (c>='A' && c<='Z');
    }

    public static boolean alphaNumeric(char c)
    {
        return (c>='a' && c<='z') || (c>='A' && c<='Z') || digit(c);
    }

    public static boolean digit(char c)
    {
        return c >= '0' && c <= '9';
    }

    public static boolean strictLeftDecimal(String s, int until)
    {
        if (Is.empty(s))
        {
            return false;
        }

        int len = Math.min(until, s.length());
        int start = (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;

        boolean hasDigit = false;
        boolean hasDot = false;

        for (int i = start; i < len; i++)
        {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9')
            {
                hasDigit = true;
            }
            else if (c == '.')
            {
                if (hasDot)
                {
                    return false; // second dot → invalid
                }
                hasDot = true;
            }
            else
            {
                return false; // invalid char
            }
        }
        return hasDigit;
    }


    public static boolean decimal(String s)
    {
        return strictLeftDecimal(s, s.length());
    }

    public static boolean integer(String s)
    {
        if (Is.empty(s))
        {
            return false;
        }

        int len = s.length();
        int start = (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;

        boolean hasDigit = false;

        for (int i = start; i < len; ++i)
        {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9')
            {
                hasDigit = true;
            }
            else
            {
                return false; // invalid char
            }
        }
        return hasDigit;
    }
}
