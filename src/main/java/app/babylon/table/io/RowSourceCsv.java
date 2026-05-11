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

/**
 * Configured CSV row source that opens a {@link RowCursorCsv} from a
 * {@link StreamSource}.
 */
final class RowSourceCsv implements RowSource
{
    private final StreamSource streamSource;
    private final ReadOptionsCsv options;

    RowSourceCsv(StreamSource streamSource, ReadOptionsCsv options)
    {
        this.streamSource = ArgumentCheck.nonNull(streamSource);
        this.options = ArgumentCheck.nonNull(options);
    }

    @Override
    public String getName()
    {
        return this.streamSource.getName();
    }

    @Override
    public RowCursor openRows()
    {
        return RowCursors.create(this.options, this.streamSource.openStream());
    }
}
