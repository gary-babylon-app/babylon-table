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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class LineReaderCSVTest
{
    @Test
    public void readLineShouldHandleQuotedCrLfInsideField() throws Exception
    {
        String csv = "A,B\r\n1,\"hello\r\nworld\"\r\n2,x\r\n";

        LineReaderFactory factory = new LineReaderFactoryCSV();
        try (LineReader reader = factory.create(TestDataSources.fromString(csv, "Test"), new ReadSettingsCSV()))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", "B"}, ((RowBuffer) reader.current()).toStringArray());
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"1", "hello\r\nworld"}, ((RowBuffer) reader.current()).toStringArray());
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"2", "x"}, ((RowBuffer) reader.current()).toStringArray());
            assertFalse(reader.next());
        }
    }

    @Test
    public void readLineShouldKeepAllFieldsInSourceOrder() throws Exception
    {
        String csv = "A,B,C\n1,2,3\n4,5,6\n";

        LineReaderFactory factory = new LineReaderFactoryCSV();
        try (LineReader reader = factory.create(TestDataSources.fromString(csv, "Test"), new ReadSettingsCSV()))
        {
            assertTrue(reader.next());
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"1", "2", "3"}, ((RowBuffer) reader.current()).toStringArray());
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"4", "5", "6"}, ((RowBuffer) reader.current()).toStringArray());
            assertFalse(reader.next());
        }
    }

    @Test
    public void readLineShouldStripUtf8BomAndParse() throws Exception
    {
        byte[] utf8BomCsv = new byte[]
        {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'A', ',', 'B', '\n', '1', ',', '2', '\n'};
        DataSource dataSource = TestDataSources.fromBytes(utf8BomCsv, "Test");

        LineReaderFactory factory = new LineReaderFactoryCSV();
        try (LineReader reader = factory.create(dataSource, new ReadSettingsCSV()))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", "B"}, ((RowBuffer) reader.current()).toStringArray());
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"1", "2"}, ((RowBuffer) reader.current()).toStringArray());
            assertFalse(reader.next());
        }
    }

    @Test
    public void readLineShouldParseUtf16LeBomWhenCharsetProvided() throws Exception
    {
        String csv = "A,B\r\n1,2\r\n";
        byte[] body = csv.getBytes(StandardCharsets.UTF_16LE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xFE);
        out.write(body);

        DataSource dataSource = TestDataSources.fromBytes(out.toByteArray(), "Test");

        LineReaderFactory factory = new LineReaderFactoryCSV();
        try (LineReader reader = factory.create(dataSource, new ReadSettingsCSV()))
        {
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"A", "B"}, ((RowBuffer) reader.current()).toStringArray());
            assertTrue(reader.next());
            assertArrayEquals(new String[]
            {"1", "2"}, ((RowBuffer) reader.current()).toStringArray());
            assertFalse(reader.next());
        }
    }

    @Test
    public void readFixedWidthShouldUseBulkSlicesAndConfiguredCharset() throws Exception
    {
        String content = "ABCDWXYZ\n12345678\n";
        byte[] body = content.getBytes(StandardCharsets.UTF_16LE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xFE);
        out.write(body);

        DataSource dataSource = TestDataSources.fromBytes(out.toByteArray(), "fixed-width.txt");
        ReadSettingsCSV settings = new ReadSettingsCSV().withFixedWidths(new int[]
        {4, 4});

        LineReaderFactory factory = new LineReaderFactoryCSV();
        try (LineReader reader = factory.create(dataSource, settings))
        {
            assertTrue(reader.next());
            assertRowEquals(new String[]
            {"ABCD", "WXYZ"}, reader.current());
            assertTrue(reader.next());
            assertRowEquals(new String[]
            {"1234", "5678"}, reader.current());
            assertFalse(reader.next());
        }
    }

    private static void assertRowEquals(String[] expected, Row row)
    {
        String[] actual = new String[row.fieldCount()];
        char[] chars = row.chars();
        for (int i = 0; i < row.fieldCount(); ++i)
        {
            actual[i] = new String(chars, row.start(i), row.length(i));
        }
        assertArrayEquals(expected, actual);
    }
}
