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

import app.babylon.lang.ArgumentCheck;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import app.babylon.table.column.Column.Type;
import app.babylon.table.column.type.TypeParser;
import app.babylon.table.column.type.TypeParsers;

public final class ColumnTypes
{
    public static final Type BYTE = new ColumnTypeByClass(byte.class, "byte", TypeParsers.STRING);
    public static final Type BYTE_OBJECT = new ColumnTypeByClass(Byte.class, "byte_object", TypeParsers.STRING);
    public static final Type INT = new ColumnTypeByClass(int.class, "int", TypeParsers.STRING);
    public static final Type INT_OBJECT = new ColumnTypeByClass(Integer.class, "int_object", TypeParsers.STRING);
    public static final Type LONG = new ColumnTypeByClass(long.class, "long", TypeParsers.STRING);
    public static final Type LONG_OBJECT = new ColumnTypeByClass(Long.class, "long_object", TypeParsers.STRING);
    public static final Type DOUBLE = new ColumnTypeByClass(double.class, "double", TypeParsers.STRING);
    public static final Type DOUBLE_OBJECT = new ColumnTypeByClass(Double.class, "double_object", TypeParsers.STRING);
    public static final Type STRING = new ColumnTypeByClass(String.class, "string", TypeParsers.STRING);
    public static final Type DECIMAL = new ColumnTypeByClass(BigDecimal.class, "bigdecimal", TypeParsers.BIG_DECIMAL);
    public static final Type LOCALDATE = new ColumnTypeByClass(LocalDate.class, "localdate",
            TypeParsers.LOCAL_DATE_YMD);

    private ColumnTypes()
    {
    }

    static Column.Type of(Class<?> valueClass)
    {
        Class<?> clazz = ArgumentCheck.nonNull(valueClass);
        Column.Type builtin = builtinOf(clazz);
        if (builtin != null)
        {
            return builtin;
        }
        return new ColumnTypeByClass(clazz);
    }

    private static Column.Type builtinOf(Class<?> clazz)
    {
        if (byte.class.equals(clazz))
        {
            return BYTE;
        }
        if (Byte.class.equals(clazz))
        {
            return BYTE_OBJECT;
        }
        if (int.class.equals(clazz))
        {
            return INT;
        }
        if (Integer.class.equals(clazz))
        {
            return INT_OBJECT;
        }
        if (long.class.equals(clazz))
        {
            return LONG;
        }
        if (Long.class.equals(clazz))
        {
            return LONG_OBJECT;
        }
        if (double.class.equals(clazz))
        {
            return DOUBLE;
        }
        if (Double.class.equals(clazz))
        {
            return DOUBLE_OBJECT;
        }
        if (String.class.equals(clazz))
        {
            return STRING;
        }
        if (BigDecimal.class.equals(clazz))
        {
            return DECIMAL;
        }
        if (LocalDate.class.equals(clazz))
        {
            return LOCALDATE;
        }
        return null;
    }

    private static final class ColumnTypeByClass implements Column.Type
    {
        private final Class<?> valueClass;
        private final String id;
        private final TypeParser<?> parser;
        private volatile TypeParser<?> lazyEnumParser;

        private ColumnTypeByClass(Class<?> valueClass)
        {
            this(valueClass, null, null);
        }

        private ColumnTypeByClass(Class<?> valueClass, String id)
        {
            this(valueClass, id, null);
        }

        private ColumnTypeByClass(Class<?> valueClass, String id, TypeParser<?> parser)
        {
            this.valueClass = ArgumentCheck.nonNull(valueClass);
            this.parser = parser;
            if (id == null)
            {
                String simpleName = valueClass.getSimpleName();
                String name = simpleName.isEmpty() ? valueClass.getName() : simpleName;
                this.id = name.toLowerCase(Locale.ROOT);
            }
            else
            {
                this.id = id.toLowerCase(Locale.ROOT);
            }
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
        public TypeParser<?> getParser()
        {
            if (this.parser != null)
            {
                return this.parser;
            }
            if (this.valueClass.isEnum())
            {
                TypeParser<?> x = this.lazyEnumParser;
                if (x == null)
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) this.valueClass
                            .asSubclass(Enum.class);
                    x = TypeParsers.enumParser((Class) enumClass);
                    this.lazyEnumParser = x;
                }
                return x;
            }
            return null;
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
