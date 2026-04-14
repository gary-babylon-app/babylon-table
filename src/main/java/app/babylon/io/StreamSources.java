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
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StreamSources
{
    public static StreamSource fromFile(String directory, String fileName)
    {
        File file = new File(directory, fileName);
        return new DataSourceFile(file);
    }

    public static StreamSource fromFile(File file)
    {
        return new DataSourceFile(file);
    }

    public static StreamSource fromClass(Class<?> clazz, String name)
    {
        return new DataSourceClassResource(clazz, name);
    }

    public static StreamSource fromString(CharSequence data, String resourceName)
    {
        return new DataSourceString(data, resourceName);
    }

    public static StreamSource fromBase64(String fileBase64, String resourceName, MimeType mimeType)
    {
        return new DataSourceBase64(fileBase64, resourceName, mimeType);
    }

    public static String getAsString(StreamSource stream)
    {
        try (InputStream inputStream = stream.openStream())
        {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            for (int result = bis.read(); result != -1; result = bis.read())
            {
                buf.write((byte) result);
            }
            return buf.toString(StandardCharsets.UTF_8.name());
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    public static String getSnippet(StreamSource stream)
    {
        try (InputStream inputStream = stream.openStream())
        {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[512];
            int count = bis.read(buffer, 0, buffer.length);
            if (count <= 0)
            {
                return "";
            }
            return new String(buffer, 0, count, StandardCharsets.UTF_8);
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }
}
