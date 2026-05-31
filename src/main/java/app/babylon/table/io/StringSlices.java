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

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import app.babylon.text.BigDecimals;
import app.babylon.text.Strings;

final class StringSlices implements CharSequence
{
    private static final int DEFAULT_CHAR_CAPACITY = 256;
    private static final int DEFAULT_FIELD_CAPACITY = 16;

    private final String chars;
    private final int[] starts;
    private final int[] ends;

    private StringSlices(Builder builder)
    {
        Objects.requireNonNull(builder, "builder");
        this.chars = builder.chars.toString();
        this.starts = new int[builder.fieldCount];
        this.ends = new int[builder.fieldCount];
        System.arraycopy(builder.starts, 0, this.starts, 0, builder.fieldCount);
        System.arraycopy(builder.ends, 0, this.ends, 0, builder.fieldCount);
        int nextCapacity = Math.max(1, builder.chars.length());
        builder.chars = new StringBuilder(nextCapacity);
        builder.fieldCount = 0;
        builder.currentFieldStart = 0;
    }

    private StringSlices(StringSlices source, int[] selectedIndexes, boolean strip)
    {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(selectedIndexes, "selectedIndexes");
        this.chars = source.chars;
        this.starts = new int[selectedIndexes.length];
        this.ends = new int[selectedIndexes.length];
        for (int i = 0; i < selectedIndexes.length; ++i)
        {
            int fieldIndex = selectedIndexes[i];
            int start = source.start(fieldIndex);
            int end = source.end(fieldIndex);
            if (strip)
            {
                start = Strings.stripxStart(this.chars, start, end);
                end = Strings.stripxEnd(this.chars, start, end);
            }
            this.starts[i] = start;
            this.ends[i] = end;
        }
    }

    String getString()
    {
        return this.chars;
    }

    StringSlices select(int[] selectedIndexes, boolean strip)
    {
        return new StringSlices(this, selectedIndexes, strip);
    }

    ByteStringSlices toByteStringSlices()
    {
        return toByteStringSlices(StandardCharsets.UTF_8);
    }

    ByteStringSlices toByteStringSlices(Charset charset)
    {
        Charset resolvedCharset = Objects.requireNonNull(charset, "charset");
        ByteStringSlices.Builder builder = new ByteStringSlices.Builder(this.chars.length(), this.starts.length,
                resolvedCharset);
        for (int i = 0; i < this.starts.length; ++i)
        {
            String value = getString(i);
            if (value != null)
            {
                byte[] bytes = value.getBytes(resolvedCharset);
                builder.append(bytes, 0, bytes.length);
            }
            builder.finishField();
        }
        return builder.build();
    }

    public int size()
    {
        return this.starts.length;
    }

    public boolean isEmpty()
    {
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                return false;
            }
        }
        return true;
    }

    public boolean isSet(int fieldIndex)
    {
        return end(fieldIndex) > start(fieldIndex);
    }

    public int start(int fieldIndex)
    {
        checkFieldIndex(fieldIndex);
        return this.starts[fieldIndex];
    }

    public int end(int fieldIndex)
    {
        checkFieldIndex(fieldIndex);
        return this.ends[fieldIndex];
    }

    @Override
    public int length()
    {
        return this.chars.length();
    }

    @Override
    public char charAt(int index)
    {
        return this.chars.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return this.chars.subSequence(start, end);
    }

    public String getString(int fieldIndex)
    {
        int start = start(fieldIndex);
        int end = end(fieldIndex);
        return start >= end ? null : this.chars.substring(start, end);
    }

    BigDecimal parseDecimal(int start, int end)
    {
        return BigDecimals.parse(this.chars, start, end);
    }

    public StringSlices copy()
    {
        Builder builder = new Builder(this.chars.length(), this.starts.length);
        for (int i = 0; i < this.starts.length; ++i)
        {
            builder.append(this.chars, this.starts[i], this.ends[i]);
            builder.finishField();
        }
        return builder.build();
    }

    private void checkFieldIndex(int fieldIndex)
    {
        if (fieldIndex < 0 || fieldIndex >= size())
        {
            throw new IndexOutOfBoundsException();
        }
    }

    static final class Builder
    {
        private StringBuilder chars;
        private int[] starts;
        private int[] ends;
        private int fieldCount;
        private int currentFieldStart;

        Builder()
        {
            this(DEFAULT_CHAR_CAPACITY, DEFAULT_FIELD_CAPACITY);
        }

        Builder(int charCapacity, int fieldCapacity)
        {
            this.chars = new StringBuilder(Math.max(1, charCapacity));
            this.starts = new int[Math.max(1, fieldCapacity)];
            this.ends = new int[Math.max(1, fieldCapacity)];
            this.fieldCount = 0;
            this.currentFieldStart = 0;
        }

        void clear()
        {
            this.chars.setLength(0);
            this.fieldCount = 0;
            this.currentFieldStart = 0;
        }

        void append(char value)
        {
            this.chars.append(value);
        }

        void append(char[] source, int offset, int length)
        {
            if (length <= 0)
            {
                return;
            }
            this.chars.append(source, offset, length);
        }

        void append(CharSequence source)
        {
            if (source != null)
            {
                this.chars.append(source);
            }
        }

        void append(CharSequence source, int start, int end)
        {
            if (source != null && start < end)
            {
                this.chars.append(source, start, end);
            }
        }

        void append(Reader reader) throws java.io.IOException
        {
            if (reader == null)
            {
                return;
            }
            char[] buffer = new char[1024];
            while (true)
            {
                int read = reader.read(buffer);
                if (read < 0)
                {
                    return;
                }
                append(buffer, 0, read);
            }
        }

        void finishField()
        {
            ensureFieldCapacity(this.fieldCount + 1);
            this.starts[this.fieldCount] = this.currentFieldStart;
            this.ends[this.fieldCount] = this.chars.length();
            ++this.fieldCount;
            this.currentFieldStart = this.chars.length();
        }

        StringSlices build()
        {
            return new StringSlices(this);
        }

        private void ensureFieldCapacity(int requiredCapacity)
        {
            if (requiredCapacity <= this.starts.length)
            {
                return;
            }
            int capacity = this.starts.length;
            while (capacity < requiredCapacity)
            {
                capacity = Math.max(capacity * 2, requiredCapacity);
            }
            int[] newStarts = new int[capacity];
            int[] newEnds = new int[capacity];
            System.arraycopy(this.starts, 0, newStarts, 0, this.fieldCount);
            System.arraycopy(this.ends, 0, newEnds, 0, this.fieldCount);
            this.starts = newStarts;
            this.ends = newEnds;
        }

    }
}
