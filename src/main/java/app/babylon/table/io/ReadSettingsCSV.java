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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.Empties;
import app.babylon.table.TableName;

public class ReadSettingsCSV extends ReadSettingsCommon
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

    public ReadSettingsCSV()
    {
        super();
        this.separator = ',';
        this.rowIncludeFilters = new HashMap<>();
        this.rowExcludeFilters = new HashMap<>();
        this.fixedWidths = null;
        this.charset = null;
        this.autoDetectEncoding = true;
    }

    protected ReadSettingsCSV(ReadSettingsCSV base)
    {
        super(base);
        this.separator = base.separator;
        this.rowIncludeFilters = new HashMap<>(base.rowIncludeFilters);
        this.rowExcludeFilters = new HashMap<>(base.rowExcludeFilters);
        this.fixedWidths = base.fixedWidths == null ? null : Arrays.copyOf(base.fixedWidths, base.fixedWidths.length);
        this.charset = base.charset;
        this.autoDetectEncoding = base.autoDetectEncoding;
    }

    public ReadSettingsCSV withCharset(Charset charset)
    {
        this.charset = charset;
        return this;
    }

    public ReadSettingsCSV withSeparator(char separator)
    {
        this.separator = separator;
        return this;
    }

    public Charset getCharset()
    {
        return this.charset;
    }

    public boolean hasCharset()
    {
        return this.charset != null;
    }

    public ReadSettingsCSV withAutoDetectEncoding(boolean autoDetectEncoding)
    {
        this.autoDetectEncoding = autoDetectEncoding;
        return this;
    }

    public boolean isAutoDetectEncoding()
    {
        return this.autoDetectEncoding;
    }

    public boolean isFixedWidths()
    {
        return !Empties.isEmpty(this.fixedWidths);
    }

    public int[] getFixedWidths()
    {
        if (this.fixedWidths == null)
        {
            return null;
        }
        return Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
    }

    public ReadSettingsCSV withRowIncludeFilter(ColumnName x, Predicate<String> filter)
    {
        if (filter != null)
        {
            this.rowIncludeFilters.put(x, filter);
        }
        return this;
    }

    public ReadSettingsCSV withRowExcludeFilter(ColumnName x, Predicate<String> filter)
    {
        if (filter != null)
        {
            this.rowExcludeFilters.put(x, filter);
        }
        return this;
    }

    public ReadSettingsCSV withFixedWidths(int[] fixedWiths)
    {
        if (fixedWiths == null)
        {
            this.fixedWidths = null;
            return this;
        }
        this.fixedWidths = Arrays.copyOf(fixedWiths, fixedWiths.length);
        return this;
    }

    public ReadSettingsCSV withIncludeResourceName(ColumnName x)
    {
        super.withIncludeResourceName(x);
        return this;
    }

    public ReadSettingsCSV withStripping(boolean stripping)
    {
        super.withStripping(stripping);
        return this;
    }

    public ReadSettingsCSV withTableName(TableName tableName)
    {
        super.withTableName(tableName);
        return this;
    }

    public ReadSettingsCSV withHeaderStrategy(HeaderStrategy headerStrategy)
    {
        super.withHeaderStrategy(headerStrategy);
        return this;
    }

    public ReadSettingsCSV withLineReaderFactory(LineReaderFactory lineReaderFactory)
    {
        super.withLineReaderFactory(lineReaderFactory);
        return this;
    }

    public ReadSettingsCSV withSelectedHeader(ColumnName x)
    {
        super.withSelectedHeader(x);
        return this;
    }

    public ReadSettingsCSV withSelectedHeaders(ColumnName... x)
    {
        super.withSelectedHeaders(x);
        return this;
    }

    public ReadSettingsCSV withColumnRename(ColumnName original, ColumnName newName)
    {
        super.withColumnRename(original, newName);
        return this;
    }

    public ReadSettingsCSV withColumnType(ColumnName columnName, Column.Type columnType)
    {
        super.withColumnType(columnName, columnType);
        return this;
    }

    public ReadSettingsCSV withColumnType(ColumnName columnName, Class<?> valueClass)
    {
        super.withColumnType(columnName, valueClass);
        return this;
    }

    public char getSeparator()
    {
        return separator;
    }

    public Column.Type getColumnType(ColumnName columnName)
    {
        return super.getColumnType(columnName);
    }

    public boolean isStripping()
    {
        return super.isStripping();
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
