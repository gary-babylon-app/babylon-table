/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.io;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

class DataSourceFile implements DataSource
{
    private final File file;

    DataSourceFile(File file)
    {
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    @Override
    public InputStream openStream()
    {
        try
        {
            return new FileInputStream(file);
        } catch (FileNotFoundException e)
        {
            throw new RuntimeException("Failed to open stream " + getName(), e);
        }
    }

    @Override
    public String getName()
    {
        return this.file.getName();
    }
}
