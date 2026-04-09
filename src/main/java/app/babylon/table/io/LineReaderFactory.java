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

import app.babylon.io.DataSource;
import java.io.IOException;

/**
 * Creates {@link LineReader} instances for a data source and a set of read
 * settings.
 */
public interface LineReaderFactory
{
    LineReader create(DataSource dataSource, Csv.Settings readSettings) throws IOException;
}
