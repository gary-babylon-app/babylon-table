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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import app.babylon.table.column.Column.Type;
import app.babylon.table.column.type.TypeParsers;

public final class ColumnTypes
{
    // @formatter:off
    public static final Type BYTE          = Column.Type.of(byte.class,       TypeParsers.BYTE);
    public static final Type BYTE_OBJECT   = Column.Type.of(Byte.class,       TypeParsers.BYTE);
    public static final Type INT           = Column.Type.of(int.class,        TypeParsers.INT);
    public static final Type INT_OBJECT    = Column.Type.of(Integer.class,    TypeParsers.INT);
    public static final Type LONG          = Column.Type.of(long.class,       TypeParsers.LONG);
    public static final Type LONG_OBJECT   = Column.Type.of(Long.class,       TypeParsers.LONG);
    public static final Type DOUBLE        = Column.Type.of(double.class,     TypeParsers.DOUBLE);
    public static final Type DOUBLE_OBJECT = Column.Type.of(Double.class,     TypeParsers.DOUBLE);
    public static final Type STRING        = Column.Type.of(String.class,     TypeParsers.STRING);
    public static final Type DECIMAL       = Column.Type.of(BigDecimal.class, TypeParsers.BIG_DECIMAL);
    public static final Type LOCALDATE     = Column.Type.of(LocalDate.class,  TypeParsers.LOCAL_DATE_YMD);
    public static final Type CURRENCY      = Column.Type.of(Currency.class,   TypeParsers.CURRENCY);
    // @formatter:on

    private ColumnTypes()
    {
    }
}
