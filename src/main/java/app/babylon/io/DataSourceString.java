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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class DataSourceString implements DataSource
{
    private final String data;
    private final String resourceName;

    public DataSourceString(CharSequence data, String resourceName)
    {
        if (data == null || data.isEmpty())
        {
            throw new IllegalArgumentException("data must not be empty");
        }
        if (resourceName == null || resourceName.isEmpty())
        {
            throw new IllegalArgumentException("resourceName must not be empty");
        }
        this.data = data.toString();
        this.resourceName = resourceName;
    }

    @Override
    public String getName()
    {
        return resourceName;
    }

    @Override
    public InputStream openStream()
    {
        return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }
}
