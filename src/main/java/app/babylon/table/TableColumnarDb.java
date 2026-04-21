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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

/**
 * Materialised table built directly from a caller-owned {@link ResultSet}.
 */
public final class TableColumnarDb extends TableColumnarMap
{
    private TableColumnarDb(TableName tableName, TableDescription description, Column[] columns)
    {
        super(tableName, description, columns);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private TableName tableName;
        private TableDescription tableDescription;
        private ResultSet resultSet;
        private final Set<ColumnName> selectedColumns;
        private final Map<ColumnName, Column.Type> explicitColumnTypes;

        private Builder()
        {
            this.tableName = null;
            this.tableDescription = null;
            this.resultSet = null;
            this.selectedColumns = new LinkedHashSet<>();
            this.explicitColumnTypes = new LinkedHashMap<>();
        }

        public Builder withTableName(TableName tableName)
        {
            this.tableName = ArgumentCheck.nonNull(tableName);
            return this;
        }

        public Builder withTableDescription(TableDescription tableDescription)
        {
            this.tableDescription = tableDescription;
            return this;
        }

        public Builder withResultSet(ResultSet resultSet)
        {
            this.resultSet = ArgumentCheck.nonNull(resultSet);
            return this;
        }

        public Builder withSelectedColumn(ColumnName columnName)
        {
            this.selectedColumns.add(ArgumentCheck.nonNull(columnName));
            return this;
        }

        public Builder withSelectedColumns(ColumnName... columnNames)
        {
            if (columnNames != null)
            {
                this.selectedColumns.addAll(Arrays.asList(columnNames));
            }
            return this;
        }

        public Builder withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.explicitColumnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
            return this;
        }

        public Builder withColumnTypes(Map<ColumnName, Column.Type> columnTypes)
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

        public TableColumnarDb build()
        {
            TableName checkedTableName = ArgumentCheck.nonNull(this.tableName);
            ResultSet checkedResultSet = ArgumentCheck.nonNull(this.resultSet);
            try
            {
                ResolvedColumn[] resolvedColumns = resolveColumns(checkedResultSet, this.selectedColumns,
                        this.explicitColumnTypes);
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
                        addValue(checkedResultSet, resolvedColumns[i].sourceIndex(), columnDefinition.type(),
                                builders[i]);
                    }
                }

                Column[] columns = new Column[builders.length];
                for (int i = 0; i < builders.length; ++i)
                {
                    columns[i] = buildColumn(builders[i], resolvedColumns[i]);
                }
                return new TableColumnarDb(checkedTableName, this.tableDescription, columns);
            }
            catch (SQLException e)
            {
                throw new TableException("Failed to read table from ResultSet.", e);
            }
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
        private static void addValue(ResultSet resultSet, int columnIndex, Column.Type columnType,
                Column.Builder builder) throws SQLException
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
                        + " for column " + targetDefinition.name()
                        + " is only supported for STRING source columns, not " + sourceDefinition.type());
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
            throw new IllegalArgumentException("Explicit ResultSet column type " + targetDefinition.type()
                    + " for column " + targetDefinition.name() + " is not supported from source type "
                    + sourceDefinition.type());
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
    }

    private record ResolvedColumn(int sourceIndex, ColumnDefinition sourceDefinition, ColumnDefinition targetDefinition)
    {
    }
}
