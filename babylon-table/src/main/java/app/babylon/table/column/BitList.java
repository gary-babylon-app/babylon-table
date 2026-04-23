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

import java.util.BitSet;

/**
 * A compact immutable sequence of boolean values backed by a bit set.
 */
public interface BitList
{
    /**
     * Creates a mutable bit-list builder.
     *
     * @return new builder
     */
    static Builder builder()
    {
        return new Builder();
    }

    /**
     * Returns the bit value at the supplied index.
     *
     * @param i
     *            zero-based index
     * @return bit value
     */
    boolean get(int i);

    /**
     * Returns the number of bits.
     *
     * @return bit-list size
     */
    int size();

    /**
     * Returns an immutable copy of this bit list.
     *
     * @return copied bit list
     */
    BitList copy();

    /**
     * Mutable builder for {@link BitList}.
     */
    final class Builder
    {
        private BitSet bits;
        private int size;

        /**
         * Creates an empty builder.
         */
        public Builder()
        {
            this.bits = new BitSet();
            this.size = 0;
        }

        /**
         * Appends a bit value.
         *
         * @param value
         *            value to append
         * @return this builder
         */
        public Builder add(boolean value)
        {
            ensureActive();
            if (value)
            {
                this.bits.set(this.size);
            }
            else
            {
                this.bits.clear(this.size);
            }
            ++this.size;
            return this;
        }

        /**
         * Returns the bit currently stored at the supplied index.
         *
         * @param i
         *            zero-based index
         * @return bit value
         */
        public boolean get(int i)
        {
            ensureActive();
            return this.bits.get(i);
        }

        /**
         * Returns the number of appended values.
         *
         * @return builder size
         */
        public int size()
        {
            ensureActive();
            return this.size;
        }

        /**
         * Builds the immutable bit list and transfers ownership.
         *
         * @return immutable bit list
         */
        public BitList build()
        {
            ensureActive();
            return new ArrayBit(this);
        }

        private void ensureActive()
        {
            if (this.bits == null)
            {
                throw new IllegalStateException("BitList.Builder has already transferred ownership.");
            }
        }

        private static final class ArrayBit implements BitList
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
            public BitList copy()
            {
                return new ArrayBit((BitSet) this.bits.clone(), this.size);
            }
        }
    }
}
