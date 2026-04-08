/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.transform;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnObject;
import java.time.LocalDate;

public final class ColumnLocalDates
{
    private ColumnLocalDates()
    {
    }

    public static LocalDate stringToDate(CharSequence x)
    {
        DateValueFacts facts = DateValueFacts.from(x);
        if (facts == null)
        {
            return null;
        }

        return facts.toLocalDate(DateFormat.YMD);
    }

    public static LocalDate stringToDate(CharSequence x, DateFormat format)
    {
        DateValueFacts facts = DateValueFacts.from(x);
        if (facts == null)
        {
            return null;
        }

        return facts.toLocalDate(format);
    }

    public static boolean isAllDates(ColumnObject<String> c)
    {
        return DateFormatInference.inferFormat(c) != DateFormat.Unknown;
    }

    public static boolean isLocalDate(Column column)
    {
        if (column instanceof ColumnObject<?> && LocalDate.class.equals(column.getType().getValueClass()))
        {
            return true;
        }
        if (column instanceof ColumnObject<?> co && String.class.equals(column.getType().getValueClass()))
        {
            @SuppressWarnings("unchecked")
            ColumnObject<String> cs = (ColumnObject<String>) co;
            return isAllDates(cs);
        }
        return false;
    }

    public static LocalDate getMinimum(ColumnObject<LocalDate> column)
    {
        if (column == null || column.size() == 0)
        {
            throw new RuntimeException("Can not compute min on column with no values.");
        }

        LocalDate min = null;
        for (int i = 0; i < column.size(); ++i)
        {
            if (column.isSet(i))
            {
                LocalDate v = column.get(i);
                if (min == null || min.compareTo(v) > 0)
                {
                    min = v;
                }
            }
        }
        if (min == null)
        {
            throw new RuntimeException("Can not compute min on column with no set values. " + column.getName());
        }
        return min;
    }
}
