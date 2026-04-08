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

import app.babylon.text.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.text.Strings;
import app.babylon.table.TableName;

public class ReadSettingsCommon implements ReadSettings
{
    static final int DEFAULT_HEADER_SCAN_LIMIT = 25;

    Map<ColumnName, ColumnName> renameHeaders;
    Map<ColumnName, Column.Type> columnTypes;
    Set<ColumnName> requestedHeaders;
    LineReaderFactory lineReaderFactory;
    TableName tableName;
    ColumnName resourceName;
    HeaderStrategy headerStrategy;
    boolean stripping;

    public ReadSettingsCommon()
    {
        this.renameHeaders = new HashMap<>();
        this.columnTypes = new HashMap<>();
        this.requestedHeaders = new LinkedHashSet<>();
        this.lineReaderFactory = null;
        this.tableName = null;
        this.resourceName = null;
        this.headerStrategy = new HeaderStrategyAuto(DEFAULT_HEADER_SCAN_LIMIT);
        this.stripping = true;
    }

    protected ReadSettingsCommon(ReadSettingsCommon base)
    {
        Objects.requireNonNull(base);
        this.renameHeaders = new HashMap<>(base.renameHeaders);
        this.columnTypes = new HashMap<>(base.columnTypes);
        this.requestedHeaders = new LinkedHashSet<>(base.requestedHeaders);
        this.lineReaderFactory = base.lineReaderFactory;
        this.tableName = base.tableName;
        this.resourceName = base.resourceName;
        this.headerStrategy = base.headerStrategy;
        this.stripping = base.stripping;
    }

    @Override
    public ReadSettingsCommon withHeaderStrategy(HeaderStrategy headerStrategy)
    {
        Objects.requireNonNull(headerStrategy);
        this.headerStrategy = headerStrategy;
        return this;
    }

    @Override
    public HeaderStrategy getHeaderStrategy()
    {
        return this.headerStrategy;
    }

    @Override
    public ReadSettingsCommon withLineReaderFactory(LineReaderFactory lineReaderFactory)
    {
        this.lineReaderFactory = lineReaderFactory;
        return this;
    }

    @Override
    public LineReaderFactory getLineReaderFactory()
    {
        return this.lineReaderFactory;
    }

    public ReadSettingsCommon withIncludeResourceName(ColumnName x)
    {
        this.resourceName = x;
        return this;
    }

    @Override
    public ReadSettingsCommon withTableName(TableName tableName)
    {
        this.tableName = tableName;
        return this;
    }

    public ReadSettingsCommon withSelectedHeader(ColumnName x)
    {
        this.requestedHeaders.add(x);
        return this;
    }

    public ReadSettingsCommon withSelectedHeaders(ColumnName... x)
    {
        if (x != null)
        {
            Collections.addAll(this.requestedHeaders, x);
        }
        return this;
    }

    public ReadSettingsCommon withColumnRename(ColumnName original, ColumnName newName)
    {
        Objects.requireNonNull(original);
        Objects.requireNonNull(newName);
        if (renameHeaders.containsKey(original))
        {
            throw new RuntimeException("Rename failed, column " + original + " already renamed");
        }
        this.renameHeaders.put(original, newName);
        return this;
    }

    public ReadSettingsCommon withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(Objects.requireNonNull(columnName), Objects.requireNonNull(columnType));
        return this;
    }

    public ReadSettingsCommon withColumnType(ColumnName columnName, Class<?> valueClass)
    {
        return withColumnType(columnName, Column.Type.of(Objects.requireNonNull(valueClass)));
    }

    @Override
    public boolean includeResourceName()
    {
        return this.resourceName != null;
    }

    @Override
    public ColumnName getResourceName()
    {
        return this.resourceName;
    }

    @Override
    public ReadSettingsCommon withStripping(boolean stripping)
    {
        this.stripping = stripping;
        return this;
    }

    @Override
    public boolean isStripping()
    {
        return this.stripping;
    }

    @Override
    public TableName getTableName()
    {
        return this.tableName;
    }

    @Override
    public ColumnName getRenameColumnName(String original)
    {
        if (Strings.isEmpty(original))
        {
            return null;
        }
        return getRenameColumnName(ColumnName.of(original));
    }

    @Override
    public ColumnName getRenameColumnName(ColumnName original)
    {
        ColumnName r = this.renameHeaders.get(original);
        if (r == null)
        {
            return original;
        }
        return r;
    }

    @Override
    public Column.Type getColumnType(ColumnName columnName)
    {
        return this.columnTypes.get(columnName);
    }

    @Override
    public Collection<ColumnName> getRequestedHeaders(Collection<ColumnName> x)
    {
        if (x == null)
        {
            x = new ArrayList<>();
        }
        x.addAll(this.requestedHeaders);
        return x;
    }

    private int currentScanLimit()
    {
        return this.headerStrategy == null ? DEFAULT_HEADER_SCAN_LIMIT : this.headerStrategy.getScanLimit();
    }
}
