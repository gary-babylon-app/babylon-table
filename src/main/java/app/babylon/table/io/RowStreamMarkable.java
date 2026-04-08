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

import java.io.IOException;

/**
 * Streams rows with support for marking a position and resetting back to it.
 */
public interface RowStreamMarkable
{
    boolean next() throws IOException;

    Row current();

    void mark(int rowIndex);

    /**
     * Reset the stream to the first data row following the row previously marked by
     * {@link #mark(int)}. If no row was marked, resets to the start of the recorded
     * rows.
     */
    void reset();

}
