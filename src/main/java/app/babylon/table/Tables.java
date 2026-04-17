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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.Column.Builder;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.selection.Selection;

public class Tables
{
    public static TableColumnar removeDuplicates(TableColumnar table, ColumnName... indexColumns)
    {
        ArgumentCheck.nonEmpty(indexColumns);
        Set<RowValue> values = new LinkedHashSet<>();
        Column[] columns = table.getColumns(indexColumns);
        ViewIndex.Builder viewIndexBuilder = ViewIndex.builder();
        for (int i = 0; i < table.getRowCount(); ++i)
        {
            RowValue rowValue = RowValue.of(columns, i);
            if (values.contains(rowValue))
            {
                continue;
            }
            values.add(rowValue);
            viewIndexBuilder.add(i);
        }
        return Tables.newTableView(table.getName(), table, viewIndexBuilder.build());
    }

    public static TableColumnar concat(TableName tableName, TableDescription tableDescription, TableColumnar... tables)
    {
        if (tables != null)
        {
            return concat(tableName, tableDescription, List.of(tables));
        }
        return null;
    }

    public static TableColumnar concat(TableName tableName, TableDescription tableDescription,
            Collection<TableColumnar> tables)
    {
        if (tables.size() == 0)
        {
            return null;
        }

        if (tables.size() == 1)
        {
            TableColumnar table = tables.iterator().next();
            tableName = (tableName == null) ? table.getName() : tableName;
            return Tables.newTable(tableName, tableDescription, table.getColumns());
        }

        Set<ColumnName> columnNames = new LinkedHashSet<>();
        for (TableColumnar table : tables)
        {
            table.getColumnNames(columnNames);
        }

        List<Column> newColumns = new ArrayList<>();
        for (ColumnName columnName : columnNames)
        {
            List<Column> columns = new ArrayList<>();
            for (TableColumnar table : tables)
            {
                Column column = table.get(columnName);
                if (column == null)
                {
                    throw new RuntimeException("expected all tables to have same columns");
                }
                columns.add(column);
            }
            Column newColumn = Columns.concat(columns);
            newColumns.add(newColumn);
        }
        if (tableName == null)
        {
            tableName = TableName.of("Concat");
        }
        return Tables.newTable(tableName, tableDescription, newColumns);
    }

    public static TableColumnar select(TableColumnar table, Selection selection)
    {
        ViewIndex.Builder viewIndexBuilder = ViewIndex.builder();
        for (int i = 0; i < table.getRowCount(); ++i)
        {
            if (selection.get(i))
            {
                viewIndexBuilder.add(i);
            }
        }

        TableDescription desc = new TableDescription(
                table.getDescription().getValue() + " Where " + selection.getName());
        TableColumnar ret = Tables.newTableView(table.getName(), desc, table, viewIndexBuilder.build());
        return ret;
    }

    public static TableColumnar leftOuterJoin(TableColumnar left, TableColumnar right, ColumnName leftKey,
            ColumnName rightKey, ColumnName... rightColumnsToAdd)
    {
        ArgumentCheck.nonNull(left);
        ArgumentCheck.nonNull(right);
        ArgumentCheck.nonNull(leftKey);
        ArgumentCheck.nonNull(rightKey);
        ArgumentCheck.nonEmpty(rightColumnsToAdd);
        return leftOuterJoin(left, right, new ColumnName[]
        {leftKey}, new ColumnName[]
        {rightKey}, rightColumnsToAdd);
    }

    public static TableColumnar leftOuterJoin(TableColumnar left, TableColumnar right, ColumnName[] leftKeys,
            ColumnName[] rightKeys, ColumnName... rightColumnsToAdd)
    {
        ArgumentCheck.nonNull(left);
        ArgumentCheck.nonNull(right);
        ArgumentCheck.nonEmpty(leftKeys);
        ArgumentCheck.nonEmpty(rightKeys);
        ArgumentCheck.nonEmpty(rightColumnsToAdd);
        if (leftKeys.length != rightKeys.length)
        {
            throw new IllegalArgumentException("leftKeys and rightKeys must have the same length");
        }

        boolean allObjectColumns = true;
        for (ColumnName rightColumnName : rightColumnsToAdd)
        {
            Column column = right.get(rightColumnName);
            if (!(column instanceof ColumnObject<?>))
            {
                allObjectColumns = false;
                break;
            }
        }
        if (allObjectColumns)
        {
            ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
            TableIndex rightIndex = new TableIndex(right, rightKeys);
            Column[] leftKeyColumns = left.getColumns(leftKeys);
            for (int i = 0; i < left.getRowCount(); ++i)
            {
                Object[] key = new Object[leftKeyColumns.length];
                for (int j = 0; j < leftKeyColumns.length; ++j)
                {
                    Column column = leftKeyColumns[j];
                    if (column instanceof ColumnObject<?> co)
                    {
                        key[j] = co.get(i);
                    }
                    else
                    {
                        throw new RuntimeException(
                                "column " + column.getName() + " not columnobject, cannot form a join key");
                    }
                }

                Integer matchIndex = rightIndex.index(key);
                if (matchIndex != null)
                {
                    rowIndexBuilder.add(matchIndex.intValue());
                }
                else
                {
                    rowIndexBuilder.addNull();
                }
            }
            return new TableColumnarLeftOuterJoin(left.getName(), left.getDescription(), left, right,
                    rowIndexBuilder.build(), rightColumnsToAdd);
        }

        TableIndex rightIndex = new TableIndex(right, rightKeys);
        Column[] rightColumns = right.getColumns(rightColumnsToAdd);
        Column.Builder[] builders = new Column.Builder[rightColumns.length];
        for (int i = 0; i < rightColumns.length; ++i)
        {
            builders[i] = Columns.newColumn(rightColumns[i].getName(), rightColumns[i].getType());
        }

        Column[] leftKeyColumns = left.getColumns(leftKeys);
        for (int i = 0; i < left.getRowCount(); ++i)
        {
            Object[] key = new Object[leftKeyColumns.length];
            for (int j = 0; j < leftKeyColumns.length; ++j)
            {
                Column column = leftKeyColumns[j];
                if (column instanceof ColumnObject<?> co)
                {
                    key[j] = co.get(i);
                }
                else
                {
                    throw new RuntimeException(
                            "column " + column.getName() + " not columnobject, cannot form a join key");
                }
            }

            Integer matchIndex = rightIndex.index(key);
            for (int j = 0; j < rightColumns.length; ++j)
            {
                addJoinedValue(builders[j], rightColumns[j], matchIndex);
            }
        }

        Map<ColumnName, Column> columnsByName = new LinkedHashMap<>();
        left.getColumns(columnsByName);
        for (Column.Builder builder : builders)
        {
            Column column = builder.build();
            columnsByName.put(column.getName(), column);
        }
        return Tables.newTable(left.getName(), left.getDescription(), columnsByName.values());
    }

    private static void addJoinedValue(Column.Builder builder, Column source, Integer matchIndex)
    {
        if (source instanceof ColumnObject<?> co && builder instanceof ColumnObject.Builder<?> coBuilder)
        {
            addJoinedObjectValue(coBuilder, co, matchIndex);
        }
        else if (source instanceof ColumnInt ci && builder instanceof ColumnInt.Builder ciBuilder)
        {
            if (matchIndex != null && ci.isSet(matchIndex.intValue()))
            {
                ciBuilder.add(ci.get(matchIndex.intValue()));
            }
            else
            {
                ciBuilder.addNull();
            }
        }
        else if (source instanceof ColumnLong cl && builder instanceof ColumnLong.Builder clBuilder)
        {
            if (matchIndex != null && cl.isSet(matchIndex.intValue()))
            {
                clBuilder.add(cl.get(matchIndex.intValue()));
            }
            else
            {
                clBuilder.addNull();
            }
        }
        else if (source instanceof ColumnDouble cd && builder instanceof ColumnDouble.Builder cdBuilder)
        {
            if (matchIndex != null && cd.isSet(matchIndex.intValue()))
            {
                cdBuilder.add(cd.get(matchIndex.intValue()));
            }
            else
            {
                cdBuilder.addNull();
            }
        }
        else
        {
            throw new IllegalArgumentException("Unsupported joined column type " + source.getType());
        }
    }

    @SuppressWarnings(
    {"rawtypes", "unchecked"})
    private static void addJoinedObjectValue(ColumnObject.Builder builder, ColumnObject source, Integer matchIndex)
    {
        if (matchIndex != null && source.isSet(matchIndex.intValue()))
        {
            builder.add(source.get(matchIndex.intValue()));
        }
        else
        {
            builder.addNull();
        }
    }

    public static TableColumnar newTable(TableName tableName, TableDescription desc, Column[] firstColumns,
            Column[] lastColumns)
    {
        ArgumentCheck.nonEmpty(firstColumns);
        List<Column> columns = new ArrayList<>();
        Collections.addAll(columns, firstColumns);
        if (!Is.empty(lastColumns))
        {
            Collections.addAll(columns, lastColumns);
        }
        return new TableColumnarMap(tableName, desc, columns.toArray(new Column[columns.size()]));
    }

    public static TableColumnar newTable(TableName tableName, TableDescription description,
            Column.Builder... columnBuilders)
    {
        ArgumentCheck.nonEmpty(columnBuilders);
        Column[] columns = new Column[columnBuilders.length];
        for (int i = 0; i < columnBuilders.length; ++i)
        {
            columns[i] = columnBuilders[i].build();
        }
        return new TableColumnarMap(tableName, description, columns);
    }

    public static TableColumnar newTable(TableName tableName, TableDescription description, Column... columns)
    {
        ArgumentCheck.nonEmpty(columns);
        return new TableColumnarMap(tableName, description, columns);
    }

    public static TableColumnar newTable(TableName tableName, Column... columns)
    {
        ArgumentCheck.nonEmpty(columns);
        return new TableColumnarMap(tableName, null, columns);
    }

    public static TableColumnar newTable(TableName tableName, Column.Builder... columnBuilders)
    {
        ArgumentCheck.nonEmpty(columnBuilders);
        return newTable(tableName, null, columnBuilders);
    }

    public static TableColumnar newTable(TableName tableName, Collection<Column> columns)
    {
        ArgumentCheck.nonEmpty(columns);
        return new TableColumnarMap(tableName, columns.toArray(new Column[columns.size()]));
    }

    public static TableColumnar newTable(TableName tableName, TableDescription description, Collection<Column> columns)
    {
        ArgumentCheck.nonEmpty(columns);
        return new TableColumnarMap(tableName, description, columns.toArray(new Column[columns.size()]));
    }

    public static TableColumnar newTableView(TableName viewTableName, TableColumnar table, ViewIndex rowIndex)
    {
        ArgumentCheck.nonNull(table);
        return new TableColumnarView(viewTableName, table, rowIndex);
    }

    public static TableColumnar newTableView(TableName viewTableName, TableDescription viewTableDescription,
            TableColumnar table, ViewIndex rowIndex)
    {
        ArgumentCheck.nonNull(table);
        return new TableColumnarView(viewTableName, viewTableDescription, table, rowIndex);
    }

}
