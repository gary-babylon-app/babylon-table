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

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableException;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.Columns;
import app.babylon.table.io.RowConsumerCreateTable;
import app.babylon.table.io.RowCursor;
import app.babylon.table.io.RowSource;
import app.babylon.table.transform.DateFormat;
import app.babylon.table.transform.Transform;
import app.babylon.text.Strings;

/**
 * Reads rows into a {@link TableColumnar}.
 * <p>
 * Example:
 *
 * <pre>{@code
 * StreamSource streamSource = StreamSources.fromString("Code,Amount\nabc,10.5\n", "values.csv");
 * RowSource rowSource = RowSourceCsv.builder().withStreamSource(streamSource).withSeparator(',').build();
 *
 * TableColumnar table = new TablePlanRead().withTableName(TableName.of("Trades")).execute(rowSource);
 * }</pre>
 * <p>
 * There are two useful layers for column typing when reading:
 * <p>
 * - source-side types from {@link RowCursor#columns()} choose the low-level
 * builder before rows are consumed and are therefore the best place to avoid
 * intermediate string materialization when that direct parser is genuinely
 * worthwhile
 * <p>
 * - plan-side types configured on this class describe the desired output type
 * and can override the source-side type when appropriate
 */
public class TablePlanRead extends TablePlanCommon<TablePlanRead>
{
    private final List<Transform> transforms;
    private final Map<ColumnName, Column.Type> columnTypes;
    private ColumnName dataSourceNameColumnName;
    private DateFormat localDateFormat;

    public TablePlanRead()
    {
        this.transforms = new ArrayList<>();
        this.columnTypes = new LinkedHashMap<>();
        this.dataSourceNameColumnName = null;
        this.localDateFormat = null;
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

    public TablePlanRead withLocalDateFormat(DateFormat localDateFormat)
    {
        this.localDateFormat = localDateFormat;
        return this;
    }

    public DateFormat getLocalDateFormat()
    {
        return this.localDateFormat;
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

    /**
     * Specifies the desired output type for a column in the resulting table.
     * <p>
     * This is best thought of as plan-side type intent. If the source cursor also
     * exposes a type for the same column, that source-side type can still matter
     * earlier because it may choose the low-level builder before rows are read.
     * <p>
     * In other words:
     * <p>
     * - source-side types are the right place to request direct builder parsing and
     * bypass intermediate strings when the parser has a real advantage
     * <p>
     * - plan-side types are the right place to describe the desired result schema
     * or override the source metadata
     *
     * @param columnName
     *            the output column name
     * @param columnType
     *            the desired output type
     * @return this plan
     */
    public TablePlanRead withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
        return this;
    }

    public TablePlanRead withColumnTypes(Column.Type columnType, ColumnName... columnNames)
    {
        Column.Type type = ArgumentCheck.nonNull(columnType);
        ColumnName[] names = ArgumentCheck.nonEmpty(columnNames);
        for (ColumnName columnName : names)
        {
            withColumnType(columnName, type);
        }
        return this;
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
    public TableColumnar execute(RowCursor rowCursor)
    {
        return execute(rowCursor, null);
    }

    @Override
    public TableColumnar execute(RowSource rowSource)
    {
        RowSource checkedRowSource = ArgumentCheck.nonNull(rowSource);
        try (RowCursor rowCursor = checkedRowSource.openRows())
        {
            return execute(rowCursor, checkedRowSource.getName());
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TableException("Failed to read rows from row source '" + checkedRowSource.getName() + "'.", e);
        }
    }

    private ColumnName[] toColumnNames(ColumnDefinition[] columnDefinitions)
    {
        ColumnName[] columnNames = new ColumnName[columnDefinitions.length];
        for (int i = 0; i < columnDefinitions.length; ++i)
        {
            columnNames[i] = columnDefinitions[i].name();
        }
        return columnNames;
    }

    private Map<ColumnName, Column.Type> sourceColumnTypes(ColumnDefinition[] columnDefinitions)
    {
        Map<ColumnName, Column.Type> source = new LinkedHashMap<>();
        for (ColumnDefinition columnDefinition : columnDefinitions)
        {
            Column.Type type = columnDefinition.type();
            if (type != null)
            {
                source.put(columnDefinition.name(), type);
            }
        }
        return source;
    }

    private TableColumnar execute(RowCursor rowCursor, String sourceName)
    {
        RowCursor checkedRowCursor = ArgumentCheck.nonNull(rowCursor);
        ColumnDefinition[] columnDefinitions = checkedRowCursor.columns();
        ColumnName[] columnNames = toColumnNames(columnDefinitions);
        Map<ColumnName, Column.Type> sourceColumnTypes = sourceColumnTypes(columnDefinitions);

        RowConsumerCreateTable rowConsumer = RowConsumerCreateTable.create(effectiveParsedTableName(sourceName),
                getTableDescription(), sourceColumnTypes, this.columnTypes, this.localDateFormat);
        rowConsumer.start(columnNames);
        while (checkedRowCursor.next())
        {
            rowConsumer.accept(checkedRowCursor.current());
        }
        TableColumnar parsed = appendResourceNameColumn(rowConsumer.build(), sourceName);
        return apply(parsed);
    }

    private TableName effectiveParsedTableName(String sourceName)
    {
        if (getTableName() != null)
        {
            return getTableName();
        }
        String extractedName = extractLastPart(sourceName);
        if (Strings.isEmpty(extractedName))
        {
            return null;
        }
        return TableName.of(extractedName);
    }

    private TableColumnar appendResourceNameColumn(TableColumnar table, String sourceName)
    {
        if (this.dataSourceNameColumnName == null)
        {
            return table;
        }
        return table.add(Columns.newString(this.dataSourceNameColumnName, sourceName, table.getRowCount()));
    }

    private static String extractLastPart(String s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        int lastSlash = s.lastIndexOf('/');
        int lastPeriod = s.lastIndexOf('.');
        if (lastPeriod >= 0 && lastPeriod > lastSlash)
        {
            return s.substring(lastSlash + 1, lastPeriod);
        }
        return s;
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
