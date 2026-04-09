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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import app.babylon.table.TableName;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

abstract class CsvSettingsBase
{
    Map<ColumnName, ColumnName> renameHeaders;
    Map<ColumnName, Column.Type> columnTypes;
    Set<ColumnName> requestedHeaders;
    LineReaderFactory lineReaderFactory;
    TableName tableName;
    ColumnName resourceName;
    HeaderStrategy headerStrategy;
    boolean stripping;

    CsvSettingsBase()
    {
        this.renameHeaders = new HashMap<>();
        this.columnTypes = new HashMap<>();
        this.requestedHeaders = new LinkedHashSet<>();
        this.lineReaderFactory = null;
        this.tableName = null;
        this.resourceName = null;
        this.headerStrategy = new HeaderStrategyAuto(Csv.DEFAULT_HEADER_SCAN_LIMIT);
        this.stripping = true;
    }

    CsvSettingsBase(CsvSettingsBase base)
    {
        ArgumentCheck.nonNull(base);
        this.renameHeaders = new HashMap<>(base.renameHeaders);
        this.columnTypes = new HashMap<>(base.columnTypes);
        this.requestedHeaders = new LinkedHashSet<>(base.requestedHeaders);
        this.lineReaderFactory = base.lineReaderFactory;
        this.tableName = base.tableName;
        this.resourceName = base.resourceName;
        this.headerStrategy = base.headerStrategy;
        this.stripping = base.stripping;
    }

    public HeaderStrategy getHeaderStrategy()
    {
        return this.headerStrategy;
    }

    public LineReaderFactory getLineReaderFactory()
    {
        return this.lineReaderFactory;
    }

    public boolean includeResourceName()
    {
        return this.resourceName != null;
    }

    public ColumnName getResourceName()
    {
        return this.resourceName;
    }

    public boolean isStripping()
    {
        return this.stripping;
    }

    public TableName getTableName()
    {
        return this.tableName;
    }

    public ColumnName getRenameColumnName(String original)
    {
        if (Strings.isEmpty(original))
        {
            return null;
        }
        return getRenameColumnName(ColumnName.of(original));
    }

    public ColumnName getRenameColumnName(ColumnName original)
    {
        ColumnName r = this.renameHeaders.get(original);
        if (r == null)
        {
            return original;
        }
        return r;
    }

    public Column.Type getColumnType(ColumnName columnName)
    {
        return this.columnTypes.get(columnName);
    }

    public Collection<ColumnName> getRequestedHeaders(Collection<ColumnName> x)
    {
        if (x == null)
        {
            x = new ArrayList<>();
        }
        x.addAll(this.requestedHeaders);
        return x;
    }
}
