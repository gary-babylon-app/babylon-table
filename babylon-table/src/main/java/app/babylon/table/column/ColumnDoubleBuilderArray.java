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

class ColumnDoubleBuilderArray implements ColumnDouble.Builder
{
    private final ColumnName name;
    private double[] values;
    private BitList.Builder isSet;
    private int size;
    private boolean hasAnySet;
    private boolean hasAnyUnset;
    private boolean built;

    ColumnDoubleBuilderArray(ColumnName cn)
    {
        this(cn, 16);
    }

    ColumnDoubleBuilderArray(ColumnName cn, int initialSize)
    {
        this.name = ArgumentCheck.nonNull(cn);
        this.values = new double[ArgumentCheck.nonNegative(initialSize)];
        this.isSet = BitList.builder();
        this.size = 0;
        this.hasAnySet = false;
        this.hasAnyUnset = false;
        this.built = false;
    }

    @Override
    public ColumnDouble.Builder addNull()
    {
        ensureActive();
        ensureCapacity(size + 1);
        this.values[size] = 0.0;
        this.isSet.add(false);
        this.hasAnyUnset = true;
        ++size;
        return this;
    }

    @Override
    public ColumnDouble.Builder add(double x)
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
    public ColumnDouble build()
    {
        boolean constant = isConstant();
        double[] transferredValues = detachValues();
        BitList transferredIsSet = detachIsSet();
        return new ColumnDoubleArray(getName(), transferredValues, transferredIsSet, size, constant, !this.hasAnyUnset,
                !this.hasAnySet);
    }

    private double[] detachValues()
    {
        ensureActive();
        double[] detached = this.values;
        this.values = null;
        return detached;
    }

    private BitList detachIsSet()
    {
        ensureActive();
        BitList detached = this.isSet.build();
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
        double firstValue = this.values[0];
        for (int i = 1; i < size; ++i)
        {
            if (!this.isSet.get(i) || Double.compare(this.values[i], firstValue) != 0)
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

    private static final class ColumnDoubleArray implements ColumnDouble
    {
        private final ColumnName name;
        private final double[] values;
        private final BitList isSet;
        private final int size;
        private final boolean isConstant;
        private final boolean isAllSet;
        private final boolean isNoneSet;

        private ColumnDoubleArray(ColumnName name, double[] values, BitList isSet, int size, boolean isConstant,
                boolean isAllSet, boolean isNoneSet)
        {
            this.name = ArgumentCheck.nonNull(name);
            this.values = ArgumentCheck.nonNull(values);
            this.isSet = ArgumentCheck.nonNull(isSet);
            this.size = ArgumentCheck.nonNegative(size);
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
        public double get(int i)
        {
            return this.values[i];
        }

        @Override
        public boolean isSet(int i)
        {
            return this.isSet.get(i);
        }

        @Override
        public double[] toArray(double[] x)
        {
            if (x == null || x.length < this.size)
            {
                x = Arrays.copyOf(this.values, this.size);
            }
            else
            {
                System.arraycopy(this.values, 0, x, 0, this.size);
            }
            return x;
        }

        @Override
        public Column view(ViewIndex rowIndex)
        {
            return new ColumnDoubleView(this, rowIndex);
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

    private static final class ColumnDoubleView implements ColumnDouble
    {
        private final ColumnDouble original;
        private final ViewIndex rowIndex;
        private final boolean isConstant;
        private final boolean isAllSet;
        private final boolean isNoneSet;

        private ColumnDoubleView(ColumnDouble original, ViewIndex rowIndex)
        {
            this.original = ArgumentCheck.nonNull(original);
            this.rowIndex = ArgumentCheck.nonNull(rowIndex);
            if (original.isConstant())
            {
                this.isConstant = true;
            }
            else
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
                    if (currentSet
                            && Double.compare(original.get(rowIndex.get(i - 1)), original.get(rowIndex.get(i))) != 0)
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
            }
            else if (original.isAllSet() && rowIndex.isAllSet())
            {
                this.isAllSet = true;
                this.isNoneSet = false;
            }
            else
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
        public double get(int i)
        {
            return isSet(i) ? this.original.get(this.rowIndex.get(i)) : 0.0d;
        }

        @Override
        public boolean isSet(int i)
        {
            return this.rowIndex.isSet(i) && this.original.isSet(this.rowIndex.get(i));
        }

        @Override
        public double[] toArray(double[] x)
        {
            if (x == null || x.length < size())
            {
                x = new double[size()];
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
            return new ColumnDoubleView(this, rowIndex);
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
