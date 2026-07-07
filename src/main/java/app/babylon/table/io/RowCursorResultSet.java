/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Arrays;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.text.Strings;

/**
 * Supplies rows from a caller-owned {@link ResultSet}.
 * <p>
 * This supplier adapts a caller-owned {@link ResultSet}. Closing the supplier
 * is a no-op; the caller remains responsible for the lifecycle of the
 * underlying JDBC resources.
 * <p>
 * Create it around a live {@code ResultSet}, call {@link #columns()} once if
 * column metadata is needed, then advance with {@link #next()} and read each
 * row through {@link #current()}.
 * <p>
 * This implementation is a demonstration of what is possible and the likely
 * structure given the ResultSet API. Different drivers may still have quirks
 * that require special implementations.
 *
 */
public class RowCursorResultSet implements RowCursor
{
    private final ResultSet resultSet;
    private final RowResultSet row;
    private final boolean closeOnClose;
    private ColumnDefinition[] columns;
    private boolean currentAvailable;

    public RowCursorResultSet(ResultSet resultSet)
    {
        this(resultSet, false);
    }

    RowCursorResultSet(ResultSet resultSet, boolean closeOnClose)
    {
        this.resultSet = ArgumentCheck.nonNull(resultSet);
        this.closeOnClose = closeOnClose;
        this.columns = resolveColumns(resultSet);
        this.row = new RowResultSet();
        this.currentAvailable = false;
    }

    static RowCursorResultSet open(PreparedStatement preparedStatement) throws SQLException
    {
        return new RowCursorResultSet(ArgumentCheck.nonNull(preparedStatement).executeQuery(), true);
    }

    @Override
    public ColumnDefinition[] columns()
    {
        return Arrays.copyOf(this.columns, this.columns.length);
    }

    @Override
    public boolean next()
    {
        try
        {
            if (!this.resultSet.next())
            {
                this.currentAvailable = false;
                return false;
            }
            this.row.reset();
            this.currentAvailable = true;
            return true;
        }
        catch (SQLException e)
        {
            throw new TableException("Failed to advance ResultSet row cursor.", e);
        }
    }

    @Override
    public RowValues current()
    {
        if (!this.currentAvailable)
        {
            throw new IllegalStateException("current row is not available until next() succeeds");
        }
        return this.row.current();
    }

    @Override
    public void close() throws SQLException
    {
        if (this.closeOnClose)
        {
            this.resultSet.close();
        }
    }

    private static ColumnDefinition[] resolveColumns(ResultSet resultSet)
    {
        try
        {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            ColumnDefinition[] definitions = new ColumnDefinition[columnCount];
            for (int i = 1; i <= columnCount; ++i)
            {
                ColumnName name = ColumnName.of(resolveHeaderName(metaData, i));
                Column.Type type = resolveColumnType(metaData, i);
                definitions[i - 1] = new ColumnDefinition(name, type);
            }
            return definitions;
        }
        catch (SQLException e)
        {
            throw new TableException("Failed to resolve ResultSet metadata.", e);
        }
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
        return switch (metaData.getColumnType(columnIndex))
        {
            case Types.TINYINT -> ColumnTypes.BYTE;
            case Types.SMALLINT, Types.INTEGER -> ColumnTypes.INT;
            case Types.BIGINT -> ColumnTypes.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> ColumnTypes.DOUBLE;
            case Types.NUMERIC, Types.DECIMAL -> ColumnTypes.DECIMAL;
            case Types.DATE -> ColumnTypes.LOCALDATE;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR,
                    Types.CLOB, Types.NCLOB ->
                ColumnTypes.STRING;
            default -> null;
        };
    }

    private final class RowResultSet
    {
        private final ResultSetRow row;

        private RowResultSet()
        {
            this.row = new ResultSetRow();
        }

        private void reset()
        {
        }

        private RowValues current()
        {
            return this.row;
        }
    }

    private final class ResultSetRow implements RowValues
    {
        @Override
        public int size()
        {
            return RowCursorResultSet.this.columns.length;
        }

        @Override
        public boolean isEmpty()
        {
            for (int i = 0; i < size(); ++i)
            {
                if (!Strings.isEmpty(getString(i)))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isSet(int fieldIndex)
        {
            return !Strings.isEmpty(getString(fieldIndex));
        }

        @Override
        public String getString(int fieldIndex)
        {
            try
            {
                return RowCursorResultSet.this.resultSet.getString(fieldIndex + 1);
            }
            catch (SQLException e)
            {
                throw new TableException("Failed to read ResultSet row text.", e);
            }
        }

        @Override
        public RowValues select(int[] selectedIndexes, boolean strip)
        {
            String[] values = new String[selectedIndexes.length];
            for (int i = 0; i < selectedIndexes.length; ++i)
            {
                String value = getString(selectedIndexes[i]);
                if (strip && !Strings.isEmpty(value))
                {
                    CharSequence stripped = Strings.stripx(value);
                    value = Strings.isEmpty(stripped) ? null : stripped.toString();
                }
                values[i] = value;
            }
            return new StringArrayRow(values);
        }

        @Override
        public void addTo(Column.Builder builder, int fieldIndex)
        {
            try
            {
                if (builder instanceof ColumnObject.Builder<?> objectBuilder)
                {
                    addObject(objectBuilder, fieldIndex);
                }
                else if (builder instanceof ColumnDouble.Builder doubleBuilder)
                {
                    addDouble(doubleBuilder, fieldIndex);
                }
                else if (builder instanceof ColumnLong.Builder longBuilder)
                {
                    addLong(longBuilder, fieldIndex);
                }
                else if (builder instanceof ColumnInt.Builder intBuilder)
                {
                    addInt(intBuilder, fieldIndex);
                }
                else
                {
                    addText(builder, fieldIndex);
                }
            }
            catch (SQLException e)
            {
                throw new TableException("Failed to read ResultSet row value.", e);
            }
        }

        private void addObject(ColumnObject.Builder<?> builder, int fieldIndex) throws SQLException
        {
            Object value = objectValue(builder, fieldIndex);
            if (value == null)
            {
                builder.addNull();
            }
            else if (builder.getType().getValueClass().isInstance(value))
            {
                addObjectValue(builder, value);
            }
            else
            {
                String text = value.toString();
                builder.add(text, 0, text.length());
            }
        }

        private void addDouble(ColumnDouble.Builder builder, int fieldIndex) throws SQLException
        {
            double value = RowCursorResultSet.this.resultSet.getDouble(fieldIndex + 1);
            if (RowCursorResultSet.this.resultSet.wasNull())
            {
                builder.addNull();
            }
            else
            {
                builder.add(value);
            }
        }

        private void addLong(ColumnLong.Builder builder, int fieldIndex) throws SQLException
        {
            long value = RowCursorResultSet.this.resultSet.getLong(fieldIndex + 1);
            if (RowCursorResultSet.this.resultSet.wasNull())
            {
                builder.addNull();
            }
            else
            {
                builder.add(value);
            }
        }

        private void addInt(ColumnInt.Builder builder, int fieldIndex) throws SQLException
        {
            int value = RowCursorResultSet.this.resultSet.getInt(fieldIndex + 1);
            if (RowCursorResultSet.this.resultSet.wasNull())
            {
                builder.addNull();
            }
            else
            {
                builder.add(value);
            }
        }

        private void addText(Column.Builder builder, int fieldIndex) throws SQLException
        {
            String value = RowCursorResultSet.this.resultSet.getString(fieldIndex + 1);
            if (Strings.isEmpty(value))
            {
                builder.addNull();
            }
            else
            {
                builder.add(value, 0, value.length());
            }
        }

        private Object objectValue(ColumnObject.Builder<?> builder, int fieldIndex) throws SQLException
        {
            Class<?> valueClass = builder.getType().getValueClass();
            if (LocalDate.class.equals(valueClass))
            {
                return RowCursorResultSet.this.resultSet.getObject(fieldIndex + 1, LocalDate.class);
            }
            if (String.class.equals(valueClass))
            {
                return RowCursorResultSet.this.resultSet.getString(fieldIndex + 1);
            }
            return RowCursorResultSet.this.resultSet.getObject(fieldIndex + 1);
        }

        @SuppressWarnings(
        {"rawtypes", "unchecked"})
        private void addObjectValue(ColumnObject.Builder<?> builder, Object value)
        {
            ColumnObject.Builder raw = builder;
            raw.add(value);
        }
    }
}
