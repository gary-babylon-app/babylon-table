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
    public static interface Type
    {
        public static Type of(Class<?> valueClass)
        {
            return ColumnTypes.of(valueClass);
        }

        public String id();

        public Class<?> getValueClass();

        public default boolean isPrimitive()
        {
            return getValueClass().isPrimitive();
        }
    }

    public Type getType();

    public int size();

    public ColumnName getName();

    public boolean isSet(int i);

    public boolean isAllSet();

    public boolean isNoneSet();

    public String toString(int i);

    default public String toString(int i, ToStringSettings settings)
    {
        return toString(i);
    }

    public int compare(int i, int j);

    public Column view(ViewIndex rowIndex);

    public Column copy(ColumnName x);

    /*
     * A a column with same column name with value of the ith row
     */
    public Column getAsColumn(int i);

}
