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
import java.util.function.Function;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.transform.ColumnLocalDates;
import app.babylon.table.transform.DateFormat;
import app.babylon.text.BigDecimals;
import app.babylon.text.Currencys;
import app.babylon.text.Strings;

public final class TypeParsers
{
    public static final TypeParser<String> STRING = s -> s == null ? null : s.toString();
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

    public static <E extends Enum<E>> TypeParser<E> enumParser(Function<CharSequence, E> parser)
    {
        Function<CharSequence, E> x = ArgumentCheck.nonNull(parser);
        return s -> Strings.isEmpty(s) ? null : x.apply(s);
    }

    public static <E extends Enum<E>> TypeParser<E> enumParser(Class<E> enumClass)
    {
        Class<E> x = ArgumentCheck.nonNull(enumClass);
        return s -> parseEnum(x, s);
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> enumClass, CharSequence s)
    {
        Class<E> x = ArgumentCheck.nonNull(enumClass);
        if (Strings.isEmpty(s))
        {
            return null;
        }
        String text = s.toString().strip();
        if (text.isEmpty())
        {
            return null;
        }
        try
        {
            return Enum.valueOf(x, text);
        }
        catch (IllegalArgumentException e)
        {
            // Fall through to the normalized lookups below.
        }
        try
        {
            return Enum.valueOf(x, text.toLowerCase());
        }
        catch (IllegalArgumentException e)
        {
            // Fall through to the normalized lookups below.
        }
        try
        {
            return Enum.valueOf(x, text.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
