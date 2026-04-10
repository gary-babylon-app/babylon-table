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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.babylon.io.DataSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.io.Csv;
import app.babylon.table.io.HeaderStrategyAuto;
import app.babylon.table.io.RowConsumerTableCreator;
import app.babylon.table.transform.Transform;

public class TableBuildPlan
{
    private final List<Transform> transforms;
    private final Map<ColumnName, Column.Type> columnTypes;
    private TableName outputTableName;
    private TableDescription outputTableDescription;

    public TableBuildPlan()
    {
        this.transforms = new ArrayList<>();
        this.columnTypes = new LinkedHashMap<>();
        this.outputTableName = null;
        this.outputTableDescription = null;
    }

    public TableBuildPlan withOutputTableName(TableName outputTableName)
    {
        this.outputTableName = outputTableName;
        return this;
    }

    public TableName getOutputTableName()
    {
        return this.outputTableName;
    }

    public TableBuildPlan withOutputTableDescription(TableDescription outputTableDescription)
    {
        this.outputTableDescription = outputTableDescription;
        return this;
    }

    public TableDescription getOutputTableDescription()
    {
        return this.outputTableDescription;
    }

    public TableBuildPlan withTransform(Transform transform)
    {
        if (transform != null)
        {
            this.transforms.add(transform);
        }
        return this;
    }

    public TableBuildPlan withTransforms(Transform... transforms)
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

    public TableBuildPlan withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
        return this;
    }

    public TableBuildPlan withColumnType(ColumnName columnName, Class<?> valueClass)
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
            throw new IllegalArgumentException("TableBuildPlan.execute requires a table name.");
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

    public TableColumnar execute(DataSource dataSource, Csv.ReadSettings settings)
    {
        return execute(dataSource, settings, new HeaderStrategyAuto());
    }

    public TableColumnar execute(DataSource dataSource, Csv.ReadSettings settings,
            app.babylon.table.io.HeaderStrategy headerStrategy)
    {
        ArgumentCheck.nonNull(dataSource);
        Csv.ReadSettings effectiveSettings = settings == null ? new Csv.ReadSettings() : settings;
        TableColumnar parsed = Csv.read(dataSource, effectiveSettings, headerStrategy,
                RowConsumerTableCreator.create(effectiveSettings, this.columnTypes));
        return apply(parsed);
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
