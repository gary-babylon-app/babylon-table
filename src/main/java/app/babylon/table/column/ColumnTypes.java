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
    public static final Type BYTE          = Column.Type.register(byte.class,       TypeParsers.STRING);
    public static final Type BYTE_OBJECT   = Column.Type.register(Byte.class,       TypeParsers.STRING);
    public static final Type INT           = Column.Type.register(int.class,        TypeParsers.STRING);
    public static final Type INT_OBJECT    = Column.Type.register(Integer.class,    TypeParsers.STRING);
    public static final Type LONG          = Column.Type.register(long.class,       TypeParsers.STRING);
    public static final Type LONG_OBJECT   = Column.Type.register(Long.class,       TypeParsers.STRING);
    public static final Type DOUBLE        = Column.Type.register(double.class,     TypeParsers.STRING);
    public static final Type DOUBLE_OBJECT = Column.Type.register(Double.class,     TypeParsers.STRING);
    public static final Type STRING        = Column.Type.register(String.class,     TypeParsers.STRING);
    public static final Type DECIMAL       = Column.Type.register(BigDecimal.class, TypeParsers.BIG_DECIMAL);
    public static final Type LOCALDATE     = Column.Type.register(LocalDate.class,  TypeParsers.LOCAL_DATE_YMD);
    public static final Type CURRENCY      = Column.Type.register(Currency.class,   TypeParsers.CURRENCY);
    // @formatter:on

    private ColumnTypes()
    {
    }
}
