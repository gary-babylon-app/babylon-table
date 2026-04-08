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

import java.util.Arrays;

class ColumnLongBuilderArray implements ColumnLong.Builder
{
    private final ColumnName name;
    private long[] values;
    private ListBit.Builder isSet;
    private int size;
    private boolean hasAnySet;
    private boolean hasAnyUnset;
    private boolean built;

    ColumnLongBuilderArray(ColumnName cn)
    {
        this(cn, 16);
    }

    ColumnLongBuilderArray(ColumnName cn, int initialSize)
    {
        this.name = Objects.requireNonNull(cn);
        this.values = new long[ArgumentChecks.nonNegative(initialSize)];
        this.isSet = ListBit.builder();
        this.size = 0;
        this.hasAnySet = false;
        this.hasAnyUnset = false;
        this.built = false;
    }

    @Override
    public ColumnLong.Builder addNull()
    {
        ensureActive();
        ensureCapacity(size + 1);
        this.values[size] = 0L;
        this.isSet.add(false);
        this.hasAnyUnset = true;
        ++size;
        return this;
    }

    @Override
    public ColumnLong.Builder add(long x)
    {
        ensureActive();
        ensureCapacity(size + 1);
        this.values[size] = x;
        this.isSet.add(true);
        this.hasAnySet = true;
        ++size;
        return this;
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

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    /**
     * Builds an immutable column and transfers ownership of internal storage.
     */
    public ColumnLong build()
    {
        boolean constant = isConstant();
        long[] transferredValues = detachValues();
        ListBit transferredIsSet = detachIsSet();
        return new ColumnLongArray(getName(), transferredValues, transferredIsSet, size, constant, !this.hasAnyUnset,
                !this.hasAnySet);
    }

    private long[] detachValues()
    {
        ensureActive();
        long[] detached = this.values;
        this.values = null;
        return detached;
    }

    private ListBit detachIsSet()
    {
        ensureActive();
        ListBit detached = this.isSet.build();
        this.isSet = null;
        this.built = true;
        return detached;
    }

    private boolean isConstant()
    {
        if (size <= 1)
        {
            return true;
        }
        boolean firstSet = this.isSet.get(0);
        if (!firstSet)
        {
            for (int i = 1; i < size; ++i)
            {
                if (this.isSet.get(i))
                {
                    return false;
                }
            }
            return true;
        }
        long firstValue = this.values[0];
        for (int i = 1; i < size; ++i)
        {
            if (!this.isSet.get(i) || this.values[i] != firstValue)
            {
                return false;
            }
        }
        return true;
    }

    private void ensureActive()
    {
        if (this.built)
        {
            throw new IllegalStateException("Builder has already transferred ownership: " + this.name);
        }
    }

    private static final class ColumnLongArray implements ColumnLong
    {
        private final ColumnName name;
        private final long[] values;
        private final ListBit isSet;
        private final int size;
        private final boolean isConstant;
        private final boolean isAllSet;
        private final boolean isNoneSet;

        private ColumnLongArray(ColumnName name, long[] values, ListBit isSet, int size, boolean isConstant,
                boolean isAllSet, boolean isNoneSet)
        {
            this.name = Objects.requireNonNull(name);
            this.values = Objects.requireNonNull(values);
            this.isSet = Objects.requireNonNull(isSet);
            this.size = ArgumentChecks.nonNegative(size);
            if (size > values.length)
            {
                throw new IllegalArgumentException("Size exceeds values length.");
            }
            this.isConstant = isConstant;
            this.isAllSet = isAllSet;
            this.isNoneSet = isNoneSet;
        }

        @Override
        public ColumnName getName()
        {
            return this.name;
        }

        @Override
        public int size()
        {
            return this.size;
        }

        @Override
        public long get(int i)
        {
            return this.values[i];
        }

        @Override
        public boolean isSet(int i)
        {
            return this.isSet.get(i);
        }

        @Override
        public long[] toArray(long[] x)
        {
            if (x == null || x.length < this.size)
            {
                x = Arrays.copyOf(this.values, this.size);
            } else
            {
                System.arraycopy(this.values, 0, x, 0, this.size);
            }
            return x;
        }

        @Override
        public Column view(ViewIndex rowIndex)
        {
            return new ColumnLongView(this, rowIndex);
        }

        public boolean isConstant()
        {
            return this.isConstant;
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
    }

    private static final class ColumnLongView implements ColumnLong
    {
        private final ColumnLong original;
        private final ViewIndex rowIndex;
        private final boolean isConstant;
        private final boolean isAllSet;
        private final boolean isNoneSet;

        private ColumnLongView(ColumnLong original, ViewIndex rowIndex)
        {
            this.original = Objects.requireNonNull(original);
            this.rowIndex = Objects.requireNonNull(rowIndex);
            if (original.isConstant())
            {
                this.isConstant = true;
            } else
            {
                boolean constant = true;
                for (int i = 1; i < rowIndex.size(); ++i)
                {
                    boolean previousSet = rowIndex.isSet(i - 1) && original.isSet(rowIndex.get(i - 1));
                    boolean currentSet = rowIndex.isSet(i) && original.isSet(rowIndex.get(i));
                    if (previousSet != currentSet)
                    {
                        constant = false;
                        break;
                    }
                    if (currentSet && original.get(rowIndex.get(i - 1)) != original.get(rowIndex.get(i)))
                    {
                        constant = false;
                        break;
                    }
                }
                this.isConstant = constant;
            }
            if (original.isNoneSet() || rowIndex.size() == 0)
            {
                this.isAllSet = false;
                this.isNoneSet = true;
            } else if (original.isAllSet() && rowIndex.isAllSet())
            {
                this.isAllSet = true;
                this.isNoneSet = false;
            } else
            {
                boolean anySet = false;
                boolean anyUnset = false;
                for (int i = 0; i < rowIndex.size(); ++i)
                {
                    boolean set = rowIndex.isSet(i) && original.isSet(rowIndex.get(i));
                    anySet |= set;
                    anyUnset |= !set;
                    if (anySet && anyUnset)
                    {
                        break;
                    }
                }
                this.isAllSet = !anyUnset;
                this.isNoneSet = !anySet;
            }
        }

        @Override
        public ColumnName getName()
        {
            return this.original.getName();
        }

        @Override
        public int size()
        {
            return this.rowIndex.size();
        }

        @Override
        public long get(int i)
        {
            return isSet(i) ? this.original.get(this.rowIndex.get(i)) : 0L;
        }

        @Override
        public boolean isSet(int i)
        {
            return this.rowIndex.isSet(i) && this.original.isSet(this.rowIndex.get(i));
        }

        @Override
        public long[] toArray(long[] x)
        {
            if (x == null || x.length < size())
            {
                x = new long[size()];
            }
            for (int i = 0; i < size(); ++i)
            {
                x[i] = get(i);
            }
            return x;
        }

        @Override
        public Column view(ViewIndex rowIndex)
        {
            return new ColumnLongView(this, rowIndex);
        }

        @Override
        public boolean isConstant()
        {
            return this.isConstant;
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
    }
}
