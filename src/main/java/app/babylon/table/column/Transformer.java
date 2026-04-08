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

import java.util.Objects;
import java.util.function.Function;

/**
 * Transforms individual column values from one type to another while carrying
 * the target value type and optional output column name.
 */
public interface Transformer<T, S> extends Function<T, S>
{
    public Class<S> valueClass();

    default public ColumnName columnName()
    {
        return null;
    }

    static <T, S> Transformer<T, S> of(Function<? super T, ? extends S> function, Class<S> valueClass)
    {
        return of(function, valueClass, null);
    }

    static <T, S> Transformer<T, S> of(Function<? super T, ? extends S> function, Class<S> valueClass,
            ColumnName columnName)
    {
        Function<? super T, ? extends S> f = Objects.requireNonNull(function);
        Class<S> cls = Objects.requireNonNull(valueClass);
        ColumnName name = columnName;
        return new Transformer<T, S>()
        {
            @Override
            public Class<S> valueClass()
            {
                return cls;
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
}
