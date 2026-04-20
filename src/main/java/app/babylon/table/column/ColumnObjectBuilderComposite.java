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

import java.util.HashSet;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;

final class ColumnObjectBuilderComposite<T> implements ColumnObject.Builder<T>
{
    private static final int DECISION_POINT = 256;
    private static final double MOSTLY_UNIQUE_THRESHOLD = 0.90d;

    private final ColumnName name;
    private final Column.Type type;
    private ColumnObject.Builder<T> arrayBuilder;
    private ColumnCategorical.Builder<T> dictionaryBuilder;
    private final Set<T> distinctSample;
    private int size;
    private boolean decided;
    private boolean useDictionary;
    private boolean built;
    ColumnObjectBuilderComposite(ColumnName columnName, Column.Type type)
    {
        this.type = ArgumentCheck.nonNull(type);
        this.arrayBuilder = new ColumnObjectBuilderArray<T>(columnName, this.type);
        this.dictionaryBuilder = new ColumnCategoricalBuilderDictionary<T>(columnName, this.type);
        this.name = ArgumentCheck.nonNull(arrayBuilder.getName());
        if (!this.name.equals(dictionaryBuilder.getName()))
        {
            throw new IllegalArgumentException("Array and dictionary builders must share the same column name.");
        }
        this.distinctSample = new HashSet<>();
        this.size = 0;
        this.decided = false;
        this.useDictionary = true;
        this.built = false;
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
    public ColumnObject.Builder<T> add(T x)
    {
        ensureActive();
        if (this.decided)
        {
            activeBuilder().add(x);
            ++this.size;
        }
        else
        {
            this.arrayBuilder.add(x);
            this.dictionaryBuilder.add(x);
            ++this.size;
            if (x != null)
            {
                this.distinctSample.add(x);
            }
            if (this.size == DECISION_POINT)
            {
                decideAtDecisionPoint();
            }
        }
        return this;
    }

    @Override
    public ColumnObject<T> build()
    {
        ensureActive();
        if (!this.decided)
        {
            decideAtCurrentSample();
        }
        ColumnObject<T> builtColumn = activeBuilder().build();
        this.built = true;
        return builtColumn;
    }

    @Override
    public <S> ColumnObject<S> build(Column.Type transformedType)
    {
        ensureActive();
        if (!this.decided)
        {
            decideAtCurrentSample();
        }
        ColumnObject<S> builtColumn = activeBuilder().build(transformedType);
        this.built = true;
        return builtColumn;
    }

    @Override
    public int size()
    {
        ensureActive();
        return this.size;
    }

    private void ensureActive()
    {
        if (this.built)
        {
            throw new IllegalStateException("Builder has already transferred ownership: " + this.name);
        }
    }

    private void decideAtDecisionPoint()
    {
        decideAtCurrentSample();
    }

    private void decideAtCurrentSample()
    {
        double distinctRatio = this.size == 0 ? 0.0d : ((double) this.distinctSample.size() / (double) this.size);
        if (distinctRatio >= MOSTLY_UNIQUE_THRESHOLD)
        {
            selectArray();
        }
        else
        {
            selectDictionary();
        }
    }

    private void selectArray()
    {
        this.useDictionary = false;
        this.dictionaryBuilder = null;
        this.distinctSample.clear();
        this.decided = true;
    }

    private void selectDictionary()
    {
        this.useDictionary = true;
        this.arrayBuilder = null;
        this.distinctSample.clear();
        this.decided = true;
    }

    private ColumnObject.Builder<T> activeBuilder()
    {
        if (this.useDictionary)
        {
            return ArgumentCheck.nonNull(this.dictionaryBuilder);
        }
        return ArgumentCheck.nonNull(this.arrayBuilder);
    }
}
