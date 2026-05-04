/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.text;

public final class Booleans
{
    public static final byte FALSE = 0;
    public static final byte TRUE = 1;
    public static final byte UNPARSED = -1;

    private Booleans()
    {
    }

    public static boolean isBooleanTrue(CharSequence s)
    {
        return s != null && isBooleanTrue(s, 0, s.length());
    }

    public static boolean isBooleanTrue(CharSequence s, int offset, int length)
    {
        return parseBooleanValue(s, offset, length) == TRUE;
    }

    public static boolean isBooleanFalse(CharSequence s)
    {
        return s != null && isBooleanFalse(s, 0, s.length());
    }

    public static boolean isBooleanFalse(CharSequence s, int offset, int length)
    {
        return parseBooleanValue(s, offset, length) == FALSE;
    }

    public static byte parseBooleanValue(CharSequence s, int offset, int length)
    {
        if (s == null || length <= 0)
        {
            return UNPARSED;
        }
        if (length == 1)
        {
            return switch (s.charAt(offset))
            {
                case 'T', 't', '1' -> TRUE;
                case 'F', 'f', '0' -> FALSE;
                default -> UNPARSED;
            };
        }
        if (Strings.equalsIgnoreCase(s, offset, length, "true"))
        {
            return TRUE;
        }
        if (Strings.equalsIgnoreCase(s, offset, length, "false"))
        {
            return FALSE;
        }
        return UNPARSED;
    }
}
