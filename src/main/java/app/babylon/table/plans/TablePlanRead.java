/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.plans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.babylon.io.DataSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableException;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.io.RowConsumerCreateTable;
import app.babylon.table.io.TabularRowReader;
import app.babylon.table.transform.Transform;

public class TablePlanRead extends TablePlanCommon<TablePlanRead>
{
    private final List<Transform> transforms;
    private final Map<ColumnName, Column.Type> columnTypes;
    private ColumnName dataSourceNameColumnName;

    public TablePlanRead()
    {
        this.transforms = new ArrayList<>();
        this.columnTypes = new LinkedHashMap<>();
        this.dataSourceNameColumnName = null;
    }

    public TablePlanRead withIncludeResourceName(ColumnName resourceName)
    {
        this.dataSourceNameColumnName = ArgumentCheck.nonNull(resourceName);
        return this;
    }

    public ColumnName getResourceName()
    {
        return this.dataSourceNameColumnName;
    }

    public TablePlanRead withTransform(Transform transform)
    {
        if (transform != null)
        {
            this.transforms.add(transform);
        }
        return this;
    }

    public TablePlanRead withTransforms(Transform... transforms)
    {
        if (transforms != null)
        {
            for (Transform transform : transforms)
            {
                withTransform(transform);
            }
        }
        return this;
    }

    public List<Transform> getTransforms()
    {
        return Collections.unmodifiableList(this.transforms);
    }

    public TablePlanRead withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
        return this;
    }

    public TablePlanRead withColumnType(ColumnName columnName, Class<?> valueClass)
    {
        return withColumnType(columnName, Column.Type.of(ArgumentCheck.nonNull(valueClass)));
    }

    public Column.Type getColumnType(ColumnName columnName)
    {
        return this.columnTypes.get(columnName);
    }

    public Map<ColumnName, Column.Type> getColumnTypes()
    {
        return Collections.unmodifiableMap(this.columnTypes);
    }

    public TableColumnar execute(TableName tableName, Column... columns)
    {
        return execute(tableName, null, columns);
    }

    public TableColumnar execute(TableName tableName, TableDescription tableDescription, Column... columns)
    {
        ArgumentCheck.nonEmpty(columns);
        TableName effectiveName = tableName == null ? getTableName() : tableName;
        if (effectiveName == null)
        {
            throw new IllegalArgumentException("TablePlanRead.execute requires a table name.");
        }
        TableDescription effectiveDescription = tableDescription == null ? getTableDescription() : tableDescription;
        TableColumnar table = Tables.newTable(effectiveName, effectiveDescription,
                Arrays.copyOf(columns, columns.length));
        return apply(table);
    }

    public TableColumnar execute(TableColumnar table)
    {
        return apply(ArgumentCheck.nonNull(table));
    }

    @Override
    public TableColumnar execute(DataSource dataSource, TabularRowReader reader)
    {
        DataSource checkedDataSource = ArgumentCheck.nonNull(dataSource);
        TabularRowReader checkedReader = ArgumentCheck.nonNull(reader);
        RowConsumerCreateTable rowConsumer = RowConsumerCreateTable.create(getTableName(), getTableDescription(),
                this.dataSourceNameColumnName, checkedDataSource.getName(), this.columnTypes);
        TabularRowReader.Result readResult = checkedReader.read(checkedDataSource, rowConsumer);
        ensureSuccess(readResult);
        TableColumnar parsed = rowConsumer.build();
        return apply(parsed);
    }

    private static void ensureSuccess(TabularRowReader.Result readResult)
    {
        if (readResult.isSuccessLike())
        {
            return;
        }
        if (readResult.getCause() instanceof RuntimeException runtimeException)
        {
            throw runtimeException;
        }
        throw new TableException(readResult.getMessage());
    }

    private TableColumnar apply(TableColumnar table)
    {
        TableColumnar result = table;
        if (!this.transforms.isEmpty())
        {
            result = result.apply(this.transforms);
        }

        TableName effectiveName = getTableName() == null ? result.getName() : getTableName();
        TableDescription effectiveDescription = getTableDescription() == null
                ? result.getDescription()
                : getTableDescription();
        if (effectiveName.equals(result.getName()) && effectiveDescription.equals(result.getDescription()))
        {
            return result;
        }
        return Tables.newTable(effectiveName, effectiveDescription, result.getColumns());
    }

    @Override
    protected TablePlanRead self()
    {
        return this;
    }
}
