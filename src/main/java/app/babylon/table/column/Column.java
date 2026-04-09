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

import app.babylon.table.ToStringSettings;
import app.babylon.table.ViewIndex;
/**
 * Base contract for a named column of tabular data, including size, null-state,
 * comparison, projection, and row-level access operations.
 */
public interface Column
{
    /**
     * Describes the runtime value type stored by a column.
     */
    public static interface Type
    {
        /**
         * Resolves a column type descriptor for the supplied Java class.
         *
         * @param valueClass
         *            the Java class represented by the column type
         * @return a reusable column type descriptor
         */
        public static Type of(Class<?> valueClass)
        {
            return ColumnTypes.of(valueClass);
        }

        /**
         * Returns the stable identifier used for this column type.
         *
         * @return the column type identifier
         */
        public String id();

        /**
         * Returns the Java class represented by this column type.
         *
         * @return the runtime value class
         */
        public Class<?> getValueClass();

        /**
         * Indicates whether the represented value class is a primitive Java type.
         *
         * @return {@code true} when the underlying value class is primitive
         */
        public default boolean isPrimitive()
        {
            return getValueClass().isPrimitive();
        }
    }

    /**
     * Returns the declared type of values stored in this column.
     *
     * @return the column value type
     */
    public Type getType();

    /**
     * Returns the number of rows in the column.
     *
     * @return the row count
     */
    public int size();

    /**
     * Returns the logical column name.
     *
     * @return the column name
     */
    public ColumnName getName();

    /**
     * Indicates whether the value at the supplied row is present.
     *
     * @param i
     *            the zero-based row index
     * @return {@code true} when the row contains a value
     */
    public boolean isSet(int i);

    /**
     * Indicates whether every row in the column contains a value.
     *
     * @return {@code true} when all rows are set
     */
    public boolean isAllSet();

    /**
     * Indicates whether no row in the column contains a value.
     *
     * @return {@code true} when every row is unset
     */
    public boolean isNoneSet();

    /**
     * Formats the value at the supplied row for display.
     *
     * @param i
     *            the zero-based row index
     * @return the formatted row value, or an empty string when unset
     */
    public String toString(int i);

    /**
     * Formats the value at the supplied row using the provided rendering settings.
     *
     * @param i
     *            the zero-based row index
     * @param settings
     *            formatting settings to apply
     * @return the formatted row value
     */
    default public String toString(int i, ToStringSettings settings)
    {
        return toString(i);
    }

    /**
     * Compares two row values from this column using the column's ordering
     * semantics.
     *
     * @param i
     *            the first row index
     * @param j
     *            the second row index
     * @return a negative number, zero, or a positive number as the first row is
     *         less than, equal to, or greater than the second
     */
    public int compare(int i, int j);

    /**
     * Returns a projected view of this column using the supplied row mapping.
     *
     * @param rowIndex
     *            the row mapping to apply
     * @return a column view over the selected rows
     */
    public Column view(ViewIndex rowIndex);

    /**
     * Creates a copy of this column with the supplied column name.
     *
     * @param x
     *            the name to assign to the copy
     * @return a column containing the same values under the new name
     */
    public Column copy(ColumnName x);

    /**
     * Returns a single-row column containing the value from the supplied row.
     *
     * @param i
     *            the row to extract
     * @return a single-row column with the same column name
     */
    public Column getAsColumn(int i);

}
