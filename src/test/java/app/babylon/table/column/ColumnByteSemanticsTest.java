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
        ColumnByte.Builder builder = ColumnByte.builder(ColumnName.of("bytes.bin"));
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
        ColumnByte.Builder builder = ColumnByte.builder(ColumnName.of("sample.xls"));
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
        ColumnByte.Builder builder = ColumnByte.builder(ColumnName.of("sample.xlsx"));
        builder.add((byte) 0x50);
        builder.add((byte) 0x4B);
        builder.add((byte) 0x03);
        builder.add((byte) 0x04);
        ColumnByte xlsx = builder.build();

        assertTrue(xlsx.isXlsx());
        assertFalse(xlsx.isXls());

        ColumnByte.Builder zipBuilder = ColumnByte.builder(ColumnName.of("sample.zip"));
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
        ColumnByte.Builder builder = ColumnByte.builder(ColumnName.of("bytes.bin"));
        builder.add((byte) 1);
        ColumnByte column = builder.build();

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(0);
        ViewIndex rowIndex = rowIndexBuilder.build();

        assertThrows(UnsupportedOperationException.class, () -> column.view(rowIndex));
        assertThrows(UnsupportedOperationException.class, () -> column.getAsColumn(0));
    }
}
