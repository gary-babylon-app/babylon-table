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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.babylon.table.column.Column.Type;

public final class ColumnTypes
{

    private static final Map<Class<?>, Column.Type> CACHE = new ConcurrentHashMap<>();

    private ColumnTypes()
    {
    }

    static Column.Type of(Class<?> valueClass)
    {
        Class<?> clazz = app.babylon.lang.ArgumentCheck.nonNull(valueClass);
        Column.Type builtin = builtinOf(clazz);
        if (builtin != null)
        {
            return builtin;
        }
        return CACHE.computeIfAbsent(clazz, ColumnTypeByClass::new);
    }

    public static final Type STRING = Type.of(String.class);
    public static final Type DECIMAL = Type.of(BigDecimal.class);
    public static final Type LOCALDATE = Type.of(LocalDate.class);

    private static Column.Type builtinOf(Class<?> clazz)
    {
        if (byte.class.equals(clazz))
        {
            return ColumnByte.TYPE;
        }
        if (int.class.equals(clazz))
        {
            return ColumnInt.TYPE;
        }
        if (long.class.equals(clazz))
        {
            return ColumnLong.TYPE;
        }
        if (double.class.equals(clazz))
        {
            return ColumnDouble.TYPE;
        }
        return null;
    }

    private static final class ColumnTypeByClass implements Column.Type
    {
        private final Class<?> valueClass;
        private final String id;

        private ColumnTypeByClass(Class<?> valueClass)
        {
            this.valueClass = app.babylon.lang.ArgumentCheck.nonNull(valueClass);
            String simpleName = valueClass.getSimpleName();
            String name = simpleName.isEmpty() ? valueClass.getName() : simpleName;
            this.id = name.toLowerCase(Locale.ROOT);
        }

        @Override
        public String id()
        {
            return this.id;
        }

        @Override
        public Class<?> getValueClass()
        {
            return this.valueClass;
        }

        @Override
        public int hashCode()
        {
            return this.valueClass.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof Column.Type other))
            {
                return false;
            }
            return this.valueClass.equals(other.getValueClass());
        }

        @Override
        public String toString()
        {
            return this.id;
        }
    }
}
