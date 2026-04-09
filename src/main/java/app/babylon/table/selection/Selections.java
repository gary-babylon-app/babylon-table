/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.selection;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.text.Strings;

public class Selections
{
    public static TableColumnar select(TableColumnar table, ColumnName colName, Pattern matchingPattern)
    {
        ColumnObject<String> column = table.getString(colName);
        Selection selection = new Selection(column.getName() + " matchs pattern " + matchingPattern.pattern());

        Set<String> uniques = (Set<String>) column.getAll(new HashSet<>());
        Set<String> matched = new HashSet<>();

        for (String s : uniques)
        {
            Matcher m = matchingPattern.matcher(s);
            if (m.find())
            {
                matched.add(s);
            }
        }
        for (int i = 0; i < column.size(); ++i)
        {
            String s = column.get(i);
            if (!Strings.isEmpty(s) && matched.contains(s))
            {
                selection.add(true);
            } else
            {
                selection.add(false);
            }
        }
        return Tables.select(table, selection);
    }

    public static <T> Selection eq(ColumnObject<T> column, T value)
    {
        ColumnObject<T> c = ArgumentCheck.nonNull(column);
        Selection selection = new Selection(c.getName() + " == " + value);
        for (int i = 0; i < c.size(); ++i)
        {
            T item = c.get(i);
            selection.add(item != null && item.equals(value));
        }
        return selection;
    }

    public static <T> Selection ne(ColumnObject<T> column, T value)
    {
        ColumnObject<T> c = ArgumentCheck.nonNull(column);
        Selection selection = new Selection(c.getName() + " != " + value);
        for (int i = 0; i < c.size(); ++i)
        {
            T item = c.get(i);
            selection.add(item != null && !item.equals(value));
        }
        return selection;
    }

    public static <T> Selection in(ColumnObject<T> column, T[] values)
    {
        ColumnObject<T> c = ArgumentCheck.nonNull(column);
        T[] v = ArgumentCheck.nonNull(values);
        Selection selection = new Selection(c.getName() + " in " + v);
        for (int i = 0; i < c.size(); ++i)
        {
            T item = c.get(i);
            boolean select = false;
            if (item != null)
            {
                for (int j = 0; j < v.length; ++j)
                {
                    if (item.equals(v[j]))
                    {
                        select = true;
                        break;
                    }
                }
            }
            selection.add(select);
        }
        return selection;
    }

    public static <T> Selection nin(ColumnObject<T> column, T[] values)
    {
        ColumnObject<T> c = ArgumentCheck.nonNull(column);
        T[] v = ArgumentCheck.nonNull(values);
        Selection selection = new Selection(c.getName() + " not in " + v);
        for (int i = 0; i < c.size(); ++i)
        {
            T item = c.get(i);
            boolean select = true;
            if (item != null)
            {
                for (int j = 0; j < v.length; ++j)
                {
                    if (item.equals(v[j]))
                    {
                        select = false;
                        break;
                    }
                }
            }
            selection.add(select);
        }
        return selection;
    }
}
