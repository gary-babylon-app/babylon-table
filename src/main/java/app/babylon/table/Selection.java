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

import java.util.Objects;

import java.util.BitSet;


public class Selection
{
    private BitSet bits;
    private int size;
    private final String name;

    public Selection(String name)
    {
        this.name = ArgumentChecks.nonEmpty(name);
        bits = new BitSet(255);
        size=0;
    }

    /**
     *
     * @param name
     * @param bitSet - careful this is not copied, so selection is mutable.
     * @param size
     */
    private Selection(String name, BitSet bitSet, int size)
    {
        this.name = ArgumentChecks.nonEmpty(name);
        BitSet source = Objects.requireNonNull(bitSet);
        if (size < 0)
        {
            throw new IllegalArgumentException("size must be >= 0");
        }
        if (source.length() > size)
        {
            throw new IllegalArgumentException("BitSet has selected indexes beyond declared size: " + size);
        }
        this.bits = BitSet.valueOf(source.toLongArray());
        this.size = size;
    }
    public String getName()
    {
        return name;
    }

    public void add(boolean b)
    {
        bits.set(size, b);
        ++size;
    }

    public int size()
    {
        return this.size;
    }

    public boolean get(int i)
    {
        return bits.get(i);
    }

    public int selected()
    {
        return this.bits.cardinality();
    }

    public Selection not()
    {
        //unnesscart copy of copied array, find better way to deep copy;
        BitSet bitset = BitSet.valueOf(this.bits.toLongArray());
        bitset.flip(0, size);

        return new Selection(this.name, bitset, size);

    }

    public Selection and(Selection x)
    {
        requireCompatible(x);
        BitSet bitset = BitSet.valueOf(this.bits.toLongArray());
        bitset.and(x.bits);
        return new Selection("(" + this.name + " AND " + x.name + ")", bitset, this.size);
    }

    public Selection or(Selection x)
    {
        requireCompatible(x);
        BitSet bitset = BitSet.valueOf(this.bits.toLongArray());
        bitset.or(x.bits);
        return new Selection("(" + this.name + " OR " + x.name + ")", bitset, this.size);
    }

    private void requireCompatible(Selection x)
    {
        Objects.requireNonNull(x);
        if (this.size != x.size)
        {
            throw new IllegalArgumentException("Selection sizes differ: " + this.size + " vs " + x.size);
        }
    }
}
