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
 * A column of byte values used for compact storage of byte-oriented data and
 * file-signature style checks.
 */
public interface ColumnByte extends Column
{
    /**
     * Column type descriptor for primitive byte columns.
     */
    public static final Type TYPE = PrimitiveColumnType.BYTE;

    /**
     * Builder for nullable byte columns.
     */
    public static interface Builder extends ColumnBuilder
    {
        /**
         * Appends a byte value.
         *
         * @param x the value to append
         * @return this builder
         */
        public Builder add(byte x);

        /**
         * Appends an unset row.
         *
         * @return this builder
         */
        public Builder addNull();

        @Override
        public ColumnByte build();
    }

    /**
     * Creates a byte column builder for the supplied column name.
     *
     * @param name the column name
     * @return a new byte column builder
     */
    public static Builder builder(ColumnName name)
    {
        return new ColumnByteBuilderArray(name);
    }

    /**
     * Returns the byte value at the supplied row.
     *
     * @param i the zero-based row index
     * @return the byte value
     */
    public byte get(int i);

    /**
     * Indicates whether the byte values match the legacy XLS file signature.
     *
     * @return {@code true} when the column contents look like XLS bytes
     */
    public boolean isXls();

    /**
     * Indicates whether the byte values match the XLSX file signature.
     *
     * @return {@code true} when the column contents look like XLSX bytes
     */
    public boolean isXlsx();
}
