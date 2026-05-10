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
import java.nio.charset.StandardCharsets;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.ToStringSettings;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

/**
 * Configured CSV table sink.
 * <p>
 * The destination stream or writer is caller-owned. This sink flushes after
 * writing, but does not close it.
 */
public final class TableSinkCsv implements TableSink
{
    public static final char DEFAULT_SEPARATOR = ',';
    public static final String DEFAULT_LINE_SEPARATOR = "\r\n";
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final char DOUBLE_QUOTE = '"';
    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';

    private final String name;
    private final Writer writer;
    private final OutputStream outputStream;
    private final Charset charset;
    private final ToStringSettings toStringSettings;
    private final boolean includeHeaders;
    private final char separator;
    private final String lineSeparator;

    private TableSinkCsv(Builder builder)
    {
        this.name = ArgumentCheck.nonNull(builder.name, "name");
        this.writer = builder.writer;
        this.outputStream = builder.outputStream;
        this.charset = ArgumentCheck.nonNull(builder.charset);
        this.toStringSettings = ArgumentCheck.nonNull(builder.toStringSettings);
        this.includeHeaders = builder.includeHeaders;
        this.separator = builder.separator;
        this.lineSeparator = ArgumentCheck.nonNull(builder.lineSeparator);

        if ((this.writer == null) == (this.outputStream == null))
        {
            throw new IllegalArgumentException("Either writer or outputStream must be specified.");
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder toWriter(String name, Writer writer)
    {
        return builder().withWriter(name, writer);
    }

    public static Builder toOutputStream(String name, OutputStream outputStream)
    {
        return builder().withOutputStream(name, outputStream);
    }

    public static Builder toOutputStream(String name, OutputStream outputStream, Charset charset)
    {
        return builder().withOutputStream(name, outputStream, charset);
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    public ToStringSettings getToStringSettings()
    {
        return this.toStringSettings;
    }

    public boolean isIncludeHeaders()
    {
        return this.includeHeaders;
    }

    public char getSeparator()
    {
        return this.separator;
    }

    public String getLineSeparator()
    {
        return this.lineSeparator;
    }

    public Charset getCharset()
    {
        return this.charset;
    }

    @Override
    public void write(TableColumnar table) throws IOException
    {
        Writer target = this.writer == null
                ? new BufferedWriter(new OutputStreamWriter(this.outputStream, this.charset))
                : this.writer;
        writeCsv(ArgumentCheck.nonNull(table), target);
    }

    private void writeCsv(TableColumnar table, Writer target) throws IOException
    {
        if (this.includeHeaders)
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
                target.write(this.separator);
            }
            writeEscaped(columnNames[i].toString(), target);
        }
        target.write(this.lineSeparator);
    }

    private void writeRow(TableColumnar table, Writer target, int rowIndex) throws IOException
    {
        StringBuilder cell = new StringBuilder();
        int i = 0;
        for (Column column : table.columns())
        {
            if (i > 0)
            {
                target.write(this.separator);
            }
            cell.setLength(0);
            if (column.isSet(rowIndex))
            {
                column.appendTo(rowIndex, cell, this.toStringSettings);
                writeEscaped(cell, target, true);
            }
            ++i;
        }
        target.write(this.lineSeparator);
    }

    private void writeEscaped(CharSequence value, Writer target) throws IOException
    {
        writeEscaped(value, target, false);
    }

    private void writeEscaped(CharSequence value, Writer target, boolean quoteEmpty) throws IOException
    {
        CharSequence text = value == null ? "" : value;
        boolean mustQuote = (quoteEmpty && text.length() == 0)
                || Strings.indexOfAny(text, this.separator, DOUBLE_QUOTE, LINE_FEED, CARRIAGE_RETURN) >= 0;
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
        private Writer writer;
        private OutputStream outputStream;
        private Charset charset;
        private ToStringSettings toStringSettings;
        private boolean includeHeaders;
        private char separator;
        private String lineSeparator;

        private Builder()
        {
            this.charset = DEFAULT_CHARSET;
            this.toStringSettings = ToStringSettings.standard();
            this.includeHeaders = true;
            this.separator = DEFAULT_SEPARATOR;
            this.lineSeparator = DEFAULT_LINE_SEPARATOR;
        }

        public Builder withName(String name)
        {
            this.name = ArgumentCheck.nonNull(name);
            return this;
        }

        public Builder withWriter(String name, Writer writer)
        {
            this.name = ArgumentCheck.nonNull(name);
            return withWriter(writer);
        }

        public Builder withWriter(Writer writer)
        {
            this.writer = ArgumentCheck.nonNull(writer);
            this.outputStream = null;
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

        public Builder withOutputStream(OutputStream outputStream)
        {
            this.outputStream = ArgumentCheck.nonNull(outputStream);
            this.writer = null;
            return this;
        }

        public Builder withOutputStream(OutputStream outputStream, Charset charset)
        {
            this.charset = ArgumentCheck.nonNull(charset);
            return withOutputStream(outputStream);
        }

        public Builder withToStringSettings(ToStringSettings toStringSettings)
        {
            this.toStringSettings = ArgumentCheck.nonNull(toStringSettings);
            return this;
        }

        public Builder withIncludeHeaders(boolean includeHeaders)
        {
            this.includeHeaders = includeHeaders;
            return this;
        }

        public Builder withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Builder withLineSeparator(String lineSeparator)
        {
            this.lineSeparator = ArgumentCheck.nonNull(lineSeparator);
            return this;
        }

        public TableSinkCsv build()
        {
            return new TableSinkCsv(this);
        }
    }
}
