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

import java.util.Map;

import app.babylon.io.StreamSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

/**
 * Configured CSV row source that opens a {@link RowCursorCsv} from a
 * {@link StreamSource}.
 */
public final class RowSourceCsv implements RowSource
{
    private final StreamSource streamSource;
    private final RowCursorCsv.Builder rowCursorBuilder;

    private RowSourceCsv(Builder builder)
    {
        this.streamSource = ArgumentCheck.nonNull(builder.streamSource);
        this.rowCursorBuilder = builder.rowCursorBuilder.copy();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    @Override
    public String getName()
    {
        return this.streamSource.getName();
    }

    @Override
    public RowCursor openRows()
    {
        return this.rowCursorBuilder.build(this.streamSource.openStream());
    }

    public static final class Builder
    {
        private StreamSource streamSource;
        private final RowCursorCsv.Builder rowCursorBuilder;

        private Builder()
        {
            this.streamSource = null;
            this.rowCursorBuilder = RowCursorCsv.builder();
        }

        public Builder withStreamSource(StreamSource streamSource)
        {
            this.streamSource = ArgumentCheck.nonNull(streamSource);
            return this;
        }

        public Builder withHeaderStrategy(HeaderStrategy headerStrategy)
        {
            this.rowCursorBuilder.withHeaderStrategy(headerStrategy);
            return this;
        }

        public Builder withSeparator(char separator)
        {
            this.rowCursorBuilder.withSeparator(separator);
            return this;
        }

        public Builder withQuote(char quote)
        {
            this.rowCursorBuilder.withQuote(quote);
            return this;
        }

        public Builder withFixedWidths(int[] fixedWidths)
        {
            this.rowCursorBuilder.withFixedWidths(fixedWidths);
            return this;
        }

        public Builder withCharset(java.nio.charset.Charset charset)
        {
            this.rowCursorBuilder.withCharset(charset);
            return this;
        }

        public Builder withAutoDetectEncoding(boolean autoDetectEncoding)
        {
            this.rowCursorBuilder.withAutoDetectEncoding(autoDetectEncoding);
            return this;
        }

        public Builder withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.rowCursorBuilder.withColumnType(columnName, columnType);
            return this;
        }

        public Builder withColumnTypes(Map<ColumnName, Column.Type> columnTypes)
        {
            this.rowCursorBuilder.withColumnTypes(columnTypes);
            return this;
        }

        public RowSourceCsv build()
        {
            return new RowSourceCsv(this);
        }
    }
}
