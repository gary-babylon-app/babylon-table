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

import app.babylon.text.Strings;

public enum MimeType
{
    // @formatter:off
    APPLICATION_PDF("application/pdf"),
    EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    MESSAGE_RFC822("message/rfc822"),
    TEXT_CSV("text/csv"),
    TEXT_PLAIN("text/plain");
    // @formatter:on

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
        return parse(s, 0, s.length());
    }

    public static MimeType parse(CharSequence s, int start, int end)
    {
        if (s == null)
        {
            return null;
        }
        int candidateStart = Strings.stripStart(s, start, end);
        int candidateEnd = Strings.stripEnd(s, candidateStart, end);
        if (candidateStart >= candidateEnd)
        {
            return null;
        }
        for (MimeType mimeType : values())
        {
            if (Strings.equalsIgnoreCase(s, candidateStart, candidateEnd, mimeType.name())
                    || Strings.equalsIgnoreCase(s, candidateStart, candidateEnd, mimeType.getName()))
            {
                return mimeType;
            }
        }
        return null;
    }
}
