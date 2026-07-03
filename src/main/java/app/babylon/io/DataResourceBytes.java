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
import java.util.Arrays;

class DataResourceBytes implements DataResource
{
    private final byte[] bytes;
    private final String resourceName;

    DataResourceBytes(byte[] bytes, String resourceName)
    {
        if (bytes == null)
        {
            throw new IllegalArgumentException("bytes must not be null");
        }
        if (resourceName == null || resourceName.isEmpty())
        {
            throw new IllegalArgumentException("resourceName must not be empty");
        }
        this.bytes = Arrays.copyOf(bytes, bytes.length);
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
        return new ByteArrayInputStream(bytes);
    }
}
