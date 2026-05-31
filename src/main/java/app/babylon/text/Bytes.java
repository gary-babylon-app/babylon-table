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

public final class Bytes
{
    private Bytes()
    {
    }

    public static boolean isStripxEmpty(ByteSequence bytes)
    {
        return bytes == null || isStripxEmpty(bytes, 0, bytes.length());
    }

    public static boolean isStripxEmpty(ByteSequence bytes, int start, int end)
    {
        if (bytes == null || start >= end)
        {
            return true;
        }
        return stripxStart(bytes, start, end) >= stripxEnd(bytes, start, end);
    }

    public static int stripxStart(ByteSequence bytes, int start, int end)
    {
        if (bytes == null || start >= end)
        {
            return start;
        }
        int strippedStart = start;
        while (strippedStart < end)
        {
            int consumed = strippableLengthAtStart(bytes, strippedStart, end);
            if (consumed == 0)
            {
                return strippedStart;
            }
            strippedStart += consumed;
        }
        return strippedStart;
    }

    public static int stripxEnd(ByteSequence bytes, int start, int end)
    {
        if (bytes == null || start >= end)
        {
            return start;
        }
        int strippedEnd = end;
        while (strippedEnd > start)
        {
            int consumed = strippableLengthAtEnd(bytes, start, strippedEnd);
            if (consumed == 0)
            {
                return strippedEnd;
            }
            strippedEnd -= consumed;
        }
        return strippedEnd;
    }

    private static int strippableLengthAtStart(ByteSequence bytes, int start, int end)
    {
        int value = unsigned(bytes.byteAt(start));
        if (isSingleByteStrippable(value))
        {
            return 1;
        }
        if (startsWith(bytes, start, end, 0xC2, 0xA0))
        {
            return 2;
        }
        if (startsWith(bytes, start, end, 0xEF, 0xBB, 0xBF) || startsWith(bytes, start, end, 0xEF, 0xBF, 0xBD))
        {
            return 3;
        }
        if (startsWith(bytes, start, end, 0xE2, 0x80, 0x8B) || startsWith(bytes, start, end, 0xE2, 0x80, 0x8C)
                || startsWith(bytes, start, end, 0xE2, 0x80, 0x8D) || startsWith(bytes, start, end, 0xE2, 0x80, 0xAF))
        {
            return 3;
        }
        return 0;
    }

    private static int strippableLengthAtEnd(ByteSequence bytes, int start, int end)
    {
        if (endsWith(bytes, start, end, 0xC2, 0xA0))
        {
            return 2;
        }
        if (endsWith(bytes, start, end, 0xEF, 0xBB, 0xBF) || endsWith(bytes, start, end, 0xEF, 0xBF, 0xBD))
        {
            return 3;
        }
        if (endsWith(bytes, start, end, 0xE2, 0x80, 0x8B) || endsWith(bytes, start, end, 0xE2, 0x80, 0x8C)
                || endsWith(bytes, start, end, 0xE2, 0x80, 0x8D) || endsWith(bytes, start, end, 0xE2, 0x80, 0xAF))
        {
            return 3;
        }
        int value = unsigned(bytes.byteAt(end - 1));
        if (isSingleByteStrippable(value))
        {
            return 1;
        }
        return 0;
    }

    private static boolean isSingleByteStrippable(int value)
    {
        return value == ' ' || value == '\t' || value == '\n' || value == '\r' || value == '\f' || value == 0x0B
                || value == 0xA0;
    }

    private static boolean startsWith(ByteSequence bytes, int start, int end, int byte1, int byte2)
    {
        return end - start >= 2 && unsigned(bytes.byteAt(start)) == byte1 && unsigned(bytes.byteAt(start + 1)) == byte2;
    }

    private static boolean startsWith(ByteSequence bytes, int start, int end, int byte1, int byte2, int byte3)
    {
        return end - start >= 3 && unsigned(bytes.byteAt(start)) == byte1 && unsigned(bytes.byteAt(start + 1)) == byte2
                && unsigned(bytes.byteAt(start + 2)) == byte3;
    }

    private static boolean endsWith(ByteSequence bytes, int start, int end, int byte1, int byte2)
    {
        return end - start >= 2 && unsigned(bytes.byteAt(end - 2)) == byte1 && unsigned(bytes.byteAt(end - 1)) == byte2;
    }

    private static boolean endsWith(ByteSequence bytes, int start, int end, int byte1, int byte2, int byte3)
    {
        return end - start >= 3 && unsigned(bytes.byteAt(end - 3)) == byte1 && unsigned(bytes.byteAt(end - 2)) == byte2
                && unsigned(bytes.byteAt(end - 1)) == byte3;
    }

    private static int unsigned(byte value)
    {
        return Byte.toUnsignedInt(value);
    }
}
