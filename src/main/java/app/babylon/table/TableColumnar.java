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

import app.babylon.lang.ArgumentCheck;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.grouping.GroupBy;
import app.babylon.table.grouping.GroupBys;
import app.babylon.table.selection.RowFilter;
import app.babylon.table.selection.Selection;
import app.babylon.table.sorting.TableSort;
import app.babylon.table.sorting.TableSort.SortOrder;
import app.babylon.table.transform.Transform;
/**
 * A column-oriented table whose data is exposed as named columns and can be
 * sliced, filtered, transformed, grouped, and sorted.
 */
public interface TableColumnar extends Table
{
    /**
     * Returns a one-row columnar table containing the last row.
     *
     * @return a one-row columnar table containing the last row
     */
    @Override
    public TableColumnar getLastRow();

    /**
     * Returns a one-row columnar table containing the first row.
     *
     * @return a one-row columnar table containing the first row
     */
    @Override
    public TableColumnar getFirstRow();

    /**
     * Returns a one-row columnar table containing the row at the supplied index.
     *
     * @param i
     *            the zero-based row index
     * @return a one-row columnar table containing the row at {@code i}
     */
    @Override
    public TableColumnar getRow(int i);

    public Column get(ColumnName x);

    default public Column.Type getType(ColumnName x)
    {
        Column column = get(x);
        return column == null ? null : column.getType();
    }

    public boolean contains(ColumnName x);

    public Collection<ColumnName> getColumnNames(Collection<ColumnName> x);

    public ColumnName[] getColumnNames();

    public Column[] getColumns();

    public <T extends Enum<T>> ColumnCategorical<T> getEnum(ColumnName x);

    public <T> ColumnCategorical<T> getCategorical(ColumnName x);

    public <T> ColumnCategorical<T> getCategorical(ColumnName x, Column.Type type);

    public <T> ColumnObject<T> getObject(ColumnName x, Column.Type type);

    public ColumnObject<String> getString(ColumnName x);

    public ColumnObject<BigDecimal> getDecimal(ColumnName x);

    public ColumnDouble getDouble(ColumnName x);

    public ColumnLong getLong(ColumnName x);

    public ColumnInt getInt(ColumnName x);

    public Iterable<Column> columns();

    public TableColumnar replace(Column... x);

    public TableColumnar add(Column... x);

    public TableColumnar removeColumns(ColumnName... x);

    /**
     * Removes columns that contain no set values.
     */
    public TableColumnar prune();

    default public String toString10()
    {
        return TablesToString.printFullTable(this, ToStringSettings.standard(), 10);
    }

    default public String toString10(ToStringSettings settings)
    {
        return TablesToString.printFullTable(this, settings, 10);
    }

    default public String toString(ToStringSettings settings)
    {
        return TablesToString.printFullTable(this, settings, this.getRowCount());
    }

    default public Column[] getColumns(ColumnName... x)
    {
        if (x == null)
        {
            return null;
        }
        Collection<ColumnName> tableNames = getColumnNames(new ArrayList<>());
        List<ColumnName> retainedNames = new ArrayList<>();
        Collections.addAll(retainedNames, x);

        retainedNames.retainAll(tableNames);

        Column[] selectedColumns = new Column[retainedNames.size()];
        for (int i = 0; i < retainedNames.size(); ++i)
        {
            Column c = get(retainedNames.get(i));
            selectedColumns[i] = c;
        }
        return selectedColumns;
    }

    public Map<ColumnName, Column> getColumns(Map<ColumnName, Column> x, ColumnName... columnNames);

    default public TableColumnar select(ColumnName... x)
    {
        if (!Is.empty(x))
        {
            return new TableColumnarMap(getName(), getDescription(), getColumns(x));
        }
        else
        {
            return this;
        }
    }

    default public TableColumnar select(Selection x)
    {
        return Tables.select(this, x);
    }

    default public TableColumnar filter(RowFilter filter)
    {
        RowFilter f = ArgumentCheck.nonNull(filter);
        IntPredicate predicate = f.bind(this);
        Selection selection = new Selection("filtered");
        for (int i = 0; i < getRowCount(); ++i)
        {
            selection.add(predicate.test(i));
        }
        return select(selection);
    }

    default public GroupBy groupBy(ColumnName... v)
    {
        return GroupBys.groupBy(this, v);
    }

    default public TableColumnar apply(Transform... transforms)
    {
        Map<ColumnName, Column> columnsByName = getColumns(new LinkedHashMap<>());
        for (Transform transform : transforms)
        {
            if (transform != null)
            {
                transform.apply(columnsByName);
            }
        }
        return Tables.newTable(getName(), getDescription(), columnsByName.values());
    }

    default public TableColumnar apply(Collection<Transform> transforms)
    {
        Map<ColumnName, Column> columnsByName = getColumns(new LinkedHashMap<>());
        for (Transform transform : transforms)
        {
            if (transform != null)
            {
                transform.apply(columnsByName);
            }
        }
        return Tables.newTable(getName(), getDescription(), columnsByName.values());
    }

    default public TableColumnar apply(Transform transform)
    {
        Map<ColumnName, Column> columnsByName = getColumns(new LinkedHashMap<>());
        if (transform != null)
        {
            transform.apply(columnsByName);
        }
        return Tables.newTable(getName(), getDescription(), columnsByName.values());
    }

    default public TableColumnar sort(ColumnName... x)
    {
        return TableSort.sort(this, x);
    }

    default public TableColumnar sort(SortOrder sortOrder, ColumnName... x)
    {
        return TableSort.sort(this, sortOrder, x);
    }

    public static boolean isEmpty(TableColumnar table)
    {
        if (table == null)
        {
            return true;
        }
        if (table.getColumnCount() == 0)
        {
            return true;
        }
        if (table.getRowCount() == 0)
        {
            return true;
        }
        return false;
    }

}
