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

        public Builder withStripping(boolean stripping)
        {
            this.rowCursorBuilder.withStripping(stripping);
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

        /**
         * Specifies a source-side column type for CSV reading.
         * <p>
         * The supplied type is exposed through the resulting
         * {@link RowCursorCsv#columns()} metadata and can therefore select the
         * low-level builder used during row consumption. This is the preferred place to
         * specify a type when the goal is to parse row slices directly into the final
         * builder rather than first building a {@code String} column and converting it
         * later.
         * <p>
         * For ordinary categorical text this is usually not needed. In that common case
         * it is often better to keep the source as {@code STRING}, let the row consumer
         * build the string dictionary naturally, and only specify a source-side type
         * when the direct parser has a real advantage.
         *
         * @param columnName
         *            the source column name
         * @param columnType
         *            the source-side column type
         * @return this builder
         */
        public Builder withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.rowCursorBuilder.withColumnType(columnName, columnType);
            return this;
        }

        /**
         * Specifies a source-side column type for CSV reading.
         * <p>
         * This is stronger than a purely post-read typing hint. The type supplied here
         * becomes part of the {@link RowCursorCsv#columns()} metadata and can therefore
         * influence which builder the row consumer creates before rows are read.
         * <p>
         * For categorical text columns, this is best reserved for cases where the
         * direct parser is meaningfully better than first creating the string
         * dictionary.
         * <p>
         * Use this when the source text can be parsed directly into the desired builder
         * and you want to avoid an intermediate string column in memory, for example:
         * <p>
         * - primitive numeric columns such as {@code int}, {@code long}, and
         * {@code double}
         * <p>
         * - custom enum-like object types with fast {@code CharSequence}-based parsers
         *
         * @param columnTypes
         *            source-side column types keyed by column name
         * @return this builder
         */
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
