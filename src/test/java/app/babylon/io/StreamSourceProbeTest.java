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
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class StreamSourceProbeTest
{
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
    }
}
