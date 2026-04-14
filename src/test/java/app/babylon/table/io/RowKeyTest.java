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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.text.BigDecimals;

class RowKeyTest
{
    @Test
    void shouldCompareAndHashByProjectedFieldValues()
    {
        RowKey key1 = RowKey.copyOf(new TestRow("A", "10", "X"), new int[]
        {0, 1});
        RowKey key2 = RowKey.copyOf(new TestRow("A", "10", "Y"), new int[]
        {0, 1});
        RowKey key3 = RowKey.copyOf(new TestRow("AB", "C", "Z"), new int[]
        {0, 1});
        RowKey key4 = RowKey.copyOf(new TestRow("A", "BC", "Q"), new int[]
        {0, 1});

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
        assertNotEquals(key3, key4);
        assertEquals("RowKey[A, 10]", key1.toString());
    }

    @Test
    void shouldUseCharBackedKeyForSingleField()
    {
        RowKey key = RowKey.copyOf(new TestRow("Zürich"), new int[]
        {0});

        assertEquals("RowKey1", key.getClass().getSimpleName());
        assertEquals("Zürich", key.getString(0));
    }

    @Test
    void shouldUseCharBackedKeyForNonLatin1SingleField()
    {
        RowKey key = RowKey.copyOf(new TestRow("Chișinău"), new int[]
        {0});

        assertEquals("RowKey1", key.getClass().getSimpleName());
        assertEquals("Chișinău", key.getString(0));
    }

    private static final class TestRow implements Row
    {
        private final char[] chars;
        private final int[] starts;
        private final int[] lengths;

        private TestRow(String... values)
        {
            this.starts = new int[values.length];
            this.lengths = new int[values.length];
            int totalLength = 0;
            for (String value : values)
            {
                totalLength += value.length();
            }
            this.chars = new char[totalLength];
            int writeIndex = 0;
            for (int i = 0; i < values.length; ++i)
            {
                String value = values[i];
                this.starts[i] = writeIndex;
                this.lengths[i] = value.length();
                value.getChars(0, value.length(), this.chars, writeIndex);
                writeIndex += value.length();
            }
        }

        @Override
        public int fieldCount()
        {
            return this.starts.length;
        }

        @Override
        public boolean isEmpty()
        {
            return this.starts.length == 0;
        }

        @Override
        public boolean isSet(int fieldIndex)
        {
            return this.lengths[fieldIndex] > 0;
        }

        @Override
        public char[] chars()
        {
            return this.chars;
        }

        @Override
        public int end()
        {
            return this.chars.length;
        }

        @Override
        public int start(int fieldIndex)
        {
            return this.starts[fieldIndex];
        }

        @Override
        public int length(int fieldIndex)
        {
            return this.lengths[fieldIndex];
        }

        public String getString(int fieldIndex)
        {
            return new String(this.chars, this.starts[fieldIndex], this.lengths[fieldIndex]);
        }

        public CharSequence getCharSequence(int fieldIndex)
        {
            return new RowBuffer.FieldCharSequence(this.chars, this.starts[fieldIndex], this.lengths[fieldIndex]);
        }

        public byte getByte(int fieldIndex)
        {
            return Byte.parseByte(getString(fieldIndex));
        }

        public int getInt(int fieldIndex)
        {
            return Integer.parseInt(getString(fieldIndex));
        }

        public long getLong(int fieldIndex)
        {
            return Long.parseLong(getString(fieldIndex));
        }

        public double getDouble(int fieldIndex)
        {
            return Double.parseDouble(getString(fieldIndex));
        }

        public BigDecimal getBigDecimal(int fieldIndex)
        {
            return BigDecimals.parse(getString(fieldIndex));
        }

        @Override
        public RowKey keyOf(int[] positions)
        {
            return RowKey.copyOf(this, positions);
        }

        @Override
        public Row copy()
        {
            return this;
        }
    }
}
