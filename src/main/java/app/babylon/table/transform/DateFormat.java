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

import java.util.Locale;

/**
 * Supported date token orderings and special date encodings.
 */
public enum DateFormat
{
    /** Day-month-year order. */
    DMY,
    /** Year-month-day order. */
    YMD,
    /** Month-day-year order. */
    MDY,
    /** Excel serial date without time. */
    ExcelLocalDate,
    /** Excel serial date/time value. */
    ExcelLocalDateTime,
    /** Unknown or ambiguous format. */
    Unknown;

    /**
     * Parses a named date format such as {@code DMY}, {@code MDY}, or {@code YMD}.
     *
     * @param s
     *            source text
     * @return parsed date format or {@code null}
     */
    public static DateFormat parse(String s)
    {
        if (s == null || s.isBlank())
        {
            return null;
        }
        s = s.toUpperCase(Locale.UK).strip();
        switch (s)
        {
            case "DMY" -> {
                return DMY;
            }
            case "MDY" -> {
                return MDY;
            }
            case "YMD" -> {
                return YMD;
            }
            default -> {
                return null;
            }
        }

    }
};
