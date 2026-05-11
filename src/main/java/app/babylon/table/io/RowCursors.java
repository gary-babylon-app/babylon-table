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

import java.io.InputStream;

import app.babylon.lang.ArgumentCheck;

public final class RowCursors
{
    private RowCursors()
    {
    }

    public static RowCursor create(ReadOptionsCsv options, InputStream inputStream)
    {
        return new RowCursorCsv(ArgumentCheck.nonNull(inputStream), ArgumentCheck.nonNull(options));
    }
}
