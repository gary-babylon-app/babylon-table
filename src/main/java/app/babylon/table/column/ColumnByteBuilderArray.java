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

import app.babylon.lang.ArgumentCheck;

import java.util.Arrays;

import app.babylon.table.ViewIndex;

class ColumnByteBuilderArray implements ColumnByte.Builder
{
    private final ColumnName name;
    private byte[] bytes;
    private BitList.Builder isSet;
    private int size;
    private boolean hasAnySet;
    private boolean hasAnyUnset;
    private boolean built;

    ColumnByteBuilderArray(ColumnName name)
    {
        this(name, 255);
    }

    ColumnByteBuilderArray(ColumnName name, int initialCapacity)
    {
        this.name = ArgumentCheck.nonNull(name);
        this.bytes = new byte[ArgumentCheck.nonNegative(initialCapacity)];
        this.isSet = BitList.builder();
        this.size = 0;
        this.hasAnySet = false;
        this.hasAnyUnset = false;
        this.built = false;
    }

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    @Override
    public ColumnByte.Builder add(byte x)
    {
        ensureActive();
        ensureCapacity(this.size + 1);
        this.bytes[this.size] = x;
        this.isSet.add(true);
        this.hasAnySet = true;
        ++this.size;
        return this;
    }

    @Override
    public ColumnByte.Builder addNull()
    {
        ensureActive();
        ensureCapacity(this.size + 1);
        this.bytes[this.size] = 0;
        this.isSet.add(false);
        this.hasAnyUnset = true;
        ++this.size;
        return this;
    }

    @Override
    public ColumnByte build()
    {
        ensureActive();
        boolean constant = isConstant();
        boolean allSet = !this.hasAnyUnset;
        byte constantValue = this.size == 0 ? (byte) 0 : this.bytes[0];
        ColumnName columnName = this.name;
        byte[] transferredBytes = this.bytes;
        BitList transferredIsSet = this.isSet.build();
        int transferredSize = this.size;
        this.bytes = null;
        this.isSet = null;
        this.size = 0;
        this.built = true;
        if (constant && allSet)
        {
            return new ColumnByteConstant(columnName, constantValue, transferredSize);
        }
        return new ColumnByteStream(columnName, transferredBytes, transferredIsSet, transferredSize, allSet,
                !this.hasAnySet);
    }

    private boolean isConstant()
    {
        if (this.size <= 1)
        {
            return true;
        }
        boolean firstSet = this.isSet.get(0);
        if (!firstSet)
        {
            for (int i = 1; i < this.size; ++i)
            {
                if (this.isSet.get(i))
                {
                    return false;
                }
            }
            return true;
        }
        byte firstValue = this.bytes[0];
        for (int i = 1; i < this.size; ++i)
        {
            if (!this.isSet.get(i) || this.bytes[i] != firstValue)
            {
                return false;
            }
        }
        return true;
    }

    private void ensureCapacity(int minCapacity)
    {
        if (minCapacity > this.bytes.length)
        {
            int newCapacity = this.bytes.length + (this.bytes.length >>> 1) + 16;
            if (newCapacity < minCapacity)
            {
                newCapacity = minCapacity;
            }
            this.bytes = Arrays.copyOf(this.bytes, newCapacity);
        }
    }

    private void ensureActive()
    {
        if (this.built)
        {
            throw new IllegalStateException("Builder has already transferred ownership: " + this.name);
        }
    }

    private static final class ColumnByteStream extends java.io.OutputStream implements ColumnByte
    {
        private final ColumnName name;
        private final byte[] bytes;
        private final BitList isSet;
        private final int size;
        private final boolean isAllSet;
        private final boolean isNoneSet;

        private ColumnByteStream(ColumnName name, byte[] bytes, BitList isSet, int size, boolean isAllSet,
                boolean isNoneSet)
        {
            this.name = ArgumentCheck.nonNull(name);
            this.bytes = ArgumentCheck.nonNull(bytes);
            this.isSet = ArgumentCheck.nonNull(isSet);
            this.size = ArgumentCheck.nonNegative(size);
            if (size > bytes.length)
            {
                throw new IllegalArgumentException("Size exceeds byte array length.");
            }
            this.isAllSet = isAllSet;
            this.isNoneSet = isNoneSet;
        }

        @Override
        public ColumnByte copy(ColumnName x)
        {
            ColumnByte.Builder builder = ColumnByte.builder(x);
            for (int i = 0; i < this.size; ++i)
            {
                if (this.isSet.get(i))
                {
                    builder.add(this.bytes[i]);
                }
                else
                {
                    builder.addNull();
                }
            }
            return builder.build();
        }
        @Override
        public Type getType()
        {
            return ColumnByte.TYPE;
        }

        @Override
        public int size()
        {
            return this.size;
        }

        @Override
        public ColumnName getName()
        {
            return this.name;
        }

        @Override
        public String toString(int i)
        {
            return isSet(i) ? Byte.toString(get(i)) : "";
        }

        @Override
        public int compare(int i, int j)
        {
            if (i == j)
            {
                return 0;
            }
            boolean aSet = isSet(i);
            boolean bSet = isSet(j);
            if (aSet && bSet)
            {
                return Byte.compare(get(i), get(j));
            }
            if (!aSet && !bSet)
            {
                return 0;
            }
            return aSet ? 1 : -1;
        }

        @Override
        public byte get(int i)
        {
            return this.bytes[i];
        }

        @Override
        public boolean isSet(int i)
        {
            return this.isSet.get(i);
        }

        @Override
        public boolean isAllSet()
        {
            return this.isAllSet;
        }

        @Override
        public boolean isNoneSet()
        {
            return this.isNoneSet;
        }

        @Override
        public void write(int b)
        {
            throw new UnsupportedOperationException("ColumnByteStream is immutable.");
        }

        @Override
        public boolean isXls()
        {
            return size >= 8 && (this.bytes[0] & 0xFF) == 0xD0 && (this.bytes[1] & 0xFF) == 0xCF
                    && (this.bytes[2] & 0xFF) == 0x11 && (this.bytes[3] & 0xFF) == 0xE0
                    && (this.bytes[4] & 0xFF) == 0xA1 && (this.bytes[5] & 0xFF) == 0xB1
                    && (this.bytes[6] & 0xFF) == 0x1A && (this.bytes[7] & 0xFF) == 0xE1;
        }

        private boolean isZip()
        {
            return size >= 4 && this.bytes[0] == 0x50 && this.bytes[1] == 0x4B && this.bytes[2] == 0x03
                    && this.bytes[3] == 0x04;
        }

        @Override
        public boolean isXlsx()
        {
            return isZip() && !this.name.getCanonical().endsWith("zip");
        }

        @Override
        public Column view(ViewIndex rowIndex)
        {
            ArgumentCheck.nonNull(rowIndex);
            ColumnByte.Builder builder = ColumnByte.builder(this.name);
            for (int i = 0; i < rowIndex.size(); ++i)
            {
                if (rowIndex.isSet(i) && this.isSet.get(rowIndex.get(i)))
                {
                    builder.add(this.bytes[rowIndex.get(i)]);
                }
                else
                {
                    builder.addNull();
                }
            }
            return builder.build();
        }

        @Override
        public Column getAsColumn(int i)
        {
            return this.isSet.get(i)
                    ? new ColumnByteConstant(this.name, this.bytes[i], 1, true)
                    : ColumnByteConstant.createNull(this.name, 1);
        }
    }
}
