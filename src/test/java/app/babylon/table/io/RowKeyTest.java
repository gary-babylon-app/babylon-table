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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RowKeyTest
{
    @Test
    void rejectsInvalidInputs()
    {
        RowBuffer row = row("A");
        CharSequence[] values =
        {"A"};

        assertHasMessage(messageWhenCopyingRow((Row) null, new int[]
        {0}));
        assertHasMessage(messageWhenCopyingValues((CharSequence[]) null, new int[]
        {0}));
        assertHasMessage(messageWhenCopyingRow(row, null));
        assertHasMessage(messageWhenCopyingRow(row, new int[0]));
        assertHasMessage(messageWhenCopyingValues(values, null));
        assertHasMessage(messageWhenCopyingValues(values, new int[0]));
    }

    @Test
    void comparesProjectedFields()
    {
        RowKey key1 = RowKey.of(row("A", "10", "X"), new int[]
        {0, 1});
        RowKey key2 = RowKey.of(row("A", "10", "Y"), new int[]
        {0, 1});
        RowKey key3 = RowKey.of(row("AB", "C", "Z"), new int[]
        {0, 1});
        RowKey key4 = RowKey.of(row("A", "BC", "Q"), new int[]
        {0, 1});

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
        assertNotEquals(key3, key4);
        assertEquals("RowKey[A, 10]", key1.toString());
    }

    @Test
    void usesRowKey1()
    {
        RowKey key = RowKey.of(row("Zürich"), new int[]
        {0});

        assertEquals("Zürich", key.getString(0));
    }

    @Test
    void usesRowKey1ForUnicode()
    {
        RowKey key = RowKey.of(row("Chișinău"), new int[]
        {0});

        assertEquals("Chișinău", key.getString(0));
    }

    @Test
    void comparesSingleFieldKeys()
    {
        RowKey rowBacked = RowKey.of(row("Alpha"), new int[]
        {0});
        RowKey valuesBacked = RowKey.of(new CharSequence[]
        {"Alpha"}, new int[]
        {0});

        assertEquals(rowBacked, rowBacked);
        assertEquals(rowBacked, valuesBacked);
        assertEquals(valuesBacked, rowBacked);
        assertEquals(rowBacked.hashCode(), valuesBacked.hashCode());
        assertNotEquals(rowBacked, RowKey.of(row("Beta"), new int[]
        {0}));
        assertNotEquals(rowBacked, RowKey.of(row("Alpha", "Z"), new int[]
        {0, 1}));
        assertNotEquals(rowBacked, RowKey.of(row("Al", "pha"), new int[]
        {0, 1}));
        assertNotEquals(rowBacked, "Alpha");
    }

    @Test
    void comparesTwoFieldKeys()
    {
        RowKey rowBacked = RowKey.of(row("AA", "BB", "tail"), new int[]
        {0, 1});
        RowKey valuesBacked = RowKey.of(new CharSequence[]
        {"AA", "BB", "tail"}, new int[]
        {0, 1});

        assertEquals(rowBacked, rowBacked);
        assertEquals(rowBacked, valuesBacked);
        assertEquals(valuesBacked, rowBacked);
        assertEquals(rowBacked.hashCode(), valuesBacked.hashCode());
        assertEquals("AA", rowBacked.getString(0));
        assertEquals("BB", rowBacked.getString(1));
        assertNotEquals(rowBacked, RowKey.of(row("AABB"), new int[]
        {0}));
        assertNotEquals(rowBacked, RowKey.of(row("A", "A", "BB"), new int[]
        {0, 1, 2}));
        assertNotEquals(rowBacked, RowKey.of(new CharSequence[]
        {"A", "ABB", "tail"}, new int[]
        {0, 1}));
        assertNotEquals(rowBacked, RowKey.of(new CharSequence[]
        {"AA", "BC", "tail"}, new int[]
        {0, 1}));
        assertNotEquals(rowBacked, "AA,BB");
    }

    @Test
    void usesRowKey3()
    {
        RowKey rowBacked = RowKey.of(row("A", "BB", "CCC", "tail"), new int[]
        {0, 1, 2});
        RowKey valuesBacked = RowKey.of(new CharSequence[]
        {"A", "BB", "CCC", "tail"}, new int[]
        {0, 1, 2});

        assertEquals(rowBacked, rowBacked);
        assertEquals(rowBacked, valuesBacked);
        assertEquals(valuesBacked, rowBacked);
        assertEquals(rowBacked.hashCode(), valuesBacked.hashCode());
        assertEquals("RowKey[A, BB, CCC]", rowBacked.toString());
        assertNotEquals(rowBacked, RowKey.of(row("ABBCCC"), new int[]
        {0}));
        assertNotEquals(rowBacked, RowKey.of(row("A", "BB", "CC", "C"), new int[]
        {0, 1, 2, 3}));
        assertNotEquals(rowBacked, RowKey.of(new CharSequence[]
        {"A", "BB", "CCX", "tail"}, new int[]
        {0, 1, 2}));
        assertNotEquals(rowBacked, RowKey.of(new CharSequence[]
        {"ABB", "CCC", "tail"}, new int[]
        {0, 1}));
        assertNotEquals(rowBacked, "A,BB,CCC");
    }

    @Test
    void usesRowKeyN()
    {
        RowKey rowBacked = RowKey.of(row("A", "", "CCC", "DD", "tail"), new int[]
        {0, 1, 2, 3});
        RowKey valuesBacked = RowKey.of(new CharSequence[]
        {"A", null, "CCC", "DD", "tail"}, new int[]
        {0, 1, 2, 3});

        assertEquals(rowBacked, rowBacked);
        assertEquals(rowBacked, valuesBacked);
        assertEquals(valuesBacked, rowBacked);
        assertEquals(rowBacked.hashCode(), valuesBacked.hashCode());
        assertEquals("", rowBacked.getString(1));
        assertEquals("CCC", rowBacked.getString(2));
        assertNotEquals(rowBacked, RowKey.of(row("A", "", "CCCDD"), new int[]
        {0, 1, 2}));
        assertNotEquals(rowBacked, RowKey.of(new CharSequence[]
        {"A", null, "CCC", "DX", "tail"}, new int[]
        {0, 1, 2, 3}));
        assertNotEquals(rowBacked, RowKey.of(new CharSequence[]
        {"A", null, "CCC"}, new int[]
        {0, 1, 2}));
        assertNotEquals(rowBacked, "A,,CCC,DD");
    }

    @Test
    void comparesAgainstCustomRowKeyImplementations()
    {
        RowKey key1 = RowKey.of(row("Alpha"), new int[]
        {0});
        RowKey key2 = RowKey.of(row("AA", "BB"), new int[]
        {0, 1});
        RowKey key3 = RowKey.of(row("A", "BB", "CCC"), new int[]
        {0, 1, 2});
        RowKey keyN = RowKey.of(row("A", "BB", "CCC", "DD"), new int[]
        {0, 1, 2, 3});

        assertEquals(key1, new CustomRowKey("Alpha"));
        assertEquals(key2, new CustomRowKey("AA", "BB"));
        assertEquals(key3, new CustomRowKey("A", "BB", "CCC"));
        assertEquals(keyN, new CustomRowKey("A", "BB", "CCC", "DD"));

        assertNotEquals(key1, new CustomRowKey("Beta"));
        assertNotEquals(key1, new CustomRowKey("AlphZ"));
        assertNotEquals(key1, new CustomRowKey("Alph", "a"));
        assertNotEquals(key1, "Alpha");
        assertNotEquals(key2, new CustomRowKey("AA", "BC"));
        assertNotEquals(key2, new CustomRowKey("AZ", "BB"));
        assertNotEquals(key2, new CustomRowKey("AABB"));
        assertNotEquals(key2, new CustomRowKey("AA", "B"));
        assertNotEquals(key3, new CustomRowKey("A", "BB", "CCX"));
        assertNotEquals(key3, new CustomRowKey("A", "BZ", "CCC"));
        assertNotEquals(key3, new CustomRowKey("A", "BB"));
        assertNotEquals(key3, new CustomRowKey("A", "BB", "CC", "C"));
        assertNotEquals(keyN, new CustomRowKey("A", "BB", "CCC", "DX"));
        assertNotEquals(keyN, new CustomRowKey("A", "BB", "CCZ", "DD"));
        assertNotEquals(keyN, new CustomRowKey("A", "BB", "CCC"));
        assertNotEquals(keyN, new CustomRowKey("A", "BB", "CC", "DD"));
    }

    private static RowBuffer row(String... values)
    {
        RowBuffer row = new RowBuffer();
        for (String value : values)
        {
            if (value != null)
            {
                row.append(value);
            }
            row.finishField();
        }
        return row;
    }

    private String messageWhenCopyingRow(Row row, int[] positions)
    {
        try
        {
            RowKey.of(row, positions);
        }
        catch (IllegalArgumentException ex)
        {
            return ex.getMessage();
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private String messageWhenCopyingValues(CharSequence[] values, int[] positions)
    {
        try
        {
            RowKey.of(values, positions);
        }
        catch (IllegalArgumentException ex)
        {
            return ex.getMessage();
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private void assertHasMessage(String message)
    {
        assertFalse(message == null || message.isBlank());
    }

    private static final class CustomRowKey extends RowKey
    {
        private final String[] values;
        private final int[] starts;
        private final int[] lengths;
        private final char[] chars;

        private CustomRowKey(String... values)
        {
            this.values = values;
            this.starts = new int[values.length];
            this.lengths = new int[values.length];

            int totalLength = 0;
            for (String value : values)
            {
                totalLength += value.length();
            }

            this.chars = new char[totalLength];
            int charIndex = 0;
            for (int i = 0; i < values.length; ++i)
            {
                String value = values[i];
                this.starts[i] = charIndex;
                this.lengths[i] = value.length();
                value.getChars(0, value.length(), this.chars, charIndex);
                charIndex += value.length();
            }
        }

        @Override
        public int fieldCount()
        {
            return this.values.length;
        }

        @Override
        protected int fieldStart(int fieldIndex)
        {
            return this.starts[fieldIndex];
        }

        @Override
        protected int fieldLength(int fieldIndex)
        {
            return this.lengths[fieldIndex];
        }

        @Override
        protected char charAt(int fieldIndex, int charIndex)
        {
            return this.chars[this.starts[fieldIndex] + charIndex];
        }

        @Override
        public int hashCode()
        {
            int hash = this.values.length;
            for (String value : this.values)
            {
                hash = 31 * hash + value.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof RowKey other))
            {
                return false;
            }
            if (other.fieldCount() != this.values.length)
            {
                return false;
            }
            for (int i = 0; i < this.values.length; ++i)
            {
                if (other.fieldLength(i) != this.lengths[i])
                {
                    return false;
                }
                for (int j = 0; j < this.lengths[i]; ++j)
                {
                    if (charAt(i, j) != other.charAt(i, j))
                    {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
