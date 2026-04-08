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

import java.util.Collection;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.TableName;

/**
 * Describes configurable options that control how tabular input is interpreted
 * during reading.
 */
public interface ReadSettings
{
    ReadSettings withHeaderStrategy(HeaderStrategy headerStrategy);

    HeaderStrategy getHeaderStrategy();

    ReadSettings withLineReaderFactory(LineReaderFactory lineReaderFactory);

    LineReaderFactory getLineReaderFactory();

    ColumnName getRenameColumnName(String original);

    ColumnName getRenameColumnName(ColumnName original);

    Collection<ColumnName> getRequestedHeaders(Collection<ColumnName> x);

    Column.Type getColumnType(ColumnName columnName);

    boolean includeResourceName();

    ColumnName getResourceName();

    ReadSettings withStripping(boolean stripping);

    boolean isStripping();

    ReadSettings withTableName(TableName tableName);

    TableName getTableName();
}
