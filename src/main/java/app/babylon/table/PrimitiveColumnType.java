/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

enum PrimitiveColumnType implements Column.Type
{
    BYTE("byte", byte.class), INT("int", int.class), LONG("long", long.class), DOUBLE("double", double.class);

    private final String id;
    private final Class<?> valueClass;

    PrimitiveColumnType(String id, Class<?> valueClass)
    {
        this.id = id;
        this.valueClass = valueClass;
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
}
