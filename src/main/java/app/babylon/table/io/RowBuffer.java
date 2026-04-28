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

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public final class RowBuffer implements Row
{
    static final class FieldCharSequence implements CharSequence
    {
        private final CharSequence chars;
        private final int start;
        private final int length;

        FieldCharSequence(CharSequence chars, int start, int length)
        {
            this.chars = chars;
            this.start = start;
            this.length = length;
        }

        FieldCharSequence(char[] chars, int start, int length)
        {
            this(CharBuffer.wrap(chars), start, length);
        }

        @Override
        public int length()
        {
            return this.length;
        }

        @Override
        public char charAt(int index)
        {
            if (index < 0 || index >= this.length)
            {
                throw new IndexOutOfBoundsException();
            }
            return this.chars.charAt(this.start + index);
        }

        @Override
        public CharSequence subSequence(int start, int end)
        {
            if (start < 0 || end < start || end > this.length)
            {
                throw new IndexOutOfBoundsException();
            }
            return new FieldCharSequence(this.chars, this.start + start, end - start);
        }

        @Override
        public String toString()
        {
            char[] text = new char[this.length];
            for (int i = 0; i < this.length; ++i)
            {
                text[i] = this.chars.charAt(this.start + i);
            }
            return new String(text);
        }
    }

    private static final int DEFAULT_CHAR_CAPACITY = 256;
    private static final int DEFAULT_FIELD_CAPACITY = 16;

    private char[] chars;
    private int[] starts;
    private int[] lengths;
    private int fieldCount;
    private int charCount;
    private int currentFieldStart;

    public RowBuffer()
    {
        this(DEFAULT_CHAR_CAPACITY, DEFAULT_FIELD_CAPACITY);
    }

    RowBuffer(int charCapacity, int fieldCapacity)
    {
        this.chars = new char[Math.max(1, charCapacity)];
        this.starts = new int[Math.max(1, fieldCapacity)];
        this.lengths = new int[Math.max(1, fieldCapacity)];
        this.fieldCount = 0;
        this.charCount = 0;
        this.currentFieldStart = 0;
    }

    RowBuffer(RowBuffer source)
    {
        this(source.charCount, source.fieldCount);
        System.arraycopy(source.chars, 0, this.chars, 0, source.charCount);
        System.arraycopy(source.starts, 0, this.starts, 0, source.fieldCount);
        System.arraycopy(source.lengths, 0, this.lengths, 0, source.fieldCount);
        this.fieldCount = source.fieldCount;
        this.charCount = source.charCount;
        this.currentFieldStart = source.currentFieldStart;
    }

    public void clear()
    {
        this.fieldCount = 0;
        this.charCount = 0;
        this.currentFieldStart = 0;
    }

    public void append(char ch)
    {
        ensureCharCapacity(this.charCount + 1);
        this.chars[this.charCount++] = ch;
    }

    public void append(char[] source, int offset, int length)
    {
        if (length <= 0)
        {
            return;
        }
        ensureCharCapacity(this.charCount + length);
        System.arraycopy(source, offset, this.chars, this.charCount, length);
        this.charCount += length;
    }

    public void append(CharSequence source)
    {
        if (source == null || source.isEmpty())
        {
            return;
        }
        int length = source.length();
        ensureCharCapacity(this.charCount + length);
        if (source instanceof String string)
        {
            string.getChars(0, length, this.chars, this.charCount);
            this.charCount += length;
            return;
        }
        for (int i = 0; i < length; ++i)
        {
            this.chars[this.charCount + i] = source.charAt(i);
        }
        this.charCount += length;
    }

    public void append(Reader reader) throws IOException
    {
        if (reader == null)
        {
            return;
        }
        while (true)
        {
            ensureCharCapacity(this.charCount + 1);
            int available = this.chars.length - this.charCount;
            int read = reader.read(this.chars, this.charCount, available);
            if (read < 0)
            {
                return;
            }
            if (read == 0)
            {
                ensureCharCapacity(this.chars.length + 1);
                continue;
            }
            this.charCount += read;
        }
    }

    public void finishField()
    {
        ensureFieldCapacity(this.fieldCount + 1);
        this.starts[this.fieldCount] = this.currentFieldStart;
        this.lengths[this.fieldCount] = this.charCount - this.currentFieldStart;
        ++this.fieldCount;
        this.currentFieldStart = this.charCount;
    }

    @Override
    public int size()
    {
        return this.fieldCount;
    }

    @Override
    public boolean isEmpty()
    {
        for (int i = 0; i < this.fieldCount; ++i)
        {
            if (isSet(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSet(int fieldIndex)
    {
        return length(fieldIndex) > 0;
    }

    @Override
    public int length()
    {
        return this.charCount;
    }

    @Override
    public char charAt(int index)
    {
        if (index < 0 || index >= this.charCount)
        {
            throw new IndexOutOfBoundsException();
        }
        return this.chars[index];
    }

    public String getString(int fieldIndex)
    {
        int length = length(fieldIndex);
        if (length == 0)
        {
            return null;
        }
        return new String(this.chars, start(fieldIndex), length);
    }

    @Override
    public RowKey keyOf(int[] positions)
    {
        return RowKey.of(this, positions);
    }

    @Override
    public RowBuffer copy()
    {
        return new RowBuffer(this);
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

    private void ensureCharCapacity(int minCapacity)
    {
        if (minCapacity <= this.chars.length)
        {
            return;
        }
        int newCapacity = Math.max(minCapacity, this.chars.length * 2);
        char[] expanded = new char[newCapacity];
        System.arraycopy(this.chars, 0, expanded, 0, this.charCount);
        this.chars = expanded;
    }

    private void ensureFieldCapacity(int minCapacity)
    {
        if (minCapacity <= this.starts.length)
        {
            return;
        }
        int newCapacity = Math.max(minCapacity, this.starts.length * 2);
        int[] expandedStarts = new int[newCapacity];
        int[] expandedLengths = new int[newCapacity];
        System.arraycopy(this.starts, 0, expandedStarts, 0, this.fieldCount);
        System.arraycopy(this.lengths, 0, expandedLengths, 0, this.fieldCount);
        this.starts = expandedStarts;
        this.lengths = expandedLengths;
    }
}
