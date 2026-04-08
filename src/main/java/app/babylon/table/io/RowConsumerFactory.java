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
 * Creates row consumers after header detection has established how incoming
 * rows should be interpreted.
 */
public interface RowConsumerFactory<T>
{
    RowConsumerResult<T> create(ReadSettingsCSV options, HeaderDetection headerDetection);
}
