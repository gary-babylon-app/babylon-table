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

import app.babylon.table.column.ColumnDefinition;

/**
 * Supplies tabular rows from a live underlying source.
 * <p>
 * A row supplier represents an open row cursor. Some implementations own the
 * underlying resource and release it on {@link #close()}, while others adapt
 * caller-owned resources and treat {@code close()} as a no-op.
 */
public interface RowCursor extends AutoCloseable
{
    ColumnDefinition[] columns();

    boolean next();

    Row current();

    @Override
    void close() throws Exception;
}
