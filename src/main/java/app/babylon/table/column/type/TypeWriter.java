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

/**
 * Writes a typed value into text.
 *
 * @param <T>
 *            the value type written by this writer
 */
public interface TypeWriter<T>
{
    void write(T value, StringBuilder out);

    default String write(T value)
    {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }
}
