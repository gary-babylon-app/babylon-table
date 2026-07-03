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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DataResourcesTest
{
    @TempDir
    Path tempDir;

    @Test
    public void fromStringShouldExposeNameAndUtf8Content()
    {
        DataResource source = DataResources.fromString("alpha\nbeta\n", "values.csv");

        assertEquals("values.csv", source.getName());
        assertEquals("alpha\nbeta\n", DataResources.getAsString(source));
        assertEquals("alpha\nbeta\n", DataResources.getSnippet(source));
    }

    @Test
    public void fromBytesShouldExposeNameAndRepeatableContent() throws Exception
    {
        byte[] bytes = "alpha\nbeta\n".getBytes(StandardCharsets.UTF_8);
        DataResource source = DataResources.fromBytes(bytes, "values.csv");

        assertEquals("values.csv", source.getName());
        assertEquals("alpha\nbeta\n", DataResources.getAsString(source));

        try (InputStream first = source.openStream(); InputStream second = source.openStream())
        {
            assertEquals('a', first.read());
            assertEquals('a', second.read());
        }
    }

    @Test
    public void fromBytesShouldDefensivelyCopyInputBytes()
    {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        DataResource source = DataResources.fromBytes(bytes, "hello.txt");

        bytes[0] = 'j';

        assertEquals("hello", DataResources.getAsString(source));
    }

    @Test
    public void fromBase64ShouldDecodeContentAndExposeMetadata()
    {
        String encoded = Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8));
        DataResource source = DataResources.fromBase64(encoded, "hello.txt", MimeType.TEXT_PLAIN);

        assertTrue(source instanceof DataResourceBase64);
        assertEquals("hello.txt", source.getName());
        assertEquals("hello", DataResources.getAsString(source));
        assertEquals("hello", DataResources.getSnippet(source));
        assertEquals(MimeType.TEXT_PLAIN, ((DataResourceBase64) source).getMimeType());
        assertEquals("hello.txt", ((DataResourceBase64) source).getResourceName());
        assertEquals(encoded, ((DataResourceBase64) source).getData());
    }

    @Test
    public void fromFileShouldReadBytesByFileAndByDirectoryAndName() throws Exception
    {
        Path file = Files.writeString(this.tempDir.resolve("sample.csv"), "x,y\n1,2\n", StandardCharsets.UTF_8);

        DataResource byFile = DataResources.fromFile(file.toFile());
        DataResource byDirectoryAndName = DataResources.fromFile(this.tempDir.toString(), "sample.csv");

        assertEquals("sample.csv", byFile.getName());
        assertEquals("sample.csv", byDirectoryAndName.getName());
        assertEquals("x,y\n1,2\n", DataResources.getAsString(byFile));
        assertEquals("x,y\n1,2\n", DataResources.getAsString(byDirectoryAndName));
    }

    @Test
    public void getSnippetShouldReturnEmptyStringForAnEmptyFile() throws Exception
    {
        Path empty = Files.write(this.tempDir.resolve("empty.txt"), new byte[0]);
        DataResource source = DataResources.fromFile(empty.toFile());

        assertEquals("", DataResources.getSnippet(source));
    }

    @Test
    public void fromClassShouldOpenExistingResource()
    {
        DataResource source = DataResources.fromClass(DataResourceProbe.class, "ExcelTestCase.xlsx");

        assertEquals("ExcelTestCase.xlsx", source.getName());
        assertNotNull(source.openStream());
        assertFalse(DataResources.getSnippet(source).isEmpty());
    }
}
