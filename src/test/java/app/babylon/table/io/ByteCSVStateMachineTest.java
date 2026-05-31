/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ByteCSVStateMachineTest
{
    @Test
    void shouldReadPlainCsvRows() throws IOException
    {
        try (LineReader reader = reader("Date,Description,Amount\n2026-01-01,Coffee,3.50\n", 8192))
        {
            assertTrue(reader.next());
            assertInstanceOf(ByteStringSlices.class, reader.current());
            assertArrayEquals(new String[]
            {"Date", "Description", "Amount"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"2026-01-01", "Coffee", "3.50"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadStandardCsvRowSourceExample() throws IOException
    {
        try (LineReader reader = reader("Date,Description,Amount\n2026-01-01,Coffee,3.50\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Date", "Description", "Amount"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"2026-01-01", "Coffee", "3.50"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadCommaAndDoubleQuoteExample() throws IOException
    {
        try (LineReader reader = reader("City,Note\nParis,\"Price,12\"\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Paris", "Price,12"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadSemicolonAndSingleQuoteExample() throws IOException
    {
        try (LineReader reader = reader("City;Note\nParis;'Price;12'\n", ';', '\'', 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Paris", "Price;12"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadWindows1252BytesExample() throws IOException
    {
        Charset charset = Charset.forName("windows-1252");
        byte[] bytes = "City,Note\nParis,Price €12\n".getBytes(charset);

        try (LineReader reader = reader(bytes, ',', '"', 8192, charset))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Paris", "Price €12"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldRejectNonAsciiCompatibleCharset()
    {
        byte[] bytes = "A,B\n".getBytes(StandardCharsets.UTF_16LE);

        assertThrows(IllegalArgumentException.class, () -> reader(bytes, ',', '"', 8192, StandardCharsets.UTF_16LE));
    }

    @Test
    void shouldReadQuotedFieldsAndEscapedQuotes() throws IOException
    {
        try (LineReader reader = reader("City,Note\nParis,\"Price,12 and \"\"quoted\"\"\"\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Paris", "Price,12 and \"quoted\""}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadCrLfRows() throws IOException
    {
        try (LineReader reader = reader("A,B\r\n1,2\r\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", "B"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"1", "2"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadAcrossBufferBoundaries() throws IOException
    {
        try (LineReader reader = reader("Alpha,Beta\n1234567890,quoted\n", 4))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Alpha", "Beta"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"1234567890", "quoted"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadEmptyAndTrailingFields() throws IOException
    {
        try (LineReader reader = reader("A,,C\nA,B,\n,,\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", null, "C"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", "B", null}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {null, null, null}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadLastRowWithoutFinalNewline() throws IOException
    {
        try (LineReader reader = reader("A,B\n1,2", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", "B"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"1", "2"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadQuotedLineFeedInsideField() throws IOException
    {
        try (LineReader reader = reader("Note,Value\n\"A\nB\",C\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Note", "Value"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A\nB", "C"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadQuotedCrLfInsideField() throws IOException
    {
        try (LineReader reader = reader("Note,Value\n\"A\r\nB\",C\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Note", "Value"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A\r\nB", "C"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadEmptyQuotedField() throws IOException
    {
        try (LineReader reader = reader("\"\",B\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {null, "B"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadEscapedQuoteAcrossBufferBoundary() throws IOException
    {
        try (LineReader reader = reader("\"A\"\"B\",C\n", 3))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A\"B", "C"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldTreatQuoteAfterUnquotedContentAsLiteral() throws IOException
    {
        try (LineReader reader = reader("A\"B,C\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A\"B", "C"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldAllowCharactersAfterClosingQuote() throws IOException
    {
        try (LineReader reader = reader("\"A\"x,B\n", 8192))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"Ax", "B"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldPassHighBitBytesThroughAsContent() throws IOException
    {
        byte[] bytes = new byte[]
        {'A', ',', (byte) 0x80, (byte) 0xA0, ',', (byte) 0xE2, (byte) 0x80, (byte) 0xAF, '\n'};

        try (LineReader reader = reader(bytes, ',', '"', 8192))
        {
            assertTrue(reader.next());
            ByteStringSlices row = reader.current();

            assertEquals(3, row.size());
            assertEquals((byte) 0x80, row.byteAt(row.start(1)));
            assertEquals((byte) 0xA0, row.byteAt(row.start(1) + 1));
            assertEquals((byte) 0xE2, row.byteAt(row.start(2)));
            assertEquals((byte) 0x80, row.byteAt(row.start(2) + 1));
            assertEquals((byte) 0xAF, row.byteAt(row.start(2) + 2));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldReadSeparatorAndRowTerminatorsAcrossBufferBoundaries() throws IOException
    {
        try (LineReader reader = reader("A,B\r\nC,D\n", 1))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", "B"}, values(reader.current()));

            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"C", "D"}, values(reader.current()));

            assertFalse(reader.next());
        }
    }

    @Test
    void shouldRejectUnterminatedQuotedField()
    {
        try (LineReader reader = reader("A,B\n1,\"unterminated", 8192))
        {
            assertTrue(reader.next());
            assertThrows(IOException.class, reader::next);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static LineReader reader(String csv, int bufferSize)
    {
        return reader(csv, ',', '"', bufferSize);
    }

    private static LineReader reader(String csv, char separator, char quote, int bufferSize)
    {
        byte[] bytes = csv.getBytes(StandardCharsets.ISO_8859_1);
        return reader(bytes, separator, quote, bufferSize);
    }

    private static LineReader reader(byte[] bytes, char separator, char quote, int bufferSize)
    {
        return reader(bytes, separator, quote, bufferSize, StandardCharsets.ISO_8859_1);
    }

    private static LineReader reader(byte[] bytes, char separator, char quote, int bufferSize, Charset charset)
    {
        return new ByteCSVStateMachine(new ByteReaderCSV(new ByteArrayInputStream(bytes), bufferSize), separator, quote,
                charset);
    }

    private static String[] values(ByteStringSlices row)
    {
        String[] values = new String[row.size()];
        for (int i = 0; i < row.size(); ++i)
        {
            values[i] = row.decode(i);
        }
        return values;
    }
}
