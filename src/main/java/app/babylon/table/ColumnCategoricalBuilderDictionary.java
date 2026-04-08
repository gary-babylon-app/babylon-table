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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

class ColumnCategoricalBuilderDictionary<T> implements ColumnCategorical.Builder<T>
{
    private final ColumnName name;
    private final Column.Type type;
    private final DictionaryEncoding<T> dictionaryEncoding;

    private CategoryCodeList.Builder codes;
    private boolean isConstant;
    private boolean hasAnySet;
    private boolean hasAnyUnset;

    ColumnCategoricalBuilderDictionary(ColumnName name, Column.Type type)
    {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.dictionaryEncoding = DictionaryEncoding.of();
        this.codes = CategoryCodeList.builder();
        this.isConstant = true;
        this.hasAnySet = false;
        this.hasAnyUnset = false;
    }

    @Override
    public ColumnCategorical.Builder<T> add(T x)
    {
        ensureActive();

        int code = x != null ? this.dictionaryEncoding.codeOf(x) : 0;
        this.codes = this.codes.add(code);
        if (code == 0)
        {
            this.hasAnyUnset = true;
        } else
        {
            this.hasAnySet = true;
        }

        int row = this.codes.size() - 1;
        if (this.isConstant && row > 0)
        {
            this.isConstant = this.codes.get(row - 1) == code;
        }
        return this;
    }

    @Override
    public ColumnCategorical.Builder<T> addNull()
    {
        return add(null);
    }

    @Override
    public int size()
    {
        ensureActive();
        return this.codes.size();
    }

    @Override
    public ColumnName getName()
    {
        return this.name;
    }

    @Override
    public T first()
    {
        return get(0);
    }

    @Override
    public T last()
    {
        return get(size() - 1);
    }

    @Override
    public ColumnCategorical<T> build()
    {
        ensureActive();
        int n = this.codes.size();
        if (this.isConstant && n > 0)
        {
            T first = get(0);
            detachState();
            return new ColumnCategoricalConstant<T>(this.name, first, n, this.type);
        }
        return new ColumnCategoricalDictionary<>(this);
    }

    private T get(int i)
    {
        return this.dictionaryEncoding.valueOf(this.codes.get(i));
    }

    private void detachState()
    {
        if (this.codes != null)
        {
            this.codes.build();
            this.codes = null;
        }
        this.dictionaryEncoding.detachValues();
    }

    private void ensureActive()
    {
        if (this.codes == null)
        {
            throw new IllegalStateException("Builder has already transferred ownership: " + this.name);
        }
    }

    private int getSize()
    {
        ensureActive();
        return this.codes.size();
    }

    private boolean isConstant()
    {
        ensureActive();
        return this.isConstant;
    }

    private boolean isAllSet()
    {
        ensureActive();
        return !this.hasAnyUnset;
    }

    private boolean isNoneSet()
    {
        ensureActive();
        return !this.hasAnySet;
    }

    private Object[] detachDictionary()
    {
        ensureActive();
        return this.dictionaryEncoding.detachValues();
    }

    private CategoryCodeList detachCodes()
    {
        if (this.codes == null)
        {
            throw new IllegalStateException("Builder has already transferred ownership: " + this.name);
        }
        CategoryCodeList out = this.codes.build();
        this.codes = null;
        return out;
    }

    private static final class ColumnCategoricalDictionary<T> implements ColumnCategorical<T>
    {
        private final ColumnName name;
        private final Column.Type type;
        private final Object[] dictionary;
        private final int size;
        private final boolean constant;
        private final boolean allSet;
        private final boolean noneSet;
        private final CategoryCodeList categoricalList;
        private final int[] activeCodes;

        private ColumnCategoricalDictionary(ColumnCategoricalBuilderDictionary<T> builder)
        {
            this.name = Objects.requireNonNull(builder.name);
            this.type = Objects.requireNonNull(builder.type);
            this.size = ArgumentChecks.nonNegative(builder.getSize());
            this.constant = builder.isConstant();
            this.allSet = builder.isAllSet();
            this.noneSet = builder.isNoneSet();
            this.dictionary = Objects.requireNonNull(builder.detachDictionary());
            this.categoricalList = Objects.requireNonNull(builder.detachCodes());
            this.activeCodes = buildDenseActiveCodes(this.dictionary.length);
            if (this.size > this.categoricalList.size())
            {
                throw new IllegalStateException("Size exceeds frozen code buffer size.");
            }
        }

        private <A> ColumnCategoricalDictionary(ColumnCategorical<A> source, Transformer<A, T> transformer)
        {
            Transformer<A, T> xform = Objects.requireNonNull(transformer);
            ColumnCategorical<A> original = Objects.requireNonNull(source);
            this.name = xform.columnName() == null ? original.getName() : xform.columnName();
            this.size = original.size();

            int n = this.size;
            if (n == 0)
            {
                this.type = Column.Type.of(xform.valueClass());
                this.dictionary = new Object[0];
                this.categoricalList = CategoryCodeList.builder().build();
                this.constant = true;
                this.allSet = false;
                this.noneSet = true;
                this.activeCodes = new int[0];
                return;
            }

            int[] originalCategoryCodes = original.getCategoryCodes(null);
            int dictionarySize = maximumCode(originalCategoryCodes) + 1;
            if (dictionarySize < 0)
            {
                throw new IllegalStateException("Negative dictionary size.");
            }

            this.type = Column.Type.of(xform.valueClass());
            this.dictionary = new Object[dictionarySize];
            boolean[] transformedOldCode = new boolean[dictionarySize];
            boolean[] liveCodes = new boolean[dictionarySize];
            CategoryCodeList.Builder remappedCodes = CategoryCodeList.builder();
            for (int i = 0; i < n; ++i)
            {
                int oldCode = original.getCategoryCode(i);
                if (oldCode < 0 || oldCode >= dictionarySize)
                {
                    throw new IllegalStateException("Category code out of dictionary bounds.");
                }

                if (original.isSet(i))
                {
                    if (!transformedOldCode[oldCode])
                    {
                        T transformed = xform.apply(original.get(i));
                        this.dictionary[oldCode] = transformed;
                        transformedOldCode[oldCode] = true;
                        liveCodes[oldCode] = transformed != null;
                    }
                    int remappedCode = this.dictionary[oldCode] == null ? 0 : oldCode;
                    remappedCodes.add(remappedCode);
                } else
                {
                    remappedCodes.add(0);
                }
            }
            this.categoricalList = remappedCodes.build();
            this.activeCodes = buildActiveCodes(liveCodes);
            this.constant = isConstantByValue(this.dictionary, this.categoricalList, n);
            this.allSet = !containsCodeZero(this.categoricalList, n);
            this.noneSet = !containsNonZeroCode(this.categoricalList, n);
        }

        @Override
        public boolean isConstant()
        {
            return this.constant;
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
        public Column.Type getType()
        {
            return this.type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get(int i)
        {
            return (T) this.dictionary[this.categoricalList.get(i)];
        }

        @SuppressWarnings("unchecked")
        @Override
        public T getCategoryValue(int categoryCode)
        {
            return (T) this.dictionary[categoryCode];
        }

        @Override
        public int getCategoryCode(int i)
        {
            return this.categoricalList.get(i);
        }

        @Override
        public int[] getCategoryCodes(int[] x)
        {
            if (x == null || x.length != this.activeCodes.length)
            {
                x = new int[this.activeCodes.length];
            }
            System.arraycopy(this.activeCodes, 0, x, 0, this.activeCodes.length);
            return x;
        }

        @Override
        public boolean isSet(int i)
        {
            return this.categoricalList.get(i) != 0;
        }

        @Override
        public boolean isAllSet()
        {
            return this.allSet;
        }

        @Override
        public boolean isNoneSet()
        {
            return this.noneSet;
        }

        @Override
        public ColumnCategorical<T> view(ViewIndex rowIndex)
        {
            return new ColumnCategoricalView<>(this, rowIndex);
        }

        @Override
        public boolean equals(Object obj)
        {
            return ColumnObject.equals(this, obj);
        }

        @Override
        public String toString()
        {
            return Columns.toString(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collection<T> getCategoricalValues(Collection<T> x)
        {
            if (x == null)
            {
                x = new ArrayList<T>();
            }
            for (int code : this.activeCodes)
            {
                x.add((T) this.dictionary[code]);
            }
            return x;
        }

        @Override
        public <S> ColumnCategorical<S> transform(Transformer<T, S> transformer)
        {
            return new ColumnCategoricalDictionary<S>(this, transformer);
        }

        private static boolean isConstantByValue(Object[] dictionary, CategoryCodeList codes, int size)
        {
            if (size <= 1)
            {
                return true;
            }
            boolean firstSet = codes.get(0) != 0;
            Object first = firstSet ? dictionary[codes.get(0)] : null;
            for (int i = 1; i < size; ++i)
            {
                boolean set = codes.get(i) != 0;
                if (set != firstSet)
                {
                    return false;
                }
                if (set)
                {
                    Object v = dictionary[codes.get(i)];
                    if (first == null ? v != null : !first.equals(v))
                    {
                        return false;
                    }
                }
            }
            return true;
        }

        private static int[] buildDenseActiveCodes(int dictionarySize)
        {
            if (dictionarySize <= 1)
            {
                return new int[0];
            }
            int[] codes = new int[dictionarySize - 1];
            for (int i = 1; i < dictionarySize; ++i)
            {
                codes[i - 1] = i;
            }
            return codes;
        }

        private static int maximumCode(int[] categoryCodes)
        {
            int max = 0;
            for (int code : categoryCodes)
            {
                max = Math.max(max, code);
            }
            return max;
        }

        private static int[] buildActiveCodes(boolean[] liveCodes)
        {
            int n = 0;
            for (int i = 1; i < liveCodes.length; ++i)
            {
                if (liveCodes[i])
                {
                    ++n;
                }
            }
            int[] codes = new int[n];
            int j = 0;
            for (int i = 1; i < liveCodes.length; ++i)
            {
                if (liveCodes[i])
                {
                    codes[j] = i;
                    ++j;
                }
            }
            return codes;
        }

        private static boolean containsCodeZero(CategoryCodeList codes, int size)
        {
            for (int i = 0; i < size; ++i)
            {
                if (codes.get(i) == 0)
                {
                    return true;
                }
            }
            return false;
        }

        private static boolean containsNonZeroCode(CategoryCodeList codes, int size)
        {
            for (int i = 0; i < size; ++i)
            {
                if (codes.get(i) != 0)
                {
                    return true;
                }
            }
            return false;
        }

    }

    static <A, S> ColumnCategorical<S> transformPreservingCodes(ColumnCategorical<A> source,
            Transformer<A, S> transformer)
    {
        return new ColumnCategoricalDictionary<S>(source, transformer);
    }
}
