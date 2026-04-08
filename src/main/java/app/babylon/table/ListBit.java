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

import java.util.BitSet;

/**
 * A compact immutable sequence of boolean values backed by a bit set.
 */
public interface ListBit
{
    static Builder builder()
    {
        return new Builder();
    }

    boolean get(int i);

    int size();

    ListBit copy();

    final class Builder
    {
        private BitSet bits;
        private int size;

        public Builder()
        {
            this.bits = new BitSet();
            this.size = 0;
        }

        public Builder add(boolean value)
        {
            ensureActive();
            if (value)
            {
                this.bits.set(this.size);
            } else
            {
                this.bits.clear(this.size);
            }
            ++this.size;
            return this;
        }

        public boolean get(int i)
        {
            ensureActive();
            return this.bits.get(i);
        }

        public int size()
        {
            ensureActive();
            return this.size;
        }

        public ListBit build()
        {
            ensureActive();
            return new ArrayBit(this);
        }

        private void ensureActive()
        {
            if (this.bits == null)
            {
                throw new IllegalStateException("ListBit.Builder has already transferred ownership.");
            }
        }

        private static final class ArrayBit implements ListBit
        {
            private final BitSet bits;
            private final int size;

            private ArrayBit(Builder builder)
            {
                this.size = builder.size();
                this.bits = (BitSet) builder.bits.clone();
                builder.bits = null;
                builder.size = 0;
            }

            private ArrayBit(BitSet bits, int size)
            {
                this.bits = bits;
                this.size = size;
            }

            @Override
            public boolean get(int i)
            {
                if (i < 0 || i >= this.size)
                {
                    throw new IndexOutOfBoundsException("Index out of bounds: " + i + ", size=" + this.size);
                }
                return this.bits.get(i);
            }

            @Override
            public int size()
            {
                return this.size;
            }

            @Override
            public ListBit copy()
            {
                return new ArrayBit((BitSet) this.bits.clone(), this.size);
            }
        }
    }
}
