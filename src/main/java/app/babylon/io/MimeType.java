/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.io;
import java.util.function.Function;

public enum MimeType
{
    APPLICATION_PDF("application/pdf"), EXCEL_XLSX(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), TEXT_CSV(
                    "text/csv"), TEXT_PLAIN("text/plain");

    public static final Function<CharSequence, MimeType> PARSER = MimeType::parse;

    private final String name;

    MimeType(String name)
    {
        if (name == null || name.isEmpty())
        {
            throw new IllegalArgumentException("name must not be empty");
        }
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public static MimeType parse(CharSequence s)
    {
        if (s == null)
        {
            return null;
        }
        String candidate = s.toString();
        for (MimeType mimeType : values())
        {
            if (mimeType.name().equalsIgnoreCase(candidate) || mimeType.getName().equalsIgnoreCase(candidate))
            {
                return mimeType;
            }
        }
        return null;
    }
}
