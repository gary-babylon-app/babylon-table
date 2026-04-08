/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

/**
 * Represents a tabular data set with a name, description, and row/column
 * dimensions.
 */
public interface Table
{
    public TableName getName();

    public TableDescription getDescription();

    public int getColumnCount();

    public int getRowCount();

    public Table getLastRow();

    public Table getFirstRow();

    public Table getRow(int i);
}
