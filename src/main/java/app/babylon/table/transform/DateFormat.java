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

public enum DateFormat
{
    DMY, YMD, MDY, ExcelLocalDate, ExcelLocalDateTime, Unknown;

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
