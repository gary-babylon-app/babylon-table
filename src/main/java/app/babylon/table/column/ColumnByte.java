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
    public static final Type TYPE = PrimitiveColumnType.BYTE;

    public static interface Builder extends ColumnBuilder
    {
        public Builder add(byte x);

        public Builder addNull();

        @Override
        public ColumnByte build();
    }

    public static Builder builder(ColumnName name)
    {
        return new ColumnByteBuilderArray(name);
    }

    public byte get(int i);

    public boolean isXls();

    public boolean isXlsx();
}
