/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.text;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public interface ByteSequence
{
    int length();

    byte byteAt(int index);

    default Charset charset()
    {
        return StandardCharsets.UTF_8;
    }

    default String decode(int start, int end)
    {
        Objects.checkFromToIndex(start, end, length());
        byte[] bytes = new byte[end - start];
        for (int i = 0; i < bytes.length; ++i)
        {
            bytes[i] = byteAt(start + i);
        }
        return new String(bytes, charset());
    }

    default String decode()
    {
        return decode(0, length());
    }

    default ByteString subSequence(int start, int end)
    {
        Objects.checkFromToIndex(start, end, length());
        ByteString.Builder builder = new ByteString.Builder(end - start, charset());
        builder.append(this, start, end);
        return builder.build();
    }
}
