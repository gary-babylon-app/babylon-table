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

import app.babylon.io.StreamSource;
import app.babylon.lang.ArgumentCheck;

public final class RowSources
{
    private RowSources()
    {
    }

    public static RowSource create(ReadOptionsCsv options, StreamSource streamSource)
    {
        return new RowSourceCsv(ArgumentCheck.nonNull(streamSource), ArgumentCheck.nonNull(options));
    }
}
