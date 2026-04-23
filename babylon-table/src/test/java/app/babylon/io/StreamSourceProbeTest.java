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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class StreamSourceProbeTest
{
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    @Test
    public void testExcelFile() throws IOException
    {
        StreamSource ds = StreamSources.fromClass(StreamSourceProbe.class, "ExcelTestCase.xlsx");

        StreamSourceProbe snippet = StreamSourceProbe.of(ds);
        assertTrue(snippet.isXlsx());
        assertFalse(snippet.isXls());
    }

    @Test
    public void testBomDetectionUtf8()
    {
        byte[] bytes = new byte[]
        {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', ',', 'b'};
        StreamSourceProbe snippet = StreamSourceProbe.of(bytes, "x.csv");
        assertTrue(snippet.hasBom());
        assertTrue(snippet.hasUtf8Bom());
        assertFalse(snippet.hasUtf16LeBom());
        assertFalse(snippet.hasUtf16BeBom());
        assertEquals(3, snippet.bomLengthBytes());
        assertEquals(StandardCharsets.UTF_8, snippet.detectedCharset());
    }

    @Test
    public void testBomDetectionUtf16Le()
    {
        byte[] bytes = new byte[]
        {(byte) 0xFF, (byte) 0xFE, 'a', 0x00};
        StreamSourceProbe snippet = StreamSourceProbe.of(bytes, "x.csv");
        assertTrue(snippet.hasBom());
        assertFalse(snippet.hasUtf8Bom());
        assertTrue(snippet.hasUtf16LeBom());
        assertFalse(snippet.hasUtf16BeBom());
        assertEquals(2, snippet.bomLengthBytes());
        assertEquals(StandardCharsets.UTF_16LE, snippet.detectedCharset());
    }

    @Test
    public void testNoBomDetection()
    {
        byte[] bytes = new byte[]
        {'a', ',', 'b'};
        StreamSourceProbe snippet = StreamSourceProbe.of(bytes, "x.csv");
        assertFalse(snippet.hasBom());
        assertFalse(snippet.hasUtf8Bom());
        assertFalse(snippet.hasUtf16LeBom());
        assertFalse(snippet.hasUtf16BeBom());
        assertEquals(0, snippet.bomLengthBytes());
        assertNull(snippet.detectedCharset());
        assertEquals(StandardCharsets.UTF_8, snippet.getCharset(StandardCharsets.UTF_8));
        assertEquals(StandardCharsets.UTF_8, snippet.getCharset(StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testUtf16LeDetectionWithoutBom()
    {
        byte[] bytes = "a,b\nc,d\n".getBytes(StandardCharsets.UTF_16LE);
        StreamSourceProbe snippet = StreamSourceProbe.of(bytes, "x.csv");

        assertFalse(snippet.hasBom());
        assertEquals(StandardCharsets.UTF_16LE, snippet.detectedCharset());
    }

    @Test
    public void testUtf16BeDetectionWithoutBom()
    {
        byte[] bytes = "a,b\nc,d\n".getBytes(StandardCharsets.UTF_16BE);
        StreamSourceProbe snippet = StreamSourceProbe.of(bytes, "x.csv");

        assertFalse(snippet.hasBom());
        assertEquals(StandardCharsets.UTF_16BE, snippet.detectedCharset());
    }

    @Test
    public void testInvalidUtf8FallsBackToProvidedCharset()
    {
        byte[] bytes = "Price €12\n".getBytes(WINDOWS_1252);
        StreamSourceProbe snippet = StreamSourceProbe.of(bytes, "x.csv");

        assertFalse(snippet.hasBom());
        assertEquals(WINDOWS_1252, snippet.detectedCharset());
        assertEquals(WINDOWS_1252, snippet.getCharset(StandardCharsets.ISO_8859_1));
    }
}
