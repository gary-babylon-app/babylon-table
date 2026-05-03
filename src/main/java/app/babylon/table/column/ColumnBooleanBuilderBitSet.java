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
import app.babylon.table.ViewIndex;

class ColumnBooleanBuilderBitSet implements ColumnBoolean.Builder
{
    private final ColumnName name;
    private BitList.Builder values;
    private BitList.Builder isSet;
    private int size;
    private boolean hasAnySet;
    private boolean hasAnyUnset;
    private boolean built;

    ColumnBooleanBuilderBitSet(ColumnName cn)
    {
        this.name = ArgumentCheck.nonNull(cn);
        this.values = BitList.builder();
        this.isSet = BitList.builder();
        this.size = 0;
        this.hasAnySet = false;
        this.hasAnyUnset = false;
        this.built = false;
    }

    public int size()
    {
        ensureActive();
        return this.size;
    }

    @Override
    public ColumnBoolean.Builder addNull()
    {
        ensureActive();
        this.values.add(false);
        this.isSet.add(false);
        this.hasAnyUnset = true;
        ++this.size;
        return this;
    }

    @Override
    public ColumnBoolean.Builder add(boolean x)
    {
        ensureActive();
        this.values.add(x);
        this.isSet.add(true);
        this.hasAnySet = true;
        ++this.size;
        return this;
    }

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    /**
     * Builds an immutable column and transfers ownership of internal storage.
     */
    @Override
    public ColumnBoolean build()
    {
        boolean constant = isConstant();
        boolean allSet = !this.hasAnyUnset;
        boolean constantValue = this.size > 0 && this.values.get(0);
        ColumnName columnName = getName();
        BitList transferredValues = detachValues();
        BitList transferredIsSet = detachIsSet();
        if (constant && allSet)
        {
            return new ColumnBooleanConstant(columnName, constantValue, this.size);
        }
        return new ColumnBooleanArray(columnName, transferredValues, transferredIsSet, this.size, constant, allSet,
                !this.hasAnySet);
    }

    private BitList detachValues()
    {
        ensureActive();
        BitList detached = this.values.build();
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
        boolean firstValue = this.values.get(0);
        for (int i = 1; i < this.size; ++i)
        {
            if (!this.isSet.get(i) || this.values.get(i) != firstValue)
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

    private static final class ColumnBooleanArray implements ColumnBoolean
    {
        private final ColumnName name;
        private final BitList values;
        private final BitList isSet;
        private final int size;
        private final boolean isConstant;
        private final boolean isAllSet;
        private final boolean isNoneSet;

        private ColumnBooleanArray(ColumnName name, BitList values, BitList isSet, int size, boolean isConstant,
                boolean isAllSet, boolean isNoneSet)
        {
            this.name = ArgumentCheck.nonNull(name);
            this.values = ArgumentCheck.nonNull(values);
            this.isSet = ArgumentCheck.nonNull(isSet);
            this.size = ArgumentCheck.nonNegative(size);
            if (size > values.size() || size > isSet.size())
            {
                throw new IllegalArgumentException("Size exceeds bit-list length.");
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
        public boolean get(int i)
        {
            return this.values.get(i);
        }

        @Override
        public boolean isSet(int i)
        {
            return this.isSet.get(i);
        }

        @Override
        public boolean[] toArray(boolean[] x)
        {
            if (x == null || x.length < this.size)
            {
                x = new boolean[this.size];
            }
            for (int i = 0; i < this.size; ++i)
            {
                x[i] = this.values.get(i);
            }
            return x;
        }

        @Override
        public Column view(ViewIndex rowIndex)
        {
            return new ColumnBooleanView(this, rowIndex);
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

    private static final class ColumnBooleanView implements ColumnBoolean
    {
        private final ColumnBoolean original;
        private final ViewIndex rowIndex;
        private final boolean isConstant;
        private final boolean isAllSet;
        private final boolean isNoneSet;

        private ColumnBooleanView(ColumnBoolean original, ViewIndex rowIndex)
        {
            this.original = ArgumentCheck.nonNull(original);
            this.rowIndex = ArgumentCheck.nonNull(rowIndex);
            this.isConstant = computeIsConstant(original, rowIndex);
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

        private static boolean computeIsConstant(ColumnBoolean original, ViewIndex rowIndex)
        {
            if (rowIndex.size() <= 1 || original.isConstant())
            {
                return true;
            }
            boolean firstSet = rowIndex.isSet(0) && original.isSet(rowIndex.get(0));
            boolean firstValue = firstSet && original.get(rowIndex.get(0));
            for (int i = 1; i < rowIndex.size(); ++i)
            {
                boolean currentSet = rowIndex.isSet(i) && original.isSet(rowIndex.get(i));
                if (currentSet != firstSet)
                {
                    return false;
                }
                if (currentSet && original.get(rowIndex.get(i)) != firstValue)
                {
                    return false;
                }
            }
            return true;
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
        public boolean get(int i)
        {
            return isSet(i) && this.original.get(this.rowIndex.get(i));
        }

        @Override
        public boolean isSet(int i)
        {
            return this.rowIndex.isSet(i) && this.original.isSet(this.rowIndex.get(i));
        }

        @Override
        public boolean[] toArray(boolean[] x)
        {
            if (x == null || x.length < size())
            {
                x = new boolean[size()];
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
            return new ColumnBooleanView(this, rowIndex);
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
