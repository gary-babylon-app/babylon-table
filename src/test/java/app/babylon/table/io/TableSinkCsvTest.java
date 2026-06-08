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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TableSinkCsvTest
{
    private static final ColumnName CATEGORY = ColumnName.of("Category");
    private static final ColumnName AMOUNT = ColumnName.of("Amount");

    @Test
    void shouldWriteCsvWithoutHeaders() throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TableSinkCsv sink = TableSinkCsv.builder().withOutputStream("cashflows.csv", outputStream)
                .withIncludeHeaders(false).build();

        sink.write(sampleTable());

        assertEquals("""
                Pay,1000000\r
                Receive,1250000.5\r
                """, outputStream.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldWriteCsvToOutputStreamUsingCharset() throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TableSinkCsv sink = TableSinkCsv.builder()
                .withOutputStream("cashflows.csv", outputStream, StandardCharsets.UTF_8).build();

        sink.write(sampleTable());

        assertEquals("""
                Category,Amount\r
                Pay,1000000\r
                Receive,1250000.5\r
                """, outputStream.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldWriteCsvToOutputStreamUsingUtf8ByDefault() throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TableSinkCsv sink = TableSinkCsv.builder().withOutputStream("cashflows.csv", outputStream).build();

        sink.write(sampleTable());

        assertEquals("""
                Category,Amount\r
                Pay,1000000\r
                Receive,1250000.5\r
                """, outputStream.toString(StandardCharsets.UTF_8));
    }

    @Test
    void directOutputStreamIsNotClosed() throws IOException
    {
        CloseTrackingOutputStream outputStream = new CloseTrackingOutputStream();
        TableSinkCsv sink = TableSinkCsv.builder().withOutputStream("cashflows.csv", outputStream).build();

        sink.write(sampleTable());

        assertFalse(outputStream.closed);
    }

    @Test
    void shouldDistinguishUnsetFromSetEmptyString() throws IOException
    {
        final ColumnName NOTE = ColumnName.of("Note");
        ColumnObject.Builder<String> notes = ColumnObject.builder(NOTE);
        notes.addNull();
        notes.add("");

        TableColumnar table = Tables.newTable(TableName.of("Notes"), notes.build());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TableSinkCsv sink = TableSinkCsv.builder().withOutputStream("notes.csv", outputStream).build();

        sink.write(table);

        assertEquals("Note\r\n\r\n\"\"\r\n", outputStream.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldEscapeCsvSpecialValues() throws IOException
    {
        final ColumnName VALUE = ColumnName.of("Value,Name");
        ColumnObject.Builder<String> values = ColumnObject.builder(VALUE);
        values.add("alpha,beta");
        values.add("say \"hello\"");
        values.add("line1\nline2");

        TableColumnar table = Tables.newTable(TableName.of("Values"), values.build());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TableSinkCsv sink = TableSinkCsv.builder().withOutputStream("values.csv", outputStream).build();

        sink.write(table);

        assertEquals("ValueName\r\n\"alpha,beta\"\r\n\"say \"\"hello\"\"\"\r\n\"line1\nline2\"\r\n",
                outputStream.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldWriteCsvUsingOptions() throws IOException
    {
        WriteOptionsCsv options = WriteOptionsCsv.builder().withIncludeHeaders(false).withSeparator(';')
                .withLineSeparator("\n").build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TableSinkCsv sink = TableSinkCsv.builder().withOutputStream("cashflows.csv", outputStream, options).build();

        sink.write(sampleTable());

        assertEquals(options, sink.getOptions());
        assertEquals("""
                Pay;1000000
                Receive;1250000.5
                """, outputStream.toString(StandardCharsets.UTF_8));
    }

    private static TableColumnar sampleTable()
    {
        ColumnObject.Builder<String> categories = ColumnObject.builder(CATEGORY);
        categories.add("Pay");
        categories.add("Receive");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(AMOUNT);
        amounts.add(new BigDecimal("1000000"));
        amounts.add(new BigDecimal("1250000.50"));

        return Tables.newTable(TableName.of("Cashflows"), new TableDescription("Cashflow rows"), categories.build(),
                amounts.build());
    }

    private static final class CloseTrackingOutputStream extends ByteArrayOutputStream
    {
        private boolean closed;

        @Override
        public void close()
        {
            this.closed = true;
        }
    }
}
