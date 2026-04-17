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

/**
 * Builds an immutable column instance for a specific column name.
 */
public interface ColumnBuilder
{
    /**
     * Returns the name that will be assigned to the built column.
     *
     * @return the target column name
     */
    public ColumnName getName();

    /**
     * Appends a value directly from a character slice.
     *
     * @param chars
     *            the source text
     * @param start
     *            the start offset
     * @param length
     *            the slice length
     * @return this builder
     */
    default ColumnBuilder add(CharSequence chars, int start, int length)
    {
        throw new UnsupportedOperationException("Character-slice add not supported by " + getClass().getName());
    }

    /**
     * Materialises the current builder contents as an immutable column.
     *
     * @return the built column
     */
    public Column build();
}
