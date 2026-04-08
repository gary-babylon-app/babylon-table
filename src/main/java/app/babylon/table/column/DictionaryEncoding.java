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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class DictionaryEncoding<T>
{
    private final Map<T, Integer> valueToCode;
    private Object[] codeToValue;
    private int size;

    private DictionaryEncoding()
    {
        this.valueToCode = new HashMap<>();
        this.codeToValue = new Object[16];
        this.size = 1;
    }

    static <T> DictionaryEncoding<T> of()
    {
        return new DictionaryEncoding<>();
    }

    int codeOf(T value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("Null values are not stored in the dictionary.");
        }
        Integer existing = this.valueToCode.get(value);
        if (existing != null)
        {
            return existing;
        }
        int code = this.size;
        this.valueToCode.put(value, code);
        ensureCapacity(code + 1);
        this.codeToValue[code] = value;
        ++this.size;
        return code;
    }

    @SuppressWarnings("unchecked")
    T valueOf(int code)
    {
        return (T) this.codeToValue[code];
    }

    int size()
    {
        return this.size;
    }

    Object[] detachValues()
    {
        Object[] out = Arrays.copyOf(this.codeToValue, this.size);
        this.codeToValue = null;
        this.size = 1;
        return out;
    }

    private void ensureCapacity(int requiredSize)
    {
        if (requiredSize <= this.codeToValue.length)
        {
            return;
        }
        int newSize = this.codeToValue.length + (this.codeToValue.length >>> 1) + 16;
        if (newSize < requiredSize)
        {
            newSize = requiredSize;
        }
        this.codeToValue = Arrays.copyOf(this.codeToValue, newSize);
    }
}
