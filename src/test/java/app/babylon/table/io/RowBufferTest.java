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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

class RowBufferTest
{
    @Test
    void fieldCharSequenceShouldExposeCharsSubSequencesAndBounds()
    {
        RowBuffer.FieldCharSequence sequence = new RowBuffer.FieldCharSequence("xxAlphaYY".toCharArray(), 2, 5);

        assertEquals(5, sequence.length());
        assertEquals('A', sequence.charAt(0));
        assertEquals('a', sequence.charAt(4));
        assertEquals("Alpha", sequence.toString());
        assertEquals("lph", sequence.subSequence(1, 4).toString());
        assertEquals("", sequence.subSequence(2, 2).toString());

        assertThrows(IndexOutOfBoundsException.class, () -> sequence.charAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.charAt(5));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.subSequence(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.subSequence(3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.subSequence(0, 6));
    }

    @Test
    void rowBufferShouldAppendFinishFieldsAndReturnStrings()
    {
        RowBuffer row = new RowBuffer();
        row.append("Alpha");
        row.finishField();
        row.append("Beta");
        row.finishField();
        row.finishField();

        assertEquals(3, row.size());
        assertEquals("Alpha", row.getString(0));
        assertEquals("Beta", row.getString(1));
        assertNull(row.getString(2));
        assertEquals(0, row.start(0));
        assertEquals(5, row.length(0));
        assertEquals(5, row.start(1));
        assertEquals(4, row.length(1));
        assertEquals(9, row.start(2));
        assertEquals(0, row.length(2));
        assertEquals(9, row.length());
        assertEquals("Alpha", row.subSequence(row.start(0), row.start(0) + row.length(0)).toString());
    }

    @Test
    void rowBufferShouldAppendFromArrayReaderAndCopy() throws IOException
    {
        RowBuffer row = new RowBuffer();
        row.append(new char[]
        {'A', 'l', 'p', 'h', 'a'}, 0, 5);
        row.finishField();
        row.append((CharSequence) null);
        row.append(new StringReader("Beta"));
        row.finishField();

        RowBuffer copy = row.copy();

        assertEquals("Alpha", copy.getString(0));
        assertEquals("Beta", copy.getString(1));
        assertEquals(2, copy.size());
        assertEquals(9, copy.length());

        row.clear();
        assertEquals(0, row.size());
        assertEquals(0, row.length());
        assertEquals("Alpha", copy.getString(0));
        assertEquals("Beta", copy.getString(1));
    }
}
