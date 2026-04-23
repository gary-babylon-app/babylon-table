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

import app.babylon.lang.ArgumentCheck;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class StreamSourceProbe
{
    private static final int BYTES_TO_SNIP = 8192;
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private final byte[] bytes;
    private final String resourceName;

    private StreamSourceProbe(byte[] bytes, String resourceName)
    {
        this.bytes = ArgumentCheck.nonNull(bytes, "bytes must not be null");
        this.resourceName = ArgumentCheck.nonNull(resourceName, "resourceName must not be null");
        if (resourceName.isEmpty())
        {
            throw new IllegalArgumentException("resourceName must not be empty");
        }
    }

    public static StreamSourceProbe of(byte[] bytes, String resourceName)
    {
        return new StreamSourceProbe(bytes, resourceName);
    }

    public static StreamSourceProbe of(BufferedInputStream bstream, String resourceName) throws IOException
    {
        ArgumentCheck.nonNull(bstream, "bstream must not be null");
        bstream.mark(BYTES_TO_SNIP);
        byte[] bytes = bstream.readNBytes(BYTES_TO_SNIP);
        bstream.reset();
        return new StreamSourceProbe(bytes, resourceName);
    }

    private static InputStream toMarkableStream(InputStream instream)
    {
        if (!instream.markSupported())
        {
            return new BufferedInputStream(instream);
        }
        return instream;
    }

    public static StreamSourceProbe of(StreamSource ds) throws IOException
    {
        try (InputStream instream = ds.openStream())
        {
            InputStream markableStream = toMarkableStream(instream);
            markableStream.mark(BYTES_TO_SNIP);
            byte[] bytes = markableStream.readNBytes(BYTES_TO_SNIP);
            markableStream.reset();
            return new StreamSourceProbe(bytes, ds.getName());
        }
    }

    public boolean isXls()
    {
        return bytes.length > 7 && bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF && bytes[2] == (byte) 0x11
                && bytes[3] == (byte) 0xE0 && bytes[4] == (byte) 0xA1 && bytes[5] == (byte) 0xB1
                && bytes[6] == (byte) 0x1A && bytes[7] == (byte) 0xE1;
    }

    public boolean isZip()
    {
        return bytes.length > 3 && bytes[0] == (byte) 0x50 && bytes[1] == (byte) 0x4B && bytes[2] == (byte) 0x03
                && bytes[3] == (byte) 0x04;
    }

    public boolean isXlsx()
    {
        return isZip() && resourceName.endsWith(".xlsx");
    }

    public boolean hasBom()
    {
        return bomLengthBytes() > 0;
    }

    public int bomLengthBytes()
    {
        if (hasUtf8Bom())
        {
            return 3;
        }
        if (hasUtf16LeBom() || hasUtf16BeBom())
        {
            return 2;
        }
        return 0;
    }

    public Charset detectedCharset()
    {
        if (hasUtf8Bom())
        {
            return StandardCharsets.UTF_8;
        }
        if (hasUtf16LeBom())
        {
            return StandardCharsets.UTF_16LE;
        }
        if (hasUtf16BeBom())
        {
            return StandardCharsets.UTF_16BE;
        }
        if (looksLikeUtf16Le())
        {
            return StandardCharsets.UTF_16LE;
        }
        if (looksLikeUtf16Be())
        {
            return StandardCharsets.UTF_16BE;
        }
        if (containsWindows1252OnlyBytes())
        {
            return WINDOWS_1252;
        }
        return null;
    }

    public Charset getCharset(Charset valueIfNull)
    {
        Charset detected = detectedCharset();
        if (detected != null)
        {
            return detected;
        }
        if (isValidUtf8())
        {
            return StandardCharsets.UTF_8;
        }
        return ArgumentCheck.nonNull(valueIfNull, "valueIfNull must not be null");
    }

    public boolean hasUtf8Bom()
    {
        return bytes.length > 2 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF;
    }

    public boolean hasUtf16LeBom()
    {
        return bytes.length > 1 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE;
    }

    public boolean hasUtf16BeBom()
    {
        return bytes.length > 1 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF;
    }

    private boolean looksLikeUtf16Le()
    {
        return looksLikeUtf16(false);
    }

    private boolean looksLikeUtf16Be()
    {
        return looksLikeUtf16(true);
    }

    private boolean looksLikeUtf16(boolean zeroOnEvenPositions)
    {
        int start = bomLengthBytes();
        int length = this.bytes.length - start;
        if (length < 8)
        {
            return false;
        }

        int evenCount = 0;
        int oddCount = 0;
        int evenZeros = 0;
        int oddZeros = 0;

        for (int i = start; i < this.bytes.length; ++i)
        {
            if (((i - start) & 1) == 0)
            {
                ++evenCount;
                if (this.bytes[i] == 0)
                {
                    ++evenZeros;
                }
            }
            else
            {
                ++oddCount;
                if (this.bytes[i] == 0)
                {
                    ++oddZeros;
                }
            }
        }

        if (evenCount == 0 || oddCount == 0)
        {
            return false;
        }

        double evenZeroRatio = (double) evenZeros / evenCount;
        double oddZeroRatio = (double) oddZeros / oddCount;
        if (zeroOnEvenPositions)
        {
            return evenZeroRatio >= 0.30d && oddZeroRatio <= 0.05d;
        }
        return oddZeroRatio >= 0.30d && evenZeroRatio <= 0.05d;
    }

    private boolean isValidUtf8()
    {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try
        {
            decoder.decode(ByteBuffer.wrap(this.bytes, bomLengthBytes(), this.bytes.length - bomLengthBytes()));
            return true;
        }
        catch (CharacterCodingException e)
        {
            return false;
        }
    }

    private boolean containsWindows1252OnlyBytes()
    {
        for (int i = bomLengthBytes(); i < this.bytes.length; ++i)
        {
            int value = this.bytes[i] & 0xFF;
            if (value >= 0x80 && value <= 0x9F)
            {
                return true;
            }
        }
        return false;
    }

    public boolean isPdf()
    {
        return bytes.length > 3 && bytes[0] == (byte) 0x25 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x44
                && bytes[3] == (byte) 0x46;
    }
}
