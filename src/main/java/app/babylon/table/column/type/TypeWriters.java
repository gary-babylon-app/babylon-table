/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column.type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.Currency;

public final class TypeWriters
{
    public static final TypeWriter<Byte> BYTE = appendUsingToString();
    public static final TypeWriter<Integer> INT = appendUsingToString();
    public static final TypeWriter<Long> LONG = appendUsingToString();
    public static final TypeWriter<Double> DOUBLE = appendUsingToString();
    public static final TypeWriter<String> STRING = appendUsingToString();
    public static final TypeWriter<BigDecimal> BIG_DECIMAL = (value, out) -> {
        if (value != null)
        {
            out.append(value.toPlainString());
        }
    };
    public static final TypeWriter<Instant> INSTANT = appendUsingToString();
    public static final TypeWriter<LocalDateTime> LOCAL_DATE_TIME = appendUsingToString();
    public static final TypeWriter<LocalTime> LOCAL_TIME = appendUsingToString();
    public static final TypeWriter<OffsetDateTime> OFFSET_DATE_TIME = appendUsingToString();
    public static final TypeWriter<Period> PERIOD = appendUsingToString();
    public static final TypeWriter<YearMonth> YEAR_MONTH = appendUsingToString();
    public static final TypeWriter<LocalDate> LOCAL_DATE = appendUsingToString();
    public static final TypeWriter<Currency> CURRENCY = (value, out) -> {
        if (value != null)
        {
            out.append(value.getCurrencyCode());
        }
    };

    private TypeWriters()
    {
    }

    private static <T> TypeWriter<T> appendUsingToString()
    {
        return (value, out) -> {
            if (value != null)
            {
                out.append(value.toString());
            }
        };
    }
}
