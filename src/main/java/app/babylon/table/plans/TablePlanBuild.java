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
import app.babylon.table.io.TabularReader;
import app.babylon.table.io.TabularReaderCsv;
import app.babylon.table.transform.Transform;

public class TablePlanBuild implements TablePlan
{
    private final List<Transform> transforms;
    private final Map<ColumnName, Column.Type> columnTypes;
    private TableName outputTableName;
    private TableDescription outputTableDescription;

    public TablePlanBuild()
    {
        this.transforms = new ArrayList<>();
        this.columnTypes = new LinkedHashMap<>();
        this.outputTableName = null;
        this.outputTableDescription = null;
    }

    public TablePlanBuild withOutputTableName(TableName outputTableName)
    {
        this.outputTableName = outputTableName;
        return this;
    }

    public TableName getOutputTableName()
    {
        return this.outputTableName;
    }

    public TablePlanBuild withOutputTableDescription(TableDescription outputTableDescription)
    {
        this.outputTableDescription = outputTableDescription;
        return this;
    }

    public TableDescription getOutputTableDescription()
    {
        return this.outputTableDescription;
    }

    public TablePlanBuild withTransform(Transform transform)
    {
        if (transform != null)
        {
            this.transforms.add(transform);
        }
        return this;
    }

    public TablePlanBuild withTransforms(Transform... transforms)
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

    public TablePlanBuild withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
        return this;
    }

    public TablePlanBuild withColumnType(ColumnName columnName, Class<?> valueClass)
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
        TableName effectiveName = tableName == null ? this.outputTableName : tableName;
        if (effectiveName == null)
        {
            throw new IllegalArgumentException("TablePlanBuild.execute requires a table name.");
        }
        TableDescription effectiveDescription = tableDescription == null
                ? this.outputTableDescription
                : tableDescription;
        TableColumnar table = Tables.newTable(effectiveName, effectiveDescription,
                Arrays.copyOf(columns, columns.length));
        return apply(table);
    }

    public TableColumnar execute(TableColumnar table)
    {
        return apply(ArgumentCheck.nonNull(table));
    }

    @Override
    public TableColumnar execute(DataSource dataSource, TabularReader reader)
    {
        ArgumentCheck.nonNull(dataSource);
        TabularReader checkedReader = ArgumentCheck.nonNull(reader);
        RowConsumerCreateTable rowConsumer = RowConsumerCreateTable.create(tableName(checkedReader),
                resourceName(checkedReader), this.columnTypes);
        TabularReader.Result readResult = checkedReader.withRowConsumer(rowConsumer).read(dataSource);
        TableColumnar parsed = getTable(readResult);
        return apply(parsed);
    }

    private static TableColumnar getTable(TabularReader.Result readResult)
    {
        if (readResult.hasTable())
        {
            return readResult.getTable();
        }
        if (readResult.getCause() instanceof RuntimeException runtimeException)
        {
            throw runtimeException;
        }
        throw new TableException(readResult.getMessage());
    }

    private static TableName tableName(TabularReader reader)
    {
        if (reader instanceof TabularReaderCsv csvReader)
        {
            return csvReader.getTableName();
        }
        return null;
    }

    private static ColumnName resourceName(TabularReader reader)
    {
        if (reader instanceof TabularReaderCsv csvReader)
        {
            return csvReader.getResourceName();
        }
        return null;
    }

    private TableColumnar apply(TableColumnar table)
    {
        TableColumnar result = table;
        if (!this.transforms.isEmpty())
        {
            result = result.apply(this.transforms);
        }

        TableName effectiveName = this.outputTableName == null ? result.getName() : this.outputTableName;
        TableDescription effectiveDescription = this.outputTableDescription == null
                ? result.getDescription()
                : this.outputTableDescription;
        if (effectiveName.equals(result.getName()) && effectiveDescription.equals(result.getDescription()))
        {
            return result;
        }
        return Tables.newTable(effectiveName, effectiveDescription, result.getColumns());
    }
}
