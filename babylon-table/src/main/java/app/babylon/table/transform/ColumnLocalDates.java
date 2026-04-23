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

/**
 * Local-date helpers for string and typed date columns.
 */
public final class ColumnLocalDates
{
    private ColumnLocalDates()
    {
    }

    /**
     * Parses a date using the default YMD interpretation.
     *
     * @param x
     *            source text
     * @return parsed date or {@code null}
     */
    public static LocalDate stringToDate(CharSequence x)
    {
        DateValueFacts facts = DateValueFacts.from(x);
        if (facts == null)
        {
            return null;
        }

        return facts.toLocalDate(DateFormat.YMD);
    }

    /**
     * Parses a date using the supplied format.
     *
     * @param x
     *            source text
     * @param format
     *            date format to use
     * @return parsed date or {@code null}
     */
    public static LocalDate stringToDate(CharSequence x, DateFormat format)
    {
        DateValueFacts facts = DateValueFacts.from(x);
        if (facts == null)
        {
            return null;
        }

        return facts.toLocalDate(format);
    }

    /**
     * Returns whether every set value in the string column looks like a date.
     *
     * @param c
     *            string column to inspect
     * @return {@code true} when the column has a known inferred date format
     */
    public static boolean isAllDates(ColumnObject<String> c)
    {
        return DateFormatInference.inferFormat(c) != DateFormat.Unknown;
    }

    /**
     * Returns whether the column is already local-date typed or can be inferred as
     * local-date text.
     *
     * @param column
     *            column to inspect
     * @return {@code true} when the column represents local dates
     */
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

    /**
     * Returns the minimum set value in a local-date column.
     *
     * @param column
     *            local-date column
     * @return minimum set date
     */
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
