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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StreamSourcesTest
{
    @TempDir
    Path tempDir;

    @Test
    public void fromStringShouldExposeNameAndUtf8Content()
    {
        StreamSource source = StreamSources.fromString("alpha\nbeta\n", "values.csv");

        assertEquals("values.csv", source.getName());
        assertEquals("alpha\nbeta\n", StreamSources.getAsString(source));
        assertEquals("alpha\nbeta\n", StreamSources.getSnippet(source));
    }

    @Test
    public void fromBase64ShouldDecodeContentAndExposeMetadata()
    {
        String encoded = Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8));
        StreamSource source = StreamSources.fromBase64(encoded, "hello.txt", MimeType.TEXT_PLAIN);

        assertTrue(source instanceof DataSourceBase64);
        assertEquals("hello.txt", source.getName());
        assertEquals("hello", StreamSources.getAsString(source));
        assertEquals("hello", StreamSources.getSnippet(source));
        assertEquals(MimeType.TEXT_PLAIN, ((DataSourceBase64) source).getMimeType());
        assertEquals("hello.txt", ((DataSourceBase64) source).getResourceName());
        assertEquals(encoded, ((DataSourceBase64) source).getData());
    }

    @Test
    public void fromFileShouldReadBytesByFileAndByDirectoryAndName() throws Exception
    {
        Path file = Files.writeString(this.tempDir.resolve("sample.csv"), "x,y\n1,2\n", StandardCharsets.UTF_8);

        StreamSource byFile = StreamSources.fromFile(file.toFile());
        StreamSource byDirectoryAndName = StreamSources.fromFile(this.tempDir.toString(), "sample.csv");

        assertEquals("sample.csv", byFile.getName());
        assertEquals("sample.csv", byDirectoryAndName.getName());
        assertEquals("x,y\n1,2\n", StreamSources.getAsString(byFile));
        assertEquals("x,y\n1,2\n", StreamSources.getAsString(byDirectoryAndName));
    }

    @Test
    public void getSnippetShouldReturnEmptyStringForAnEmptyFile() throws Exception
    {
        Path empty = Files.write(this.tempDir.resolve("empty.txt"), new byte[0]);
        StreamSource source = StreamSources.fromFile(empty.toFile());

        assertEquals("", StreamSources.getSnippet(source));
    }

    @Test
    public void fromClassShouldOpenExistingResource()
    {
        StreamSource source = StreamSources.fromClass(StreamSourceProbe.class, "ExcelTestCase.xlsx");

        assertEquals("ExcelTestCase.xlsx", source.getName());
        assertNotNull(source.openStream());
        assertFalse(StreamSources.getSnippet(source).isEmpty());
    }
}
