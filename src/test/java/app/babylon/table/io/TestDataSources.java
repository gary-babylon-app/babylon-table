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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class TestDataSources
{
    private TestDataSources()
    {
    }

    static DataSource fromString(String text, String name)
    {
        return fromBytes(text.getBytes(StandardCharsets.UTF_8), name);
    }

    static DataSource fromBytes(byte[] bytes, String name)
    {
        final byte[] payload = Arrays.copyOf(bytes, bytes.length);
        return new DataSource()
        {
            @Override
            public String getName()
            {
                return name;
            }

            @Override
            public ByteArrayInputStream openStream()
            {
                return new ByteArrayInputStream(payload);
            }
        };
    }
}
