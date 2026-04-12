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

import java.util.Optional;

import app.babylon.lang.ArgumentCheck;

/**
 * Describes a logical input column with a required name and an optional type.
 */
public record ColumnDefinition(ColumnName name, Optional<Column.Type> type)
{
    public ColumnDefinition
    {
        ArgumentCheck.nonNull(name, "name must not be null");
        type = type == null ? Optional.empty() : type;
    }

    public ColumnDefinition(ColumnName name)
    {
        this(name, Optional.empty());
    }

    public ColumnDefinition(ColumnName name, Column.Type type)
    {
        this(name, Optional.ofNullable(type));
    }
}
