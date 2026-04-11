/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;

public abstract class TabularRowReaderCommon<R extends TabularRowReaderCommon<R>> implements TabularRowReader
{
    private final Set<ColumnName> selectedColumns;
    private final Map<ColumnName, ColumnName> columnRenames;
    private final Set<ColumnName> renamedTargets;
    private RowFilter rowFilter;

    protected TabularRowReaderCommon()
    {
        this.selectedColumns = new LinkedHashSet<>();
        this.columnRenames = new LinkedHashMap<>();
        this.renamedTargets = new HashSet<>();
        this.rowFilter = null;
    }

    @Override
    public R withSelectedColumn(ColumnName columnName)
    {
        this.selectedColumns.add(ArgumentCheck.nonNull(columnName));
        return self();
    }

    @Override
    public R withSelectedColumns(ColumnName... columnNames)
    {
        if (!Is.empty(columnNames))
        {
            this.selectedColumns.addAll(Arrays.asList(columnNames));
        }
        return self();
    }

    public R withSelectedColumns(Collection<ColumnName> columnNames)
    {
        if (!Is.empty(columnNames))
        {
            this.selectedColumns.addAll(columnNames);
        }
        return self();
    }

    @Override
    public R withColumnRename(ColumnName original, ColumnName newName)
    {
        ColumnName originalChecked = ArgumentCheck.nonNull(original);
        ColumnName newNameChecked = ArgumentCheck.nonNull(newName);
        if (this.columnRenames.containsKey(originalChecked))
        {
            throw new IllegalArgumentException("Rename failed, column " + originalChecked + " already renamed");
        }
        if (this.renamedTargets.contains(newNameChecked))
        {
            throw new IllegalArgumentException("Rename failed, target column " + newNameChecked + " already used");
        }
        this.columnRenames.put(originalChecked, newNameChecked);
        this.renamedTargets.add(newNameChecked);
        return self();
    }

    @Override
    public R withColumnRenames(Map<ColumnName, ColumnName> renames)
    {
        if (!Is.empty(renames))
        {
            // loop because we want the validation checking
            for (Map.Entry<ColumnName, ColumnName> entry : renames.entrySet())
            {
                withColumnRename(entry.getKey(), entry.getValue());
            }
        }
        return self();
    }

    @Override
    public R withRowFilter(RowFilter rowFilter)
    {
        this.rowFilter = ArgumentCheck.nonNull(rowFilter);
        return self();
    }

    protected Set<ColumnName> getSelectedColumns(Set<ColumnName> x)
    {
        if (x == null)
        {
            x = new LinkedHashSet<ColumnName>(this.selectedColumns.size());
        }
        x.addAll(this.selectedColumns);
        return x;
    }

    protected Map<ColumnName, ColumnName> getColumnRenames(Map<ColumnName, ColumnName> x)
    {
        if (x == null)
        {
            x = new LinkedHashMap<ColumnName, ColumnName>(this.columnRenames.size());
        }
        return this.columnRenames;
    }

    protected RowFilter getRowFilter()
    {
        return this.rowFilter;
    }

    @Override
    public ColumnName getColumnReName(ColumnName original)
    {
        return this.columnRenames.getOrDefault(original, original);
    }

    protected abstract R self();
}
