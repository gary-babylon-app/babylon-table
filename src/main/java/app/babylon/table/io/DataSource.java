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

/**
 * Supplies a named input stream for reading tabular data from an external
 * source.
 */
public interface DataSource
{
    String getName();

    InputStream openStream();
}
