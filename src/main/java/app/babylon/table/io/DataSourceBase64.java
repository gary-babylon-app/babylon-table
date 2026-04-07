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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

class DataSourceBase64 implements DataSource
{
    private final String data;
    private final String resourceName;
    private final MimeType mimeType;

    private volatile byte[] byteArray;

    public DataSourceBase64(CharSequence data, String resourceName, MimeType mimeType)
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
        this.mimeType = mimeType;
    }

    public MimeType getMimeType()
    {
        return mimeType;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getData()
    {
        return data;
    }

    @Override
    public String getName()
    {
        return resourceName;
    }

    @Override
    public InputStream openStream()
    {
        byte[] local = this.byteArray;

        if (local == null && !data.isEmpty())
        {
            local = Base64.getDecoder().decode(data);
            this.byteArray = local;
        }
        return new ByteArrayInputStream(local);
    }
}
