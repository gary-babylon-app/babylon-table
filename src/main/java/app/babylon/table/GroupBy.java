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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class GroupBy
{
    private final ColumnName[] groupBys;
    private final Column.Type[] groupByTypes;

    private final Map<GroupKey, TableColumnar> groupedTables;

    public GroupBy(Column.Type[] groupByTypes, ColumnName[] groupBys, Map<GroupKey, TableColumnar> groupedTables)
    {
        this.groupByTypes = Arrays.copyOf(groupByTypes, groupByTypes.length);
        this.groupBys = Arrays.copyOf(groupBys, groupBys.length);
        this.groupedTables = new HashMap<>(groupedTables);

    }

    public Map<GroupKey, TableColumnar> getGroupedTables(Map<GroupKey, TableColumnar> x)
    {
        if (x == null)
        {
            x = new HashMap<>();
        }
        x.putAll(this.groupedTables);
        return x;
    }

    @SuppressWarnings("unchecked")
    public TableColumnar aggregate(TableName tableName, ColumnName[] columnNames, Aggregate[] aggregates)
    {

        ColumnObject.Builder<Object>[] groupColumns = new ColumnObject.Builder[groupBys.length];
        for (int i = 0; i < groupColumns.length; ++i)
        {
            groupColumns[i] = (ColumnObject.Builder<Object>) Columns.newColumn(groupBys[i], groupByTypes[i]);
        }

        ColumnBuilder[] aggregateColumns = new ColumnBuilder[columnNames.length];
        TableColumnar sampleGroup = groupedTables.values().stream().findFirst().orElse(null);
        for (int i = 0; i < columnNames.length; ++i)
        {
            aggregateColumns[i] = newAggregateBuilder(sampleGroup, columnNames[i]);
        }

        for (Entry<GroupKey, TableColumnar> e : groupedTables.entrySet())
        {
            GroupKey groupKey = e.getKey();
            TableColumnar groupTable = e.getValue();
            for (int i = 0; i < groupBys.length; ++i)
            {
                groupColumns[i].add(groupKey.getComponent(i));
            }

            for (int i = 0; i < columnNames.length; ++i)
            {
                addAggregateValue(aggregateColumns[i], groupTable.get(columnNames[i]), aggregates[i]);
            }
        }
        if (tableName == null)
        {
            tableName = TableName.of("Summary");
        }

        Column[] builtGroupColumns = new Column[groupColumns.length];
        for (int i = 0; i < groupColumns.length; ++i)
        {
            builtGroupColumns[i] = groupColumns[i].build();
        }
        Column[] builtAggregateColumns = new Column[aggregateColumns.length];
        for (int i = 0; i < aggregateColumns.length; ++i)
        {
            builtAggregateColumns[i] = aggregateColumns[i].build();
        }
        TableDescription description = new TableDescription("");
        TableColumnar table = Tables.newTable(tableName, description, builtGroupColumns, builtAggregateColumns);
        return table;
    }

    private static ColumnBuilder newAggregateBuilder(TableColumnar sampleGroup, ColumnName columnName)
    {
        if (sampleGroup != null)
        {
            Column sourceColumn = sampleGroup.get(columnName);
            if (sourceColumn instanceof ColumnDouble)
            {
                return ColumnDouble.builder(columnName);
            }
        }
        return ColumnObject.builderDecimal(columnName);
    }

    private static void addAggregateValue(ColumnBuilder builder, Column sourceColumn, Aggregate aggregate)
    {
        if (builder instanceof ColumnDouble.Builder doubleBuilder && sourceColumn instanceof ColumnDouble sourceDouble)
        {
            doubleBuilder.add(Columns.aggregate(sourceDouble, aggregate));
            return;
        }
        if (builder instanceof ColumnObject.Builder<?> objectBuilder && sourceColumn instanceof ColumnObject<?>)
        {
            @SuppressWarnings("unchecked")
            ColumnObject.Builder<BigDecimal> decimalBuilder = (ColumnObject.Builder<BigDecimal>) objectBuilder;
            @SuppressWarnings("unchecked")
            ColumnObject<BigDecimal> decimalColumn = (ColumnObject<BigDecimal>) sourceColumn;
            decimalBuilder.add(Columns.aggregate(decimalColumn, aggregate));
            return;
        }
        throw new IllegalArgumentException("Unsupported aggregate source column type for " + sourceColumn.getName()
                + ": " + sourceColumn.getClass().getSimpleName());
    }
}
