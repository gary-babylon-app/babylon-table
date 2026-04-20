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

import java.util.function.Function;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.transform.TransformParseMode;
import app.babylon.text.Strings;

/**
 * Transforms individual column values from one type to another while carrying
 * the target column type and optional output column name.
 */
public interface Transformer<T, S> extends Function<T, S>
{
    public Column.Type type();

    public ColumnName columnName();

    static <T, S> Transformer<T, S> of(Function<? super T, ? extends S> function, Column.Type type)
    {
        return of(function, type, null);
    }

    static <T, S> Transformer<T, S> of(Function<? super T, ? extends S> function, Column.Type type,
            ColumnName columnName)
    {
        Function<? super T, ? extends S> f = ArgumentCheck.nonNull(function);
        Column.Type targetType = ArgumentCheck.nonNull(type);
        ColumnName name = columnName;
        return new Transformer<T, S>()
        {
            @Override
            public Column.Type type()
            {
                return targetType;
            }

            @Override
            public ColumnName columnName()
            {
                return name;
            }

            @Override
            public S apply(T t)
            {
                return f.apply(t);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <S> Transformer<String, S> parser(Column.Type type, TransformParseMode parseMode)
    {
        return parser(type, parseMode, null);
    }

    @SuppressWarnings("unchecked")
    static <S> Transformer<String, S> parser(Column.Type type, TransformParseMode parseMode, ColumnName columnName)
    {
        Column.Type targetType = ArgumentCheck.nonNull(type);
        TransformParseMode resolvedParseMode = parseMode == null ? TransformParseMode.EXACT : parseMode;
        return of(s -> {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            return resolvedParseMode.apply(x -> (S) targetType.getParser().parse(x), s);
        }, targetType, columnName);
    }
}
