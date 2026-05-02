/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.transform;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import java.util.Map;

/**
 * Applies a named structural transformation to a table's columns map.
 */
public interface Transform
{
    public String getName();

    default public void apply(SourceMetadata metadata, Map<ColumnName, Column> columnsByName)
    {
        apply(columnsByName);
    }

    public void apply(Map<ColumnName, Column> columnsByName);
}
