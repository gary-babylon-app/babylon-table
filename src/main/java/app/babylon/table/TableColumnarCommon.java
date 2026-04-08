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

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

abstract class TableColumnarCommon implements TableColumnar
{

    private final TableDescription description;

    TableColumnarCommon(TableDescription description)
    {
        this.description = description == null ? new TableDescription() : description;
    }

    @Override
    public TableDescription getDescription()
    {
        return this.description;
    }

    @Override
    public ColumnInt getInt(ColumnName x)
    {
        Column column = get(x);
        if (column instanceof ColumnInt)
        {
            return (ColumnInt) column;
        }
        if (column != null)
        {
            throw new RuntimeException(x + " not int column.");
        }
        return null;
    }

    @Override
    public ColumnDouble getDouble(ColumnName x)
    {
        Column column = get(x);
        if (column instanceof ColumnDouble)
        {
            return (ColumnDouble) column;
        }
        if (column != null)
        {
            throw new RuntimeException(x + " not double column.");
        }
        return null;
    }

    @Override
    public ColumnLong getLong(ColumnName x)
    {
        Column column = get(x);
        if (column instanceof ColumnLong)
        {
            return (ColumnLong) column;
        }
        if (column != null)
        {
            throw new RuntimeException(x + " not long column.");
        }
        return null;
    }

    @Override
    public TableColumnar replaceOrAdd(Column... x)
    {
        Collection<Column> replaces = new ArrayList<>();
        Collection<Column> adds = new ArrayList<>();
        for (Column column : x)
        {
            if (this.contains(column.getName()))
            {
                replaces.add(column);
            } else
            {
                adds.add(column);
            }
        }

        TableColumnar t = this;
        if (replaces.size() > 0)
        {
            t = this.replace(replaces.toArray(new Column[replaces.size()]));
        }
        if (adds.size() > 0)
        {
            t = this.addColumns(adds.toArray(new Column[adds.size()]));
        }
        return t;
    }

    @Override
    public ColumnName[] getColumnNames()
    {
        Collection<ColumnName> colNames = getColumnNames(new ArrayList<>());
        return colNames.toArray(new ColumnName[colNames.size()]);
    }

    @Override
    public Iterable<Column> columns()
    {
        ColumnName[] names = getColumnNames();
        return () -> new Iterator<Column>()
        {
            private int index = 0;

            @Override
            public boolean hasNext()
            {
                return this.index < names.length;
            }

            @Override
            public Column next()
            {
                if (!hasNext())
                {
                    throw new NoSuchElementException();
                }
                ColumnName columnName = names[this.index++];
                return get(columnName);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ColumnObject<T> getTyped(ColumnName x, Class<T> clazz)
    {
        if (clazz == null)
        {
            throw new IllegalArgumentException("Column value class cannot be null.");
        }
        Column column = get(x);
        if (column != null && column instanceof ColumnObject<?> && clazz.equals(column.getType().getValueClass()))
        {
            return (ColumnObject<T>) column;
        }
        if (column != null)
        {
            throw new RuntimeException(x + " not " + clazz.getSimpleName() + " column but " + column.getType());
        }
        return null;
    }

    @Override
    public ColumnObject<String> getString(ColumnName x)
    {
        return getTyped(x, String.class);
    }

    @Override
    public ColumnObject<BigDecimal> getDecimal(ColumnName x)
    {
        return getTyped(x, BigDecimal.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> ColumnCategorical<T> getEnum(ColumnName x)
    {
        Column column = get(x);
        if (column instanceof ColumnCategorical && column.getType().getValueClass().isEnum())
        {
            return (ColumnCategorical<T>) column;
        }
        if (column != null)
        {
            throw new RuntimeException(x + " not Enum column.");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ColumnCategorical<T> getCategorical(ColumnName x)
    {
        Column column = get(x);
        if (column instanceof ColumnCategorical)
        {
            return (ColumnCategorical<T>) column;
        }
        if (column != null)
        {
            throw new RuntimeException(x + " column not categorical column in " + this.getName() + ". Column Type: "
                    + column.getClass().getSimpleName());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ColumnCategorical<T> getCategorical(ColumnName x, Class<T> clazz)
    {
        if (clazz == null)
        {
            throw new IllegalArgumentException("Categorical value class cannot be null.");
        }
        Column column = get(x);
        if (column != null && column instanceof ColumnCategorical<?> && clazz.equals(column.getType().getValueClass()))
        {
            return (ColumnCategorical<T>) column;
        }
        if (column != null)
        {
            throw new RuntimeException(
                    x + " not " + clazz.getSimpleName() + " categorical column but " + column.getType());
        }
        return null;
    }

    @Override
    public String toString()
    {
        return TablesToString.printSmallTable(this);
    }

    @Override
    public TableColumnar getRow(int i)
    {
        Column[] columns = getColumns();
        Column[] newOneRowColumns = new Column[getColumnCount()];
        for (int j = 0; j < newOneRowColumns.length; ++j)
        {
            newOneRowColumns[j] = columns[j].getAsColumn(i);
        }
        TableDescription description = new TableDescription("");
        return Tables.newTable(TableName.of(getName().toString() + " Row " + i), description, newOneRowColumns);
    }

    @Override
    public TableColumnar getLastRow()
    {
        return getRow(getRowCount() - 1);
    }

    @Override
    public TableColumnar getFirstRow()
    {
        return getRow(0);
    }

    @Override
    public Map<ColumnName, Column> getColumns(Map<ColumnName, Column> x, ColumnName... columnNames)
    {
        if (x == null)
        {
            x = new LinkedHashMap<>();
        }
        Column[] columns = Empties.isEmpty(columnNames) ? getColumns() : getColumns(columnNames);
        if (columns != null)
        {
            for (Column column : columns)
            {
                x.put(column.getName(), column);
            }
        }
        return x;
    }

    @Override
    public TableColumnar replace(Column... x)
    {
        if (x != null && x.length > 0)
        {
            Map<ColumnName, Column> replacementsByName = new HashMap<>();
            for (Column column : x)
            {
                if (column != null)
                {
                    ColumnName name = column.getName();
                    if (!contains(name))
                    {
                        throw new RuntimeException("No column to replace with " + name);
                    }
                    replacementsByName.put(name, column);
                }
            }
            if (replacementsByName.isEmpty())
            {
                return this;
            }

            Column[] columns = getColumns();
            for (int i = 0; i < columns.length; ++i)
            {
                Column currentColumn = columns[i];
                Column replacement = replacementsByName.get(currentColumn.getName());
                if (replacement != null)
                {
                    columns[i] = replacement;
                }
            }
            return new TableColumnarMap(getName(), getDescription(), columns);
        }
        return this;
    }

    @Override
    public TableColumnar addColumns(Column... x)
    {
        if (!Empties.isEmpty(x))
        {
            for (int i = 0; i < x.length; ++i)
            {
                if (contains(x[i].getName()))
                {
                    throw new RuntimeException(
                            "AddColumns failed on table " + getName() + ", " + x[i].getName() + " already present.");
                }
            }
            Column[] allColumns = new Column[this.getColumnCount() + x.length];
            Column[] existingColumns = this.getColumns();
            for (int i = 0; i < existingColumns.length; ++i)
            {
                allColumns[i] = existingColumns[i];
            }
            for (int i = 0; i < x.length; ++i)
            {
                allColumns[i + this.getColumnCount()] = x[i];
            }

            return new TableColumnarMap(getName(), getDescription(), allColumns);
        } else
        {
            return this;
        }
    }

    @Override
    public TableColumnar prune()
    {
        Collection<ColumnName> columnsToRemove = new ArrayList<>();
        for (Column column : getColumns())
        {
            if (column.isNoneSet())
            {
                columnsToRemove.add(column.getName());
            }
        }
        if (columnsToRemove.isEmpty())
        {
            return this;
        }
        return removeColumns(columnsToRemove.toArray(new ColumnName[columnsToRemove.size()]));
    }
}
