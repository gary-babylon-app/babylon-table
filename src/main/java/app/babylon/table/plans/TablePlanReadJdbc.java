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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableException;
import app.babylon.table.Tables;
import app.babylon.table.TableName;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.table.io.RowCursor;
import app.babylon.table.io.RowSource;
import app.babylon.text.Strings;

/**
 * JDBC-specific read plan that materialises a {@link ResultSet} directly into a
 * {@link TableColumnar}.
 */
public class TablePlanReadJdbc extends TablePlanCommon<TablePlanReadJdbc>
{
    private final Set<ColumnName> selectedColumns;
    private final Map<ColumnName, Column.Type> columnTypes;

    public TablePlanReadJdbc()
    {
        this.selectedColumns = new LinkedHashSet<>();
        this.columnTypes = new LinkedHashMap<>();
    }

    public TablePlanReadJdbc withSelectedColumn(ColumnName columnName)
    {
        this.selectedColumns.add(ArgumentCheck.nonNull(columnName));
        return this;
    }

    public TablePlanReadJdbc withSelectedColumns(ColumnName... columnNames)
    {
        if (columnNames != null)
        {
            this.selectedColumns.addAll(Arrays.asList(columnNames));
        }
        return this;
    }

    public TablePlanReadJdbc withSelectedColumns(Collection<ColumnName> columnNames)
    {
        if (columnNames != null)
        {
            this.selectedColumns.addAll(columnNames);
        }
        return this;
    }

    public Set<ColumnName> getSelectedColumns()
    {
        return Collections.unmodifiableSet(this.selectedColumns);
    }

    public TablePlanReadJdbc withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
        return this;
    }

    public TablePlanReadJdbc withColumnTypes(Map<ColumnName, Column.Type> columnTypes)
    {
        if (columnTypes != null)
        {
            for (Map.Entry<ColumnName, Column.Type> entry : columnTypes.entrySet())
            {
                withColumnType(entry.getKey(), entry.getValue());
            }
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

    public TableColumnar execute(ResultSet resultSet)
    {
        TableName checkedTableName = ArgumentCheck.nonNull(getTableName(), "table name must not be null");
        ResultSet checkedResultSet = ArgumentCheck.nonNull(resultSet);
        try
        {
            ResolvedColumn[] resolvedColumns = resolveColumns(checkedResultSet, this.selectedColumns, this.columnTypes);
            Column.Builder[] builders = new Column.Builder[resolvedColumns.length];
            for (int i = 0; i < resolvedColumns.length; ++i)
            {
                builders[i] = Columns.newBuilder(resolvedColumns[i].sourceDefinition().name(),
                        resolvedColumns[i].sourceDefinition().type());
            }

            while (checkedResultSet.next())
            {
                for (int i = 0; i < resolvedColumns.length; ++i)
                {
                    ColumnDefinition columnDefinition = resolvedColumns[i].sourceDefinition();
                    addValue(checkedResultSet, resolvedColumns[i].sourceIndex(), columnDefinition.type(), builders[i]);
                }
            }

            Column[] columns = new Column[builders.length];
            for (int i = 0; i < builders.length; ++i)
            {
                columns[i] = buildColumn(builders[i], resolvedColumns[i]);
            }
            return Tables.newTable(checkedTableName, getTableDescription(), columns);
        }
        catch (SQLException e)
        {
            throw new TableException("Failed to read table from ResultSet.", e);
        }
    }

    @Override
    public TableColumnar execute(RowCursor rowCursor)
    {
        throw new UnsupportedOperationException("TablePlanReadJdbc.execute(RowCursor) is not supported.");
    }

    @Override
    public TableColumnar execute(RowSource rowSource)
    {
        throw new UnsupportedOperationException("TablePlanReadJdbc.execute(RowSource) is not supported.");
    }

    @Override
    protected TablePlanReadJdbc self()
    {
        return this;
    }

    private static ResolvedColumn[] resolveColumns(ResultSet resultSet, Set<ColumnName> selectedColumns,
            Map<ColumnName, Column.Type> explicitColumnTypes) throws SQLException
    {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        ResolvedColumn[] definitions = new ResolvedColumn[columnCount];
        int resolvedCount = 0;
        for (int i = 1; i <= columnCount; ++i)
        {
            ColumnName name = ColumnName.of(resolveHeaderName(metaData, i));
            if (!selectedColumns.isEmpty() && !selectedColumns.contains(name))
            {
                continue;
            }
            Column.Type sourceType = resolveColumnType(metaData, i);
            Column.Type targetType = explicitColumnTypes.getOrDefault(name, sourceType);
            definitions[resolvedCount] = new ResolvedColumn(i, new ColumnDefinition(name, sourceType),
                    new ColumnDefinition(name, targetType));
            resolvedCount++;
        }
        return Arrays.copyOf(definitions, resolvedCount);
    }

    private static String resolveHeaderName(ResultSetMetaData metaData, int columnIndex) throws SQLException
    {
        CharSequence label = Strings.stripx(metaData.getColumnLabel(columnIndex));
        if (!Strings.isEmpty(label))
        {
            return label.toString();
        }
        CharSequence name = Strings.stripx(metaData.getColumnName(columnIndex));
        if (!Strings.isEmpty(name))
        {
            return name.toString();
        }
        return "Column" + columnIndex;
    }

    private static Column.Type resolveColumnType(ResultSetMetaData metaData, int columnIndex) throws SQLException
    {
        int jdbcType = metaData.getColumnType(columnIndex);
        return switch (jdbcType)
        {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> ColumnTypes.INT;
            case Types.BIGINT -> ColumnTypes.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> ColumnTypes.DOUBLE;
            case Types.NUMERIC, Types.DECIMAL -> ColumnTypes.DECIMAL;
            case Types.DATE -> ColumnTypes.LOCALDATE;
            case Types.TIME -> ColumnTypes.LOCAL_TIME;
            case Types.TIMESTAMP -> ColumnTypes.LOCAL_DATE_TIME;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR ->
                ColumnTypes.STRING;
            default -> throw new IllegalArgumentException("Unsupported ResultSet column type "
                    + describeJdbcType(jdbcType) + " for column " + resolveHeaderName(metaData, columnIndex));
        };
    }

    @SuppressWarnings("unchecked")
    private static void addValue(ResultSet resultSet, int columnIndex, Column.Type columnType, Column.Builder builder)
            throws SQLException
    {
        if (ColumnTypes.INT.equals(columnType))
        {
            ColumnInt.Builder intBuilder = (ColumnInt.Builder) builder;
            int value = resultSet.getInt(columnIndex);
            if (resultSet.wasNull())
            {
                intBuilder.addNull();
            }
            else
            {
                intBuilder.add(value);
            }
            return;
        }
        if (ColumnTypes.LONG.equals(columnType))
        {
            ColumnLong.Builder longBuilder = (ColumnLong.Builder) builder;
            long value = resultSet.getLong(columnIndex);
            if (resultSet.wasNull())
            {
                longBuilder.addNull();
            }
            else
            {
                longBuilder.add(value);
            }
            return;
        }
        if (ColumnTypes.DOUBLE.equals(columnType))
        {
            ColumnDouble.Builder doubleBuilder = (ColumnDouble.Builder) builder;
            double value = resultSet.getDouble(columnIndex);
            if (resultSet.wasNull())
            {
                doubleBuilder.addNull();
            }
            else
            {
                doubleBuilder.add(value);
            }
            return;
        }
        if (ColumnTypes.DECIMAL.equals(columnType))
        {
            ColumnObject.Builder<BigDecimal> objectBuilder = (ColumnObject.Builder<BigDecimal>) builder;
            objectBuilder.add(resultSet.getBigDecimal(columnIndex));
            return;
        }
        if (ColumnTypes.LOCALDATE.equals(columnType))
        {
            ColumnObject.Builder<LocalDate> objectBuilder = (ColumnObject.Builder<LocalDate>) builder;
            objectBuilder.add(resultSet.getObject(columnIndex, LocalDate.class));
            return;
        }
        if (ColumnTypes.LOCAL_TIME.equals(columnType))
        {
            ColumnObject.Builder<LocalTime> objectBuilder = (ColumnObject.Builder<LocalTime>) builder;
            objectBuilder.add(resultSet.getObject(columnIndex, LocalTime.class));
            return;
        }
        if (ColumnTypes.LOCAL_DATE_TIME.equals(columnType))
        {
            ColumnObject.Builder<LocalDateTime> objectBuilder = (ColumnObject.Builder<LocalDateTime>) builder;
            objectBuilder.add(resultSet.getObject(columnIndex, LocalDateTime.class));
            return;
        }
        ColumnObject.Builder<String> objectBuilder = (ColumnObject.Builder<String>) builder;
        objectBuilder.add(resultSet.getString(columnIndex));
    }

    @SuppressWarnings("unchecked")
    private static Column buildColumn(Column.Builder builder, ResolvedColumn resolvedColumn)
    {
        ColumnDefinition sourceDefinition = resolvedColumn.sourceDefinition();
        ColumnDefinition targetDefinition = resolvedColumn.targetDefinition();
        if (sourceDefinition.type().equals(targetDefinition.type()))
        {
            return builder.build();
        }
        if (sourceDefinition.type().isPrimitive())
        {
            return builder.build();
        }
        if (!ColumnTypes.STRING.equals(sourceDefinition.type()))
        {
            throw new IllegalArgumentException("Explicit ResultSet column type " + targetDefinition.type()
                    + " for column " + targetDefinition.name() + " is only supported for STRING source columns, not "
                    + sourceDefinition.type());
        }
        if (targetDefinition.type().isPrimitive())
        {
            throw new IllegalArgumentException("Explicit ResultSet primitive column type " + targetDefinition.type()
                    + " for column " + targetDefinition.name() + " is not supported from STRING source columns.");
        }
        if (builder instanceof ColumnObject.Builder<?> objectBuilder)
        {
            return ((ColumnObject.Builder<Object>) objectBuilder).build(targetDefinition.type());
        }
        throw new IllegalArgumentException("Explicit ResultSet column type " + targetDefinition.type() + " for column "
                + targetDefinition.name() + " is not supported from source type " + sourceDefinition.type());
    }

    private static String describeJdbcType(int jdbcType)
    {
        return switch (jdbcType)
        {
            case Types.BINARY -> "BINARY";
            case Types.VARBINARY -> "VARBINARY";
            case Types.LONGVARBINARY -> "LONGVARBINARY";
            case Types.BLOB -> "BLOB";
            case Types.CLOB -> "CLOB";
            case Types.NCLOB -> "NCLOB";
            case Types.ARRAY -> "ARRAY";
            case Types.STRUCT -> "STRUCT";
            case Types.REF -> "REF";
            case Types.ROWID -> "ROWID";
            case Types.SQLXML -> "SQLXML";
            case Types.JAVA_OBJECT -> "JAVA_OBJECT";
            case Types.OTHER -> "OTHER";
            case Types.TIME_WITH_TIMEZONE -> "TIME_WITH_TIMEZONE";
            case Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP_WITH_TIMEZONE";
            default -> Integer.toString(jdbcType);
        };
    }

    private record ResolvedColumn(int sourceIndex, ColumnDefinition sourceDefinition, ColumnDefinition targetDefinition)
    {
    }
}
