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

import app.babylon.lang.ArgumentCheck;

import app.babylon.io.DataSource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import app.babylon.table.TableException;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.lang.Is;

public class Csv
{
    static final int DEFAULT_HEADER_SCAN_LIMIT = 25;

    public static class Settings extends CsvSettingsBase
    {
        public static final Predicate<String> NON_EMPTY = s -> {
            if (s == null)
            {
                return false;
            }
            for (int i = 0; i < s.length(); ++i)
            {
                if (!Character.isWhitespace(s.charAt(i)))
                {
                    return true;
                }
            }
            return false;
        };

        public static final Predicate<String> ISIN = regex(Pattern.compile("(^[A-Z][A-Z][A-Z0-9]{10})"));

        char separator;
        Map<ColumnName, Predicate<String>> rowIncludeFilters;
        Map<ColumnName, Predicate<String>> rowExcludeFilters;
        int[] fixedWidths;
        Charset charset;
        boolean autoDetectEncoding;

        public Settings()
        {
            super();
            this.separator = ',';
            this.rowIncludeFilters = new HashMap<>();
            this.rowExcludeFilters = new HashMap<>();
            this.fixedWidths = null;
            this.charset = null;
            this.autoDetectEncoding = true;
        }

        public Settings(Settings base)
        {
            super(base);
            this.separator = base.separator;
            this.rowIncludeFilters = new HashMap<>(base.rowIncludeFilters);
            this.rowExcludeFilters = new HashMap<>(base.rowExcludeFilters);
            this.fixedWidths = base.fixedWidths == null
                    ? null
                    : Arrays.copyOf(base.fixedWidths, base.fixedWidths.length);
            this.charset = base.charset;
            this.autoDetectEncoding = base.autoDetectEncoding;
        }

        public Settings withCharset(Charset charset)
        {
            this.charset = charset;
            return this;
        }

        public Settings withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Settings withAutoDetectEncoding(boolean autoDetectEncoding)
        {
            this.autoDetectEncoding = autoDetectEncoding;
            return this;
        }

        public Settings withRowIncludeFilter(ColumnName x, Predicate<String> filter)
        {
            if (filter != null)
            {
                this.rowIncludeFilters.put(x, filter);
            }
            return this;
        }

        public Settings withRowExcludeFilter(ColumnName x, Predicate<String> filter)
        {
            if (filter != null)
            {
                this.rowExcludeFilters.put(x, filter);
            }
            return this;
        }

        public Settings withFixedWidths(int[] fixedWiths)
        {
            if (fixedWiths == null)
            {
                this.fixedWidths = null;
                return this;
            }
            this.fixedWidths = Arrays.copyOf(fixedWiths, fixedWiths.length);
            return this;
        }

        public Settings withIncludeResourceName(ColumnName x)
        {
            this.resourceName = x;
            return this;
        }

        public Settings withStripping(boolean stripping)
        {
            this.stripping = stripping;
            return this;
        }

        public Settings withTableName(TableName tableName)
        {
            this.tableName = tableName;
            return this;
        }

        public Settings withHeaderStrategy(HeaderStrategy headerStrategy)
        {
            ArgumentCheck.nonNull(headerStrategy);
            this.headerStrategy = headerStrategy;
            return this;
        }

        public Settings withLineReaderFactory(LineReaderFactory lineReaderFactory)
        {
            this.lineReaderFactory = lineReaderFactory;
            return this;
        }

        public Settings withSelectedHeader(ColumnName x)
        {
            this.requestedHeaders.add(x);
            return this;
        }

        public Settings withSelectedHeaders(ColumnName... x)
        {
            if (x != null)
            {
                this.requestedHeaders.addAll(Arrays.asList(x));
            }
            return this;
        }

        public Settings withColumnRename(ColumnName original, ColumnName newName)
        {
            ArgumentCheck.nonNull(original);
            ArgumentCheck.nonNull(newName);
            if (this.renameHeaders.containsKey(original))
            {
                throw new RuntimeException("Rename failed, column " + original + " already renamed");
            }
            this.renameHeaders.put(original, newName);
            return this;
        }

        public Settings withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.columnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
            return this;
        }

        public Settings withColumnType(ColumnName columnName, Class<?> valueClass)
        {
            return withColumnType(columnName, Column.Type.of(ArgumentCheck.nonNull(valueClass)));
        }

        public Charset getCharset()
        {
            return this.charset;
        }

        public boolean hasCharset()
        {
            return this.charset != null;
        }

        public boolean isAutoDetectEncoding()
        {
            return this.autoDetectEncoding;
        }

        public boolean isFixedWidths()
        {
            return !Is.empty(this.fixedWidths);
        }

        public int[] getFixedWidths()
        {
            if (this.fixedWidths == null)
            {
                return null;
            }
            return Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
        }

        public char getSeparator()
        {
            return this.separator;
        }

        public boolean hasRowIncludeFilters()
        {
            return this.rowIncludeFilters.size() > 0;
        }

        public boolean hasRowExcludeFilters()
        {
            return this.rowExcludeFilters.size() > 0;
        }

        public Predicate<String> getRowIncludeFilter(ColumnName x)
        {
            return this.rowIncludeFilters.get(x);
        }

        public Predicate<String> getRowExcludeFilter(ColumnName x)
        {
            return this.rowExcludeFilters.get(x);
        }

        public static Predicate<String> regex(Pattern pattern)
        {
            if (pattern == null)
            {
                return null;
            }
            return s -> s != null && pattern.matcher(s).find();
        }
    }

    public static TableColumnar read(DataSource ds, Settings options)
    {
        HeaderStrategy headerStrategy = options == null ? null : options.getHeaderStrategy();
        return read(ds, options, headerStrategy, RowConsumerTableCreator.factory());
    }

    public static TableColumnar read(DataSource ds, Settings options, HeaderStrategy headerStrategy)
    {
        return read(ds, options, headerStrategy, RowConsumerTableCreator.factory());
    }

    public static <T> T read(DataSource ds, Settings options, RowConsumerFactory<T> rowConsumerFactory)
    {
        HeaderStrategy headerStrategy = options == null ? null : options.getHeaderStrategy();
        return read(ds, options, headerStrategy, rowConsumerFactory);
    }

    public static <T> T read(DataSource ds, Settings options, HeaderStrategy headerStrategy,
            RowConsumerFactory<T> rowConsumerFactory)
    {
        if (options == null)
        {
            options = new Settings();
        }
        if (headerStrategy == null)
        {
            headerStrategy = new HeaderStrategyAuto();
        }
        if (rowConsumerFactory == null)
        {
            throw new IllegalArgumentException("rowConsumerFactory must not be null");
        }
        LineReaderFactory lineReaderFactory = options.getLineReaderFactory();
        if (lineReaderFactory == null)
        {
            lineReaderFactory = new LineReaderFactoryCSV();
        }
        try (LineReader lineReader = lineReaderFactory.create(ds, options))
        {
            RowStreamMarkable parsedRowStream = new RowStreamBuffered(lineReader);

            HeaderDetection headerDetection = headerStrategy.detect(parsedRowStream, options);

            RowConsumerResult<T> rowConsumer = rowConsumerFactory.create(options, headerDetection);
            RowProjected projectedRow = createRowProjected(options, headerDetection);

            parsedRowStream.reset();
            while (parsedRowStream.next())
            {
                rowConsumer.accept(projectedRow.with(parsedRowStream.current()));
            }
            return rowConsumer.buildResult(ds);
        } catch (TableException e)
        {
            throw e;
        } catch (IOException e)
        {
            throw new TableException("Failed to read table from data source '" + ds.getName() + "'.", e);
        }
    }

    private static RowProjected createRowProjected(Settings options, HeaderDetection headerDetection)
    {
        RowProjected projectedRow = options.isStripping()
                ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                : new ProjectedRow(headerDetection.getSelectedPositions());
        return projectedRow;
    }
}
