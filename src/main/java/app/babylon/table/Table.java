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

    /**
     * Returns a one-row table containing the last row.
     *
     * @return a one-row table containing the last row
     */
    public Table getLastRow();

    /**
     * Returns a one-row table containing the first row.
     *
     * @return a one-row table containing the first row
     */
    public Table getFirstRow();

    /**
     * Returns a one-row table containing the row at the supplied index.
     *
     * @param i
     *            the zero-based row index
     * @return a one-row table containing the row at {@code i}
     */
    public Table getRow(int i);
}
