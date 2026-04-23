/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

/**
 * Represents a parsed input row represented as a character slice.
 */
public interface Row extends CharSequence
{
    int size();

    boolean isEmpty();

    boolean isSet(int fieldIndex);

    int start(int fieldIndex);

    int length(int fieldIndex);

    @Override
    default CharSequence subSequence(int start, int end)
    {
        return new RowBuffer.FieldCharSequence(this, start, end - start);
    }

    RowKey keyOf(int[] fieldPositions);

    Row copy();
}
