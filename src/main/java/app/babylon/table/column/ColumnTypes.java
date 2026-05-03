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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.Currency;

import app.babylon.table.column.Column.Type;
import app.babylon.table.column.type.TypeParsers;
import app.babylon.table.column.type.TypeWriters;

/**
 * Standard built-in column types.
 */
public final class ColumnTypes
{
    // @formatter:off
    /** Primitive boolean type. */
    public static final Type BOOLEAN       = Column.Type.of(boolean.class,    TypeParsers.BOOLEAN,
            TypeWriters.BOOLEAN);
    /** Boxed boolean type. */
    public static final Type BOOLEAN_OBJECT = Column.Type.of(Boolean.class,   TypeParsers.BOOLEAN,
            TypeWriters.BOOLEAN);
    /** Primitive byte type. */
    public static final Type BYTE          = Column.Type.of(byte.class,       TypeParsers.BYTE, TypeWriters.BYTE);
    /** Boxed byte type. */
    public static final Type BYTE_OBJECT   = Column.Type.of(Byte.class,       TypeParsers.BYTE, TypeWriters.BYTE);
    /** Primitive int type. */
    public static final Type INT           = Column.Type.of(int.class,        TypeParsers.INT, TypeWriters.INT);
    /** Boxed int type. */
    public static final Type INT_OBJECT    = Column.Type.of(Integer.class,    TypeParsers.INT, TypeWriters.INT);
    /** Primitive long type. */
    public static final Type LONG          = Column.Type.of(long.class,       TypeParsers.LONG, TypeWriters.LONG);
    /** Boxed long type. */
    public static final Type LONG_OBJECT   = Column.Type.of(Long.class,       TypeParsers.LONG, TypeWriters.LONG);
    /** Primitive double type. */
    public static final Type DOUBLE        = Column.Type.of(double.class,     TypeParsers.DOUBLE, TypeWriters.DOUBLE);
    /** Boxed double type. */
    public static final Type DOUBLE_OBJECT = Column.Type.of(Double.class,     TypeParsers.DOUBLE, TypeWriters.DOUBLE);
    /** String type. */
    public static final Type STRING        = Column.Type.of(String.class,     TypeParsers.STRING, TypeWriters.STRING);
    /** BigDecimal type. */
    public static final Type DECIMAL       = Column.Type.of(BigDecimal.class, TypeParsers.BIG_DECIMAL,
            TypeWriters.BIG_DECIMAL);
    /** Instant type. */
    public static final Type INSTANT       = Column.Type.of(Instant.class,    TypeParsers.INSTANT, TypeWriters.INSTANT);
    /** LocalDateTime type. */
    public static final Type LOCAL_DATE_TIME = Column.Type.of(LocalDateTime.class, TypeParsers.LOCAL_DATE_TIME,
            TypeWriters.LOCAL_DATE_TIME);
    /** LocalTime type. */
    public static final Type LOCAL_TIME    = Column.Type.of(LocalTime.class,  TypeParsers.LOCAL_TIME,
            TypeWriters.LOCAL_TIME);
    /** OffsetDateTime type. */
    public static final Type OFFSET_DATE_TIME = Column.Type.of(OffsetDateTime.class, TypeParsers.OFFSET_DATE_TIME,
            TypeWriters.OFFSET_DATE_TIME);
    /** Period type. */
    public static final Type PERIOD        = Column.Type.of(Period.class,     TypeParsers.PERIOD, TypeWriters.PERIOD);
    /** YearMonth type. */
    public static final Type YEAR_MONTH    = Column.Type.of(YearMonth.class,  TypeParsers.YEAR_MONTH,
            TypeWriters.YEAR_MONTH);
    /** LocalDate type. */
    public static final Type LOCALDATE     = Column.Type.of(LocalDate.class,  TypeParsers.LOCAL_DATE_YMD,
            TypeWriters.LOCAL_DATE);
    /** Currency type. */
    public static final Type CURRENCY      = Column.Type.of(Currency.class,   TypeParsers.CURRENCY,
            TypeWriters.CURRENCY);
    // @formatter:on

    private ColumnTypes()
    {
    }
}
