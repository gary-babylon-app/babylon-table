/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import app.babylon.lang.ArgumentCheck;

/**
 * Describes a logical input column with a required name and an optional type.
 *
 * @param name
 *            column name
 * @param type
 *            optional column type
 */
public record ColumnDefinition(ColumnName name, Column.Type type)
{
    /**
     * Validates the canonical record state.
     */
    public ColumnDefinition
    {
        ArgumentCheck.nonNull(name, "name must not be null");
    }

    /**
     * Creates an untyped column definition.
     *
     * @param name
     *            column name
     */
    public ColumnDefinition(ColumnName name)
    {
        this(name, null);
    }
}
