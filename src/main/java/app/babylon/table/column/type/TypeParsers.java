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
import java.time.LocalDate;
import java.util.Currency;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.transform.ColumnLocalDates;
import app.babylon.table.transform.DateFormat;
import app.babylon.text.BigDecimals;
import app.babylon.text.Currencys;
import app.babylon.text.Strings;

public final class TypeParsers
{
    public static final TypeParser<Object> NULL = s -> null;
    public static final TypeParser<String> STRING = s -> s == null ? null : s.toString();
    public static final TypeParser<Byte> BYTE = new TypeParser<>()
    {
        @Override
        public Byte parse(CharSequence s)
        {
            return Strings.isEmpty(s) ? null : parseByte(s);
        }

        @Override
        public Byte parse(CharSequence s, int offset, int length)
        {
            return s == null || length <= 0 ? null : parseByte(s, offset, length);
        }
    };
    public static final TypeParser<Integer> INT = new TypeParser<>()
    {
        @Override
        public Integer parse(CharSequence s)
        {
            return Strings.isEmpty(s) ? null : parseInt(s);
        }

        @Override
        public Integer parse(CharSequence s, int offset, int length)
        {
            return s == null || length <= 0 ? null : parseInt(s, offset, length);
        }
    };
    public static final TypeParser<Long> LONG = new TypeParser<>()
    {
        @Override
        public Long parse(CharSequence s)
        {
            return Strings.isEmpty(s) ? null : parseLong(s);
        }

        @Override
        public Long parse(CharSequence s, int offset, int length)
        {
            return s == null || length <= 0 ? null : parseLong(s, offset, length);
        }
    };
    public static final TypeParser<Double> DOUBLE = new TypeParser<>()
    {
        @Override
        public Double parse(CharSequence s)
        {
            return Strings.isEmpty(s) ? null : parseDouble(s);
        }

        @Override
        public Double parse(CharSequence s, int offset, int length)
        {
            return s == null || length <= 0 ? null : parseDouble(s, offset, length);
        }
    };
    public static final TypeParser<BigDecimal> BIG_DECIMAL = BigDecimals::parse;
    public static final TypeParser<LocalDate> LOCAL_DATE_YMD = localDate(DateFormat.YMD);
    public static final TypeParser<Currency> CURRENCY = new TypeParser<>()
    {
        @Override
        public Currency parse(CharSequence s)
        {
            return Currencys.parse(s);
        }

        @Override
        public Currency parse(CharSequence s, int offset, int length)
        {
            return Currencys.parse(s, offset, length);
        }
    };

    private TypeParsers()
    {
    }

    public static TypeParser<LocalDate> localDate(DateFormat format)
    {
        DateFormat x = ArgumentCheck.nonNull(format);
        return s -> ColumnLocalDates.stringToDate(s, x);
    }
}
