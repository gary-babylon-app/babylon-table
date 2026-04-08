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
import java.io.InputStream;

class DataSourceClassResource implements DataSource
{
    private final Class<?> clazz;
    private final String name;

    DataSourceClassResource(Class<?> clazz, String name)
    {
        this.clazz = app.babylon.lang.ArgumentCheck.nonNull(clazz, "clazz must not be null");
        if (name == null || name.isEmpty())
        {
            throw new IllegalArgumentException("name must not be empty");
        }
        this.name = name;
    }

    @Override
    public InputStream openStream()
    {
        return this.clazz.getResourceAsStream(this.name);
    }

    public String getName()
    {
        return name;
    }
}
