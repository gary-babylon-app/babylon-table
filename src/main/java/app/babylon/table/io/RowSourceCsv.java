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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import app.babylon.io.DataSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

/**
 * Configured CSV row source that opens a {@link RowSupplierCsv} from a
 * {@link DataSource}.
 */
public final class RowSourceCsv implements RowSource
{
    private final DataSource dataSource;
    private final HeaderStrategy headerStrategy;
    private final char separator;
    private final int[] fixedWidths;
    private final Charset charset;
    private final boolean autoDetectEncoding;
    private final Map<ColumnName, Column.Type> explicitColumnTypes;

    private RowSourceCsv(Builder builder)
    {
        this.dataSource = ArgumentCheck.nonNull(builder.dataSource);
        this.headerStrategy = ArgumentCheck.nonNull(builder.headerStrategy);
        this.separator = builder.separator;
        this.fixedWidths = builder.fixedWidths == null
                ? null
                : Arrays.copyOf(builder.fixedWidths, builder.fixedWidths.length);
        this.charset = ArgumentCheck.nonNull(builder.charset);
        this.autoDetectEncoding = builder.autoDetectEncoding;
        this.explicitColumnTypes = new LinkedHashMap<>(builder.explicitColumnTypes);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    @Override
    public String getName()
    {
        return this.dataSource.getName();
    }

    @Override
    public RowSupplier openRows()
    {
        return RowSupplierCsv.builder().withHeaderStrategy(this.headerStrategy).withSeparator(this.separator)
                .withFixedWidths(this.fixedWidths).withCharset(this.charset)
                .withAutoDetectEncoding(this.autoDetectEncoding).withColumnTypes(this.explicitColumnTypes)
                .build(this.dataSource.openStream());
    }

    public static final class Builder
    {
        private final Map<ColumnName, Column.Type> explicitColumnTypes;
        private DataSource dataSource;
        private HeaderStrategy headerStrategy;
        private char separator;
        private int[] fixedWidths;
        private Charset charset;
        private boolean autoDetectEncoding;

        private Builder()
        {
            this.explicitColumnTypes = new LinkedHashMap<>();
            this.dataSource = null;
            this.headerStrategy = new HeaderStrategyAuto(HeaderStrategy.DEFAULT_SCAN_LIMIT);
            this.separator = ',';
            this.fixedWidths = null;
            this.charset = StandardCharsets.UTF_8;
            this.autoDetectEncoding = true;
        }

        public Builder withDataSource(DataSource dataSource)
        {
            this.dataSource = ArgumentCheck.nonNull(dataSource);
            return this;
        }

        public Builder withHeaderStrategy(HeaderStrategy headerStrategy)
        {
            this.headerStrategy = ArgumentCheck.nonNull(headerStrategy);
            return this;
        }

        public Builder withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Builder withFixedWidths(int[] fixedWidths)
        {
            this.fixedWidths = fixedWidths == null ? null : Arrays.copyOf(fixedWidths, fixedWidths.length);
            return this;
        }

        public Builder withCharset(Charset charset)
        {
            this.charset = ArgumentCheck.nonNull(charset);
            return this;
        }

        public Builder withAutoDetectEncoding(boolean autoDetectEncoding)
        {
            this.autoDetectEncoding = autoDetectEncoding;
            return this;
        }

        public Builder withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.explicitColumnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
            return this;
        }

        public Builder withColumnTypes(Map<ColumnName, Column.Type> columnTypes)
        {
            if (columnTypes != null)
            {
                for (Map.Entry<ColumnName, Column.Type> entry : columnTypes.entrySet())
                {
                    withColumnType(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public RowSourceCsv build()
        {
            return new RowSourceCsv(this);
        }
    }
}
