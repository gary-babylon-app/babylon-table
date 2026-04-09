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

import java.io.Closeable;
import java.io.IOException;

/**
 * Iterates through parsed rows from an input source while exposing the current
 * row and reader settings.
 */
public interface LineReader extends Closeable
{
    boolean next() throws IOException;

    Row current();

    Csv.ReadSettings getSettings();
}
