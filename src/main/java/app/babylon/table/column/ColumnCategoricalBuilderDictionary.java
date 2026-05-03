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

import java.util.ArrayList;
import java.util.Collection;

import app.babylon.table.ViewIndex;
import app.babylon.table.column.type.TypeParser;

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
        this.name = ArgumentCheck.nonNull(name);
        this.type = ArgumentCheck.nonNull(type);
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
        }
        else
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
    public Column.Type getType()
    {
        return this.type;
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

    @Override
    public Column buildAs(Column.Type transformedType)
    {
        ensureActive();
        Column.Type targetType = ArgumentCheck.nonNull(transformedType);
        if (targetType.isPrimitive())
        {
            return app.babylon.table.transform.TransformToPrimitive.builder(targetType, getName()).build()
                    .apply(build());
        }
        Class<?> valueClass = this.type.getValueClass();
        if (!CharSequence.class.isAssignableFrom(valueClass))
        {
            throw new IllegalStateException(
                    "Categorical parsed build requires CharSequence values, not " + valueClass.getName());
        }
        @SuppressWarnings("unchecked")
        TypeParser<Object> parser = (TypeParser<Object>) targetType.getParser();
        return new ColumnCategoricalDictionary<Object>(this, targetType, parser);
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
        private final boolean denseActiveCodes;

        private ColumnCategoricalDictionary(ColumnCategoricalBuilderDictionary<T> builder)
        {
            this.name = ArgumentCheck.nonNull(builder.name);
            this.type = ArgumentCheck.nonNull(builder.type);
            this.size = ArgumentCheck.nonNegative(builder.getSize());
            this.constant = builder.isConstant();
            this.allSet = builder.isAllSet();
            this.noneSet = builder.isNoneSet();
            this.dictionary = ArgumentCheck.nonNull(builder.detachDictionary());
            this.categoricalList = ArgumentCheck.nonNull(builder.detachCodes());
            this.denseActiveCodes = true;
            if (this.size > this.categoricalList.size())
            {
                throw new IllegalStateException("Size exceeds frozen code buffer size.");
            }
        }

        private ColumnCategoricalDictionary(ColumnCategoricalBuilderDictionary<?> builder, Column.Type transformedType,
                TypeParser<T> parser)
        {
            this.name = ArgumentCheck.nonNull(builder.name);
            this.size = ArgumentCheck.nonNegative(builder.getSize());
            this.type = ArgumentCheck.nonNull(transformedType);
            boolean originalAllSet = builder.isAllSet();

            Object[] sourceDictionary = ArgumentCheck.nonNull(builder.detachDictionary());
            CategoryCodeList detachedCodes = ArgumentCheck.nonNull(builder.detachCodes());
            int dictionarySize = sourceDictionary.length;
            this.dictionary = new Object[dictionarySize];
            boolean allLive = true;
            boolean anyLive = false;
            boolean remapRequired = false;

            for (int oldCode = 1; oldCode < dictionarySize; ++oldCode)
            {
                CharSequence sourceValue = (CharSequence) sourceDictionary[oldCode];
                T transformed = parser.parse(sourceValue);
                this.dictionary[oldCode] = transformed;
                boolean live = transformed != null;
                allLive &= live;
                anyLive |= live;
                remapRequired |= !live;
            }

            if (remapRequired)
            {
                CategoryCodeList.Builder remappedCodes = CategoryCodeList.builder();
                for (int i = 0; i < this.size; ++i)
                {
                    int oldCode = detachedCodes.get(i);
                    if (oldCode < 0 || oldCode >= dictionarySize)
                    {
                        throw new IllegalStateException("Category code out of dictionary bounds.");
                    }
                    if (oldCode == 0)
                    {
                        remappedCodes.add(0);
                    }
                    else
                    {
                        remappedCodes.add(this.dictionary[oldCode] != null ? oldCode : 0);
                    }
                }
                this.categoricalList = remappedCodes.build();
            }
            else
            {
                this.categoricalList = detachedCodes;
            }

            this.denseActiveCodes = !remapRequired;
            this.allSet = originalAllSet && allLive;
            this.noneSet = !anyLive;
            if (this.noneSet)
            {
                this.constant = true;
            }
            else if (!this.allSet)
            {
                this.constant = false;
            }
            else if (dictionarySize <= 2)
            {
                this.constant = true;
            }
            else
            {
                // Parsing can be many-to-one even when no remap is required.
                // For example, source categorical values "usd", "USD", "usd"
                // may occupy dictionary codes 1 and 2, while both transformed
                // values become the same Currency. In that case the dictionary
                // still has more than one live non-null code, but the column is
                // constant by value.
                this.constant = allDenseValuesEqual(this.dictionary);
            }
        }

        private <A> ColumnCategoricalDictionary(ColumnCategorical<A> source, Transformer<A, T> transformer)
        {
            Transformer<A, T> xform = ArgumentCheck.nonNull(transformer);
            ColumnCategorical<A> original = ArgumentCheck.nonNull(source);
            this.name = xform.columnName() == null ? original.getName() : xform.columnName();
            this.size = original.size();

            int n = this.size;
            if (n == 0)
            {
                this.type = xform.type();
                this.dictionary = new Object[0];
                this.categoricalList = CategoryCodeList.builder().build();
                this.denseActiveCodes = true;
                this.constant = true;
                this.allSet = false;
                this.noneSet = true;
                return;
            }

            int[] originalCategoryCodes = original.getCategoryCodes(null);
            int dictionarySize = maximumCode(originalCategoryCodes) + 1;
            if (dictionarySize < 0)
            {
                throw new IllegalStateException("Negative dictionary size.");
            }

            this.type = xform.type();
            this.dictionary = new Object[dictionarySize];
            boolean allLive = true;
            boolean anyLive = false;
            boolean remapRequired = false;
            boolean denseActiveCodes = isDenseCategoryCodes(originalCategoryCodes);
            for (int oldCode : originalCategoryCodes)
            {
                if (oldCode < 0 || oldCode >= dictionarySize)
                {
                    throw new IllegalStateException("Category code out of dictionary bounds.");
                }
                T transformed = xform.apply(original.getCategoryValue(oldCode));
                this.dictionary[oldCode] = transformed;
                boolean live = transformed != null;
                allLive &= live;
                anyLive |= live;
                remapRequired |= !live;
            }

            CategoryCodeList.Builder remappedCodes = CategoryCodeList.builder();
            for (int i = 0; i < n; ++i)
            {
                int oldCode = original.getCategoryCode(i);
                if (oldCode < 0 || oldCode >= dictionarySize)
                {
                    throw new IllegalStateException("Category code out of dictionary bounds.");
                }

                if (oldCode == 0)
                {
                    remappedCodes.add(0);
                }
                else if (remapRequired)
                {
                    remappedCodes.add(this.dictionary[oldCode] != null ? oldCode : 0);
                }
                else
                {
                    remappedCodes.add(oldCode);
                }
            }
            this.categoricalList = remappedCodes.build();
            this.denseActiveCodes = !remapRequired && denseActiveCodes;
            this.allSet = original.isAllSet() && allLive;
            this.noneSet = !anyLive;
            if (this.noneSet)
            {
                this.constant = true;
            }
            else if (!this.allSet)
            {
                this.constant = false;
            }
            else if (originalCategoryCodes.length <= 1)
            {
                this.constant = true;
            }
            else
            {
                this.constant = allUsedValuesEqual(this.dictionary, originalCategoryCodes);
            }
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
            if (this.denseActiveCodes)
            {
                int activeCodeCount = Math.max(this.dictionary.length - 1, 0);
                if (x == null || x.length != activeCodeCount)
                {
                    x = new int[activeCodeCount];
                }
                for (int code = 1; code < this.dictionary.length; ++code)
                {
                    x[code - 1] = code;
                }
                return x;
            }

            int activeCodeCount = 0;
            for (int code = 1; code < this.dictionary.length; ++code)
            {
                if (this.dictionary[code] != null)
                {
                    ++activeCodeCount;
                }
            }
            if (x == null || x.length != activeCodeCount)
            {
                x = new int[activeCodeCount];
            }
            int i = 0;
            for (int code = 1; code < this.dictionary.length; ++code)
            {
                if (this.dictionary[code] != null)
                {
                    x[i] = code;
                    ++i;
                }
            }
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
            if (this.denseActiveCodes)
            {
                for (int code = 1; code < this.dictionary.length; ++code)
                {
                    x.add((T) this.dictionary[code]);
                }
                return x;
            }
            for (int code = 1; code < this.dictionary.length; ++code)
            {
                if (this.dictionary[code] != null)
                {
                    x.add((T) this.dictionary[code]);
                }
            }
            return x;
        }

        @Override
        public <S> ColumnCategorical<S> transform(Transformer<T, S> transformer)
        {
            return new ColumnCategoricalDictionary<S>(this, transformer);
        }

        private static boolean allDenseValuesEqual(Object[] dictionary)
        {
            if (dictionary.length <= 2)
            {
                return true;
            }
            Object first = dictionary[1];
            for (int code = 2; code < dictionary.length; ++code)
            {
                Object value = dictionary[code];
                if (first == null ? value != null : !first.equals(value))
                {
                    return false;
                }
            }
            return true;
        }

        private static boolean allUsedValuesEqual(Object[] dictionary, int[] usedCodes)
        {
            if (usedCodes.length <= 1)
            {
                return true;
            }
            Object first = dictionary[usedCodes[0]];
            for (int i = 1; i < usedCodes.length; ++i)
            {
                Object value = dictionary[usedCodes[i]];
                if (first == null ? value != null : !first.equals(value))
                {
                    return false;
                }
            }
            return true;
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

        private static boolean isDenseCategoryCodes(int[] categoryCodes)
        {
            for (int i = 0; i < categoryCodes.length; ++i)
            {
                if (categoryCodes[i] != i + 1)
                {
                    return false;
                }
            }
            return true;
        }

    }

    static <A, S> ColumnCategorical<S> transformPreservingCodes(ColumnCategorical<A> source,
            Transformer<A, S> transformer)
    {
        return new ColumnCategoricalDictionary<S>(source, transformer);
    }
}
