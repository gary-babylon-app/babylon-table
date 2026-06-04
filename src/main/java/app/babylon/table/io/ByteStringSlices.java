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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import app.babylon.table.column.Column;
import app.babylon.text.ByteSequence;
import app.babylon.text.ByteString;
import app.babylon.text.Bytes;

public final class ByteStringSlices implements RowValues, ByteSequence, Comparable<ByteStringSlices>
{
    private static final int DEFAULT_BYTE_CAPACITY = 256;
    private static final int DEFAULT_FIELD_CAPACITY = 16;

    private final ByteString byteString;
    private final int[] starts;
    private final int[] ends;

    private ByteStringSlices(Builder builder)
    {
        Objects.requireNonNull(builder, "builder");
        this.starts = new int[builder.fieldCount];
        this.ends = new int[builder.fieldCount];
        System.arraycopy(builder.starts, 0, this.starts, 0, builder.fieldCount);
        System.arraycopy(builder.ends, 0, this.ends, 0, builder.fieldCount);
        int nextCapacity = Math.max(1, builder.bytes.length());
        this.byteString = builder.bytes.build();
        builder.bytes = new ByteString.Builder(nextCapacity, builder.charset);
    }

    private ByteStringSlices(ByteStringSlices source, int[] selectedIndexes, boolean strip)
    {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(selectedIndexes, "selectedIndexes");
        this.byteString = source.byteString;
        this.starts = new int[selectedIndexes.length];
        this.ends = new int[selectedIndexes.length];
        for (int i = 0; i < selectedIndexes.length; ++i)
        {
            int fieldIndex = selectedIndexes[i];
            int start = source.length();
            int end = source.length();
            if (fieldIndex < source.size())
            {
                start = source.start(fieldIndex);
                end = source.end(fieldIndex);
            }
            if (strip)
            {
                start = Bytes.stripxStart(this.byteString, start, end);
                end = Bytes.stripxEnd(this.byteString, start, end);
            }
            this.starts[i] = start;
            this.ends[i] = end;
        }
    }

    public ByteString getByteString()
    {
        return this.byteString;
    }

    public ByteStringSlices select(int[] selectedIndexes)
    {
        return select(selectedIndexes, false);
    }

    @Override
    public ByteStringSlices select(int[] selectedIndexes, boolean strip)
    {
        return new ByteStringSlices(this, selectedIndexes, strip);
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

    public int length(int fieldIndex)
    {
        return end(fieldIndex) - start(fieldIndex);
    }

    @Override
    public int length()
    {
        return this.byteString.length();
    }

    @Override
    public byte byteAt(int index)
    {
        return this.byteString.byteAt(index);
    }

    @Override
    public Charset charset()
    {
        return this.byteString.charset();
    }

    @Override
    public String decode(int start, int end)
    {
        return this.byteString.decode(start, end);
    }

    public String decode(int fieldIndex)
    {
        int start = start(fieldIndex);
        int end = end(fieldIndex);
        return start >= end ? null : decode(start, end);
    }

    public String getString(int fieldIndex)
    {
        return decode(fieldIndex);
    }

    @Override
    public void addTo(Column.Builder builder, int fieldIndex)
    {
        if (!isSet(fieldIndex))
        {
            builder.addNull();
            return;
        }
        builder.add(this.byteString, start(fieldIndex), end(fieldIndex));
    }

    @Override
    public int hashCode()
    {
        int hash = size();
        for (int fieldIndex = 0; fieldIndex < size(); ++fieldIndex)
        {
            int start = start(fieldIndex);
            int end = end(fieldIndex);
            hash = 31 * hash + end - start;
            for (int i = start; i < end; ++i)
            {
                hash = 31 * hash + Byte.toUnsignedInt(byteAt(i));
            }
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
        if (!(obj instanceof RowValues other) || size() != other.size())
        {
            return false;
        }
        if (!(other instanceof ByteStringSlices otherBytes))
        {
            for (int fieldIndex = 0; fieldIndex < size(); ++fieldIndex)
            {
                if (!Objects.equals(getString(fieldIndex), other.getString(fieldIndex)))
                {
                    return false;
                }
            }
            return true;
        }
        for (int fieldIndex = 0; fieldIndex < size(); ++fieldIndex)
        {
            int start = start(fieldIndex);
            int end = end(fieldIndex);
            int otherStart = otherBytes.start(fieldIndex);
            int length = end - start;
            if (length != otherBytes.end(fieldIndex) - otherStart)
            {
                return false;
            }
            for (int i = 0; i < length; ++i)
            {
                if (byteAt(start + i) != otherBytes.byteAt(otherStart + i))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int compareTo(ByteStringSlices other)
    {
        Objects.requireNonNull(other, "other");
        int fieldCount = Math.min(size(), other.size());
        for (int fieldIndex = 0; fieldIndex < fieldCount; ++fieldIndex)
        {
            int comparison = compareField(other, fieldIndex);
            if (comparison != 0)
            {
                return comparison;
            }
        }
        return Integer.compare(size(), other.size());
    }

    private int compareField(ByteStringSlices other, int fieldIndex)
    {
        int start = start(fieldIndex);
        int end = end(fieldIndex);
        int otherStart = other.start(fieldIndex);
        int otherEnd = other.end(fieldIndex);
        int length = Math.min(end - start, otherEnd - otherStart);
        for (int i = 0; i < length; ++i)
        {
            int comparison = Integer.compare(Byte.toUnsignedInt(byteAt(start + i)),
                    Byte.toUnsignedInt(other.byteAt(otherStart + i)));
            if (comparison != 0)
            {
                return comparison;
            }
        }
        return Integer.compare(end - start, otherEnd - otherStart);
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
        private ByteString.Builder bytes;
        private int[] starts;
        private int[] ends;
        private int fieldCount;
        private int currentFieldStart;
        private final Charset charset;

        Builder()
        {
            this(StandardCharsets.UTF_8);
        }

        Builder(Charset charset)
        {
            this(DEFAULT_BYTE_CAPACITY, DEFAULT_FIELD_CAPACITY, charset);
        }

        Builder(int byteCapacity, int fieldCapacity, Charset charset)
        {
            this.charset = Objects.requireNonNull(charset, "charset");
            this.bytes = new ByteString.Builder(Math.max(1, byteCapacity), charset);
            this.starts = new int[Math.max(1, fieldCapacity)];
            this.ends = new int[Math.max(1, fieldCapacity)];
            this.fieldCount = 0;
            this.currentFieldStart = 0;
        }

        Builder(ByteStringSlices row)
        {
            this(byteCount(row), row.size(), row.charset());
            for (int i = 0; i < row.size(); ++i)
            {
                append(row, row.start(i), row.end(i));
                finishField();
            }
        }

        void clear()
        {
            this.bytes.clear();
            this.fieldCount = 0;
            this.currentFieldStart = 0;
        }

        void append(int value)
        {
            this.bytes.append((byte) value);
        }

        void append(byte[] source, int offset, int length)
        {
            if (length <= 0)
            {
                return;
            }
            this.bytes.append(source, offset, length);
        }

        void append(ByteStringSlices row, int start, int end)
        {
            this.bytes.append(row, start, end);
        }

        void append(ByteSequence bytes, int start, int end)
        {
            this.bytes.append(bytes, start, end);
        }

        void finishField()
        {
            ensureFieldCapacity(this.fieldCount + 1);
            this.starts[this.fieldCount] = this.currentFieldStart;
            this.ends[this.fieldCount] = this.bytes.length();
            ++this.fieldCount;
            this.currentFieldStart = this.bytes.length();
        }

        ByteStringSlices build()
        {
            return new ByteStringSlices(this);
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

        private static int byteCount(ByteStringSlices row)
        {
            Objects.requireNonNull(row, "row");
            int byteCount = 0;
            for (int i = 0; i < row.size(); ++i)
            {
                byteCount += row.end(i) - row.start(i);
            }
            return byteCount;
        }
    }
}
