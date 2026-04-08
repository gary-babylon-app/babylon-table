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

import app.babylon.text.Strings;

public final class TableName implements Comparable<TableName>
{
    private String original;
    private String clean;

    public static TableName of(String s)
    {
        return new TableName(s);
    }
    private TableName(String s)
    {
        this.original = ArgumentChecks.nonEmpty(s);
        this.clean = ArgumentChecks.nonEmpty(clean(s));
    }

    @Override
    public String toString()
    {
        return original;
    }

    public String getClean()
    {
        return clean;
    }

    public String getOriginal()
    {
        return original;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof TableName)
        {
            TableName that = (TableName) obj;
            return this.clean.equals(that.clean);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return clean.hashCode();
    }

    @Override
    public int compareTo(TableName o)
    {
        return this.clean.compareTo(o.clean);
    }

    public static String clean(String s)
    {
        if (Strings.isEmpty(s))
        {
            return "";
        }

        // TODO optimise
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (Strings.isAlphaNumeric(c))
            {
                c = Character.toLowerCase(c);
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
