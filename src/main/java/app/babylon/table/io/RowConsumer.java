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

import app.babylon.table.column.ColumnName;

/**
 * Consumes parsed rows during a read operation after being initialised with the
 * projected column names that incoming rows will expose.
 */
public interface RowConsumer<T>
{
    void start(ColumnName[] columnNames);

    void accept(Row row);

    T build();
}
