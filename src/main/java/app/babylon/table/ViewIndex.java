/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

import app.babylon.lang.ArgumentCheck;

import java.util.Arrays;

/**
 * Maps logical row positions in a view to row positions in the underlying data,
 * including optional unset entries.
 */
public interface ViewIndex
{
    int BYTE_MAX_VALUE = 0xFF;
    int CHAR_MAX_VALUE = 0xFFFF;
    int BYTE_NULL = BYTE_MAX_VALUE;
    int CHAR_NULL = CHAR_MAX_VALUE;
    int INT_NULL = Integer.MAX_VALUE;

    static Builder builder()
    {
        return new BuilderInt();
    }

    static ViewIndex of(ViewIndex viewIndex)
    {
        ArgumentCheck.nonNull(viewIndex);
        return viewIndex.copy();
    }

    int size();

    boolean isAllSet();

    boolean isSet(int i);

    int get(int i);

    int[] toArray(int[] x);

    ViewIndex copy();

    interface Builder
    {
        Builder add(int x);

        Builder addNull();

        Builder addAll(int[] x);

        int size();

        boolean isSet(int i);

        int get(int i);

        ViewIndex build();
    }

    final class BuilderInt implements Builder
    {
        private int[] values;
        private int size;
        private int maxValue;
        private boolean hasNulls;
        private boolean built;

        private BuilderInt()
        {
            this.values = new int[16];
            this.size = 0;
            this.maxValue = -1;
            this.hasNulls = false;
            this.built = false;
        }

        @Override
        public Builder add(int x)
        {
            ensureActive();
            validate(x);
            ensureCapacity(this.size + 1);
            this.values[this.size] = x;
            ++this.size;
            if (x > this.maxValue)
            {
                this.maxValue = x;
            }
            return this;
        }

        @Override
        public Builder addNull()
        {
            ensureActive();
            ensureCapacity(this.size + 1);
            this.values[this.size] = INT_NULL;
            ++this.size;
            this.hasNulls = true;
            return this;
        }

        @Override
        public Builder addAll(int[] x)
        {
            ensureActive();
            if (x == null || x.length == 0)
            {
                return this;
            }
            ensureCapacity(this.size + x.length);
            for (int value : x)
            {
                validate(value);
                this.values[this.size] = value;
                ++this.size;
                if (value > this.maxValue)
                {
                    this.maxValue = value;
                }
            }
            return this;
        }

        @Override
        public int size()
        {
            ensureActive();
            return this.size;
        }

        @Override
        public boolean isSet(int i)
        {
            ensureActive();
            checkBounds(i);
            return this.values[i] != INT_NULL;
        }

        @Override
        public int get(int i)
        {
            ensureActive();
            checkBounds(i);
            return this.values[i];
        }

        @Override
        public ViewIndex build()
        {
            ensureActive();
            ViewIndex out;
            if (this.maxValue <= BYTE_MAX_VALUE - 1)
            {
                out = new ArrayByte(this.values, this.size);
            } else if (this.maxValue <= CHAR_MAX_VALUE - 1)
            {
                out = new ArrayChar(this.values, this.size, this.hasNulls);
            } else
            {
                out = new ArrayInt(this.values, this.size, this.hasNulls);
            }
            this.values = null;
            this.size = 0;
            this.maxValue = -1;
            this.hasNulls = false;
            this.built = true;
            return out;
        }

        private void validate(int x)
        {
            ArgumentCheck.nonNegative(x);
            if (x == INT_NULL)
            {
                throw new IllegalArgumentException("Ordinal value reserved for null sentinel.");
            }
        }

        private void ensureCapacity(int requiredSize)
        {
            if (requiredSize <= this.values.length)
            {
                return;
            }
            int newSize = this.values.length + (this.values.length >>> 1) + 16;
            if (newSize < requiredSize)
            {
                newSize = requiredSize;
            }
            this.values = Arrays.copyOf(this.values, newSize);
        }

        private void ensureActive()
        {
            if (this.built)
            {
                throw new IllegalStateException("Builder has already built ViewIndex.");
            }
        }

        private void checkBounds(int i)
        {
            if (i < 0 || i >= this.size)
            {
                throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
            }
        }
    }

    final class ArrayByte implements ViewIndex
    {
        private final byte[] values;
        private final int size;
        private final boolean hasNulls;

        private ArrayByte(int[] values, int size)
        {
            this.size = ArgumentCheck.nonNegative(size);
            this.values = new byte[size];
            boolean hasNulls = false;
            for (int i = 0; i < size; ++i)
            {
                if (values[i] == INT_NULL)
                {
                    hasNulls = true;
                }
                this.values[i] = (byte) (values[i] == INT_NULL ? BYTE_NULL : values[i]);
            }
            this.hasNulls = hasNulls;
        }

        private ArrayByte(ArrayByte other)
        {
            this.size = other.size;
            this.values = Arrays.copyOf(other.values, other.size);
            this.hasNulls = other.hasNulls;
        }

        @Override
        public int size()
        {
            return this.size;
        }

        @Override
        public boolean isAllSet()
        {
            return !this.hasNulls;
        }

        @Override
        public boolean isSet(int i)
        {
            checkBounds(i);
            return (this.values[i] & BYTE_MAX_VALUE) != BYTE_NULL;
        }

        @Override
        public int get(int i)
        {
            checkBounds(i);
            return this.values[i] & BYTE_MAX_VALUE;
        }

        @Override
        public int[] toArray(int[] x)
        {
            if (x == null || x.length < this.size)
            {
                x = new int[this.size];
            }
            for (int i = 0; i < this.size; ++i)
            {
                x[i] = this.values[i] & BYTE_MAX_VALUE;
            }
            return x;
        }

        @Override
        public ViewIndex copy()
        {
            return new ArrayByte(this);
        }

        private void checkBounds(int i)
        {
            if (i < 0 || i >= this.size)
            {
                throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
            }
        }
    }

    final class ArrayChar implements ViewIndex
    {
        private final char[] values;
        private final int size;
        private final boolean hasNulls;

        private ArrayChar(int[] values, int size, boolean hasNulls)
        {
            this.size = ArgumentCheck.nonNegative(size);
            this.values = new char[size];
            this.hasNulls = hasNulls;
            for (int i = 0; i < size; ++i)
            {
                this.values[i] = (char) (values[i] == INT_NULL ? CHAR_NULL : values[i]);
            }
        }

        private ArrayChar(ArrayChar other)
        {
            this.size = other.size;
            this.values = Arrays.copyOf(other.values, other.size);
            this.hasNulls = other.hasNulls;
        }

        @Override
        public int size()
        {
            return this.size;
        }

        @Override
        public boolean isAllSet()
        {
            return !this.hasNulls;
        }

        @Override
        public boolean isSet(int i)
        {
            checkBounds(i);
            return this.values[i] != CHAR_NULL;
        }

        @Override
        public int get(int i)
        {
            checkBounds(i);
            return this.values[i];
        }

        @Override
        public int[] toArray(int[] x)
        {
            if (x == null || x.length < this.size)
            {
                x = new int[this.size];
            }
            for (int i = 0; i < this.size; ++i)
            {
                x[i] = this.values[i];
            }
            return x;
        }

        @Override
        public ViewIndex copy()
        {
            return new ArrayChar(this);
        }

        private void checkBounds(int i)
        {
            if (i < 0 || i >= this.size)
            {
                throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
            }
        }
    }

    final class ArrayInt implements ViewIndex
    {
        private final int[] values;
        private final boolean hasNulls;

        private ArrayInt(int[] values, int size, boolean hasNulls)
        {
            this.values = new int[size];
            this.hasNulls = hasNulls;
            for (int i = 0; i < size; ++i)
            {
                this.values[i] = values[i];
            }
        }

        private ArrayInt(ArrayInt other)
        {
            this.values = Arrays.copyOf(other.values, other.values.length);
            this.hasNulls = other.hasNulls;
        }

        @Override
        public int size()
        {
            return this.values.length;
        }

        @Override
        public boolean isAllSet()
        {
            return !this.hasNulls;
        }

        @Override
        public boolean isSet(int i)
        {
            checkBounds(i);
            return this.values[i] != INT_NULL;
        }

        @Override
        public int get(int i)
        {
            checkBounds(i);
            return this.values[i];
        }

        @Override
        public int[] toArray(int[] x)
        {
            if (x == null || x.length < this.values.length)
            {
                return Arrays.copyOf(this.values, this.values.length);
            }
            System.arraycopy(this.values, 0, x, 0, this.values.length);
            return x;
        }

        @Override
        public ViewIndex copy()
        {
            return new ArrayInt(this);
        }

        private void checkBounds(int i)
        {
            if (i < 0 || i >= this.values.length)
            {
                throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.values.length);
            }
        }
    }
}
