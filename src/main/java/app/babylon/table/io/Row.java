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
 * Represents a parsed input row as offsets into a shared character buffer.
 */
public interface Row
{
    int fieldCount();

    char[] chars();

    int end();

    int start(int fieldIndex);

    int length(int fieldIndex);

    Row copy();
}
