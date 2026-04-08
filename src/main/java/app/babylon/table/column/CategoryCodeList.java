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

import java.util.Arrays;

import app.babylon.lang.ArgumentCheck;

/**
 * Stores category codes as a compact immutable integer sequence for
 * dictionary-encoded columns.
 */
public interface CategoryCodeList
{
    int BYTE_MAX_VALUE = 0xFF;
    int CHAR_MAX_VALUE = 0xFFFF;

    static Builder builder()
    {
        return new BuilderInt();
    }

    int size();

    int get(int i);

    int[] toArray(int[] x);

    CategoryCodeList copy();

    interface Builder
    {
        Builder add(int x);

        Builder addAll(int[] x);

        int size();

        int get(int i);

        CategoryCodeList build();
    }

    final class BuilderInt implements Builder
    {
        private int[] values;
        private int size;
        private int maxValue;
        private boolean built;

        private BuilderInt()
        {
            this.values = new int[16];
            this.size = 0;
            this.maxValue = -1;
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
        public Builder addAll(int[] x)
        {
            ensureActive();
            if (x == null || x.length == 0)
            {
                return this;
            }
            ensureCapacity(this.size + x.length);
            for (int i = 0; i < x.length; ++i)
            {
                int value = x[i];
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
        public int get(int i)
        {
            ensureActive();
            if (i < 0 || i >= this.size)
            {
                throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
            }
            return this.values[i];
        }

        @Override
        public CategoryCodeList build()
        {
            ensureActive();
            CategoryCodeList out;
            if (this.maxValue <= BYTE_MAX_VALUE)
            {
                out = new ArrayByte(this.values, this.size);
            } else if (this.maxValue <= CHAR_MAX_VALUE)
            {
                out = new ArrayChar(this.values, this.size);
            } else
            {
                out = new ArrayInt(this.values, this.size);
            }
            this.values = null;
            this.size = 0;
            this.maxValue = -1;
            this.built = true;
            return out;
        }

        private void validate(int x)
        {
            ArgumentCheck.nonNegative(x);
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
                throw new IllegalStateException("Builder has already built CategoryCodeList.");
            }
        }

        private static final class ArrayByte implements CategoryCodeList
        {
            private final byte[] values;
            private final int size;

            private ArrayByte(int[] values, int size)
            {
                app.babylon.lang.ArgumentCheck.nonNull(values);
                this.size = ArgumentCheck.nonNegative(size);
                if (this.size > values.length)
                {
                    throw new IllegalArgumentException("Size exceeds backing array length.");
                }
                this.values = new byte[size];
                for (int i = 0; i < size; ++i)
                {
                    this.values[i] = (byte) values[i];
                }
            }

            private ArrayByte(ArrayByte other)
            {
                this.size = other.size;
                this.values = Arrays.copyOf(other.values, other.size);
            }

            @Override
            public int size()
            {
                return this.size;
            }

            @Override
            public int get(int i)
            {
                if (i < 0 || i >= this.size)
                {
                    throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
                }
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
            public CategoryCodeList copy()
            {
                return new ArrayByte(this);
            }
        }

        private static final class ArrayChar implements CategoryCodeList
        {
            private final char[] values;
            private final int size;

            private ArrayChar(int[] values, int size)
            {
                app.babylon.lang.ArgumentCheck.nonNull(values);
                this.size = ArgumentCheck.nonNegative(size);
                if (this.size > values.length)
                {
                    throw new IllegalArgumentException("Size exceeds backing array length.");
                }
                this.values = new char[size];
                for (int i = 0; i < size; ++i)
                {
                    this.values[i] = (char) values[i];
                }
            }

            private ArrayChar(ArrayChar other)
            {
                this.size = other.size;
                this.values = Arrays.copyOf(other.values, other.size);
            }

            @Override
            public int size()
            {
                return this.size;
            }

            @Override
            public int get(int i)
            {
                if (i < 0 || i >= this.size)
                {
                    throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
                }
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
            public CategoryCodeList copy()
            {
                return new ArrayChar(this);
            }
        }

        private static final class ArrayInt implements CategoryCodeList
        {
            private final int[] values;
            private final int size;

            private ArrayInt(int[] values, int size)
            {
                app.babylon.lang.ArgumentCheck.nonNull(values);
                this.size = ArgumentCheck.nonNegative(size);
                if (this.size > values.length)
                {
                    throw new IllegalArgumentException("Size exceeds backing array length.");
                }
                this.values = Arrays.copyOf(values, size);
            }

            private ArrayInt(ArrayInt other)
            {
                this.size = other.size;
                this.values = Arrays.copyOf(other.values, other.size);
            }

            @Override
            public int size()
            {
                return this.size;
            }

            @Override
            public int get(int i)
            {
                if (i < 0 || i >= this.size)
                {
                    throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
                }
                return this.values[i];
            }

            @Override
            public int[] toArray(int[] x)
            {
                if (x == null || x.length < this.size)
                {
                    return Arrays.copyOf(this.values, this.size);
                }
                System.arraycopy(this.values, 0, x, 0, this.size);
                return x;
            }

            @Override
            public CategoryCodeList copy()
            {
                return new ArrayInt(this);
            }
        }
    }
}
