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
import app.babylon.table.selection.Selection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnByteTest
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
    public void builderShouldCreateByteConstantWhenAllValuesAreSetAndEqual()
    {
        ColumnByte.Builder builder = ColumnByte.builder(ColumnName.of("B"));
        builder.add((byte) 7);
        builder.add((byte) 7);
        builder.add((byte) 7);

        ColumnByte column = builder.build();

        assertTrue(column instanceof ColumnByteConstant);
    }

    @Test
    public void builderShouldNotCreateByteConstantWhenNullsArePresent()
    {
        ColumnByte.Builder builder = ColumnByte.builder(ColumnName.of("B"));
        builder.add((byte) 7);
        builder.addNull();
        builder.add((byte) 7);

        ColumnByte column = builder.build();

        assertFalse(column instanceof ColumnByteConstant);
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
    public void selectShouldUseBytePredicateAndTreatNullAsFalse()
    {
        final ColumnName B = ColumnName.of("B");
        ColumnByte.Builder builder = ColumnByte.builder(B);
        builder.add((byte) 1);
        builder.addNull();
        builder.add((byte) 3);

        ColumnByte column = builder.build();
        Selection selection = column.select(v -> v > 1);

        assertEquals(3, selection.size());
        assertFalse(selection.get(0));
        assertFalse(selection.get(1));
        assertTrue(selection.get(2));
        assertEquals(1, selection.selected());
    }

    @Test
    public void getAsColumnShouldReturnByteConstant()
    {
        final ColumnName BYTES_BIN = ColumnName.of("BYTES_BIN");
        ColumnByte.Builder builder = ColumnByte.builder(BYTES_BIN);
        builder.add((byte) 1);
        builder.addNull();
        ColumnByte column = builder.build();

        ColumnByte single = (ColumnByte) column.getAsColumn(0);
        ColumnByte nullSingle = (ColumnByte) column.getAsColumn(1);

        assertEquals(1, single.size());
        assertEquals("1", single.toString(0));
        assertTrue(single.isSet(0));

        assertEquals(1, nullSingle.size());
        assertFalse(nullSingle.isSet(0));
        assertEquals("", nullSingle.toString(0));
    }

    @Test
    public void viewAndCopyShouldPreserveByteValuesAndNulls()
    {
        final ColumnName BYTES_BIN = ColumnName.of("BYTES_BIN");
        ColumnByte.Builder builder = ColumnByte.builder(BYTES_BIN);
        builder.add((byte) 1);
        builder.addNull();
        builder.add((byte) 3);
        ColumnByte column = builder.build();

        ViewIndex rowIndex = ViewIndex.builder().add(2).addNull().add(0).build();
        ColumnByte view = (ColumnByte) column.view(rowIndex);
        ColumnByte copy = view.copy(ColumnName.of("COPY"));

        assertEquals(3, view.size());
        assertEquals("3", view.toString(0));
        assertFalse(view.isSet(1));
        assertEquals("1", view.toString(2));

        assertEquals(ColumnName.of("COPY"), copy.getName());
        assertEquals(3, copy.size());
        assertEquals("3", copy.toString(0));
        assertFalse(copy.isSet(1));
        assertEquals("1", copy.toString(2));
    }

    @Test
    public void byteConstantShouldExposeValueSizeAndViewBehavior()
    {
        final ColumnName B = ColumnName.of("B");
        ColumnByteConstant constant = new ColumnByteConstant(B, (byte) 7, 4, true);

        assertEquals(B, constant.getName());
        assertEquals(7, constant.getValue());
        assertEquals(4, constant.getSize());
        assertEquals(4, constant.size());
        assertTrue(constant.isAllSet());
        assertFalse(constant.isNoneSet());

        ColumnByte constantView = (ColumnByte) constant.view(ViewIndex.builder().add(2).addNull().add(0).build());
        assertEquals(3, constantView.size());
        assertEquals("7", constantView.toString(0));
        assertFalse(constantView.isSet(1));
        assertEquals("7", constantView.toString(2));
    }

    @Test
    public void columnsNewByteShouldCreateByteConstant()
    {
        ColumnByte constant = Columns.newByte(ColumnName.of("B"), (byte) 9, 3);

        assertEquals(3, constant.size());
        assertEquals("9", constant.toString(0));
        assertEquals("9", constant.toString(2));
        assertTrue(constant.isAllSet());
    }
}
