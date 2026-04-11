/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import app.babylon.table.ViewIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnByteSemanticsTest
{
    @Test
    public void byteColumnTracksNullsAndValues()
    {
        final ColumnName BYTES_BIN = ColumnName.of("BYTES_BIN");
        ColumnByte.Builder builder = ColumnByte.builder(BYTES_BIN);
        builder.add((byte) 42);
        builder.addNull();
        builder.add((byte) -5);

        ColumnByte column = builder.build();

        assertEquals(3, column.size());
        assertTrue(column.isSet(0));
        assertFalse(column.isSet(1));
        assertTrue(column.isSet(2));
        assertEquals("42", column.toString(0));
        assertEquals("", column.toString(1));
        assertEquals("-5", column.toString(2));
    }

    @Test
    public void detectsXlsHeader()
    {
        final ColumnName SAMPLE_XLS = ColumnName.of("SAMPLE_XLS");
        ColumnByte.Builder builder = ColumnByte.builder(SAMPLE_XLS);
        builder.add((byte) 0xD0);
        builder.add((byte) 0xCF);
        builder.add((byte) 0x11);
        builder.add((byte) 0xE0);
        builder.add((byte) 0xA1);
        builder.add((byte) 0xB1);
        builder.add((byte) 0x1A);
        builder.add((byte) 0xE1);
        ColumnByte column = builder.build();

        assertTrue(column.isXls());
        assertFalse(column.isXlsx());
    }

    @Test
    public void detectsXlsxZipHeaderUnlessNamedZip()
    {
        final ColumnName SAMPLE_XLSX = ColumnName.of("SAMPLE_XLSX");
        final ColumnName SAMPLE_ZIP = ColumnName.of("SAMPLE_ZIP");
        ColumnByte.Builder builder = ColumnByte.builder(SAMPLE_XLSX);
        builder.add((byte) 0x50);
        builder.add((byte) 0x4B);
        builder.add((byte) 0x03);
        builder.add((byte) 0x04);
        ColumnByte xlsx = builder.build();

        assertTrue(xlsx.isXlsx());
        assertFalse(xlsx.isXls());

        ColumnByte.Builder zipBuilder = ColumnByte.builder(SAMPLE_ZIP);
        zipBuilder.add((byte) 0x50);
        zipBuilder.add((byte) 0x4B);
        zipBuilder.add((byte) 0x03);
        zipBuilder.add((byte) 0x04);
        ColumnByte zip = zipBuilder.build();

        assertFalse(zip.isXlsx());
    }

    @Test
    public void viewAndGetAsColumnRemainUnsupportedForNow()
    {
        final ColumnName BYTES_BIN = ColumnName.of("BYTES_BIN");
        ColumnByte.Builder builder = ColumnByte.builder(BYTES_BIN);
        builder.add((byte) 1);
        ColumnByte column = builder.build();

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(0);
        ViewIndex rowIndex = rowIndexBuilder.build();

        assertThrows(UnsupportedOperationException.class, () -> column.view(rowIndex));
        assertThrows(UnsupportedOperationException.class, () -> column.getAsColumn(0));
    }
}
