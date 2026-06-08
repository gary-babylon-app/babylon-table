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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.ToStringSettings;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

/**
 * Configured CSV table sink.
 * <p>
 * The destination stream is caller-owned. This sink flushes after writing, but
 * does not close it.
 */
public final class TableSinkCsv implements TableSink
{
    public static final char DEFAULT_SEPARATOR = WriteOptionsCsv.DEFAULT_SEPARATOR;
    public static final String DEFAULT_LINE_SEPARATOR = WriteOptionsCsv.DEFAULT_LINE_SEPARATOR;
    public static final Charset DEFAULT_CHARSET = WriteOptionsCsv.DEFAULT_CHARSET;

    private static final char DOUBLE_QUOTE = '"';
    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';

    private final String name;
    private final OutputStream outputStream;
    private final WriteOptionsCsv options;

    private TableSinkCsv(Builder builder)
    {
        this.name = ArgumentCheck.nonNull(builder.name, "name");
        this.outputStream = ArgumentCheck.nonNull(builder.outputStream, "outputStream");
        this.options = ArgumentCheck.nonNull(builder.options);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    public ToStringSettings getToStringSettings()
    {
        return this.options.toStringSettings();
    }

    public boolean isIncludeHeaders()
    {
        return this.options.includeHeaders();
    }

    public char getSeparator()
    {
        return this.options.separator();
    }

    public String getLineSeparator()
    {
        return this.options.lineSeparator();
    }

    public Charset getCharset()
    {
        return this.options.charset();
    }

    public WriteOptionsCsv getOptions()
    {
        return this.options;
    }

    @Override
    public void write(TableColumnar table) throws IOException
    {
        Writer target = writer(this.outputStream);
        writeCsv(ArgumentCheck.nonNull(table), target);
    }

    private Writer writer(OutputStream out)
    {
        return new BufferedWriter(new OutputStreamWriter(out, this.options.charset()));
    }

    private void writeCsv(TableColumnar table, Writer target) throws IOException
    {
        if (this.options.includeHeaders())
        {
            writeHeader(table, target);
        }
        for (int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex)
        {
            writeRow(table, target, rowIndex);
        }
        target.flush();
    }

    private void writeHeader(TableColumnar table, Writer target) throws IOException
    {
        ColumnName[] columnNames = table.getColumnNames();
        for (int i = 0; i < columnNames.length; ++i)
        {
            if (i > 0)
            {
                target.write(this.options.separator());
            }
            writeEscaped(columnNames[i].toString(), target);
        }
        target.write(this.options.lineSeparator());
    }

    private void writeRow(TableColumnar table, Writer target, int rowIndex) throws IOException
    {
        StringBuilder cell = new StringBuilder();
        int i = 0;
        for (Column column : table.columns())
        {
            if (i > 0)
            {
                target.write(this.options.separator());
            }
            cell.setLength(0);
            if (column.isSet(rowIndex))
            {
                column.appendTo(rowIndex, cell, this.options.toStringSettings());
                writeEscaped(cell, target, true);
            }
            ++i;
        }
        target.write(this.options.lineSeparator());
    }

    private void writeEscaped(CharSequence value, Writer target) throws IOException
    {
        writeEscaped(value, target, false);
    }

    private void writeEscaped(CharSequence value, Writer target, boolean quoteEmpty) throws IOException
    {
        CharSequence text = value == null ? "" : value;
        boolean mustQuote = (quoteEmpty && text.length() == 0)
                || Strings.indexOfAny(text, this.options.separator(), DOUBLE_QUOTE, LINE_FEED, CARRIAGE_RETURN) >= 0;
        if (!mustQuote)
        {
            target.append(text);
            return;
        }
        target.write(DOUBLE_QUOTE);
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            if (c == DOUBLE_QUOTE)
            {
                target.write(DOUBLE_QUOTE);
            }
            target.write(c);
        }
        target.write(DOUBLE_QUOTE);
    }

    public static final class Builder
    {
        private String name;
        private OutputStream outputStream;
        private WriteOptionsCsv options;

        private Builder()
        {
            this.options = WriteOptionsCsv.standard();
        }

        public Builder withName(String name)
        {
            this.name = ArgumentCheck.nonNull(name);
            return this;
        }

        public Builder withOutputStream(String name, OutputStream outputStream)
        {
            this.name = ArgumentCheck.nonNull(name);
            return withOutputStream(outputStream);
        }

        public Builder withOutputStream(String name, OutputStream outputStream, Charset charset)
        {
            this.name = ArgumentCheck.nonNull(name);
            return withOutputStream(outputStream, charset);
        }

        public Builder withOutputStream(String name, OutputStream outputStream, WriteOptionsCsv options)
        {
            this.name = ArgumentCheck.nonNull(name);
            return withOutputStream(outputStream, options);
        }

        public Builder withOutputStream(OutputStream outputStream)
        {
            this.outputStream = ArgumentCheck.nonNull(outputStream);
            return this;
        }

        public Builder withOutputStream(OutputStream outputStream, Charset charset)
        {
            this.options = toOptionsBuilder().withCharset(charset).build();
            return withOutputStream(outputStream);
        }

        public Builder withOutputStream(OutputStream outputStream, WriteOptionsCsv options)
        {
            this.options = ArgumentCheck.nonNull(options);
            return withOutputStream(outputStream);
        }

        public Builder withToStringSettings(ToStringSettings toStringSettings)
        {
            this.options = toOptionsBuilder().withToStringSettings(toStringSettings).build();
            return this;
        }

        public Builder withIncludeHeaders(boolean includeHeaders)
        {
            this.options = toOptionsBuilder().withIncludeHeaders(includeHeaders).build();
            return this;
        }

        public Builder withSeparator(char separator)
        {
            this.options = toOptionsBuilder().withSeparator(separator).build();
            return this;
        }

        public Builder withLineSeparator(String lineSeparator)
        {
            this.options = toOptionsBuilder().withLineSeparator(lineSeparator).build();
            return this;
        }

        public Builder withOptions(WriteOptionsCsv options)
        {
            this.options = ArgumentCheck.nonNull(options);
            return this;
        }

        public TableSinkCsv build()
        {
            return new TableSinkCsv(this);
        }

        private WriteOptionsCsv.Builder toOptionsBuilder()
        {
            return WriteOptionsCsv.builder().withCharset(this.options.charset())
                    .withToStringSettings(this.options.toStringSettings())
                    .withIncludeHeaders(this.options.includeHeaders()).withSeparator(this.options.separator())
                    .withLineSeparator(this.options.lineSeparator());
        }
    }
}
