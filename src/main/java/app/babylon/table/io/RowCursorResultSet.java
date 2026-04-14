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

import java.io.IOException;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Arrays;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;
import app.babylon.text.Strings;

/**
 * Supplies rows from a caller-owned {@link ResultSet}.
 * <p>
 * This supplier adapts a caller-owned {@link ResultSet}. Closing the supplier
 * is a no-op; the caller remains responsible for the lifecycle of the
 * underlying JDBC resources.
 * <p>
 * Example:
 *
 * <pre>{@code
 * Connection connection = ...;
 * try (PreparedStatement statement = connection
 *         .prepareStatement("select city, amount from trades where trade_date >= ?"))
 * {
 *     statement.setDate(1, java.sql.Date.valueOf("2026-01-01"));
 *     try (ResultSet resultSet = statement.executeQuery())
 *     {
 *         RowCursor supplier = new RowCursorResultSet(resultSet);
 *
 *         ColumnDefinition[] columns = supplier.columns();
 *         while (supplier.next())
 *         {
 *             Row row = supplier.current();
 *         }
 *     }
 * }
 * }</pre>
 *
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
    public Row current()
    {
        if (!this.currentAvailable)
        {
            throw new IllegalStateException("current row is not available until next() succeeds");
        }
        return this.row;
    }

    @Override
    public void close() throws SQLException
    {
        if (this.closeOnClose)
        {
            this.resultSet.close();
        }
    }

    private void appendColumnValue(RowBuffer rowBuffer, int columnIndex) throws SQLException, IOException
    {
        Reader columnValueReader = null;
        try
        {
            columnValueReader = this.resultSet.getCharacterStream(columnIndex);
        }
        catch (SQLFeatureNotSupportedException e)
        {
            appendStringValue(rowBuffer, columnIndex);
            return;
        }

        if (columnValueReader == null)
        {
            appendStringValue(rowBuffer, columnIndex);
            return;
        }

        try (Reader ignored = columnValueReader)
        {
            rowBuffer.append(columnValueReader);
        }
    }

    private void appendStringValue(RowBuffer rowBuffer, int columnIndex) throws SQLException
    {
        String value = this.resultSet.getString(columnIndex);
        if (value != null)
        {
            rowBuffer.append(value);
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
        String label = Strings.stripx(metaData.getColumnLabel(columnIndex));
        if (!Strings.isEmpty(label))
        {
            return label;
        }
        String name = Strings.stripx(metaData.getColumnName(columnIndex));
        if (!Strings.isEmpty(name))
        {
            return name;
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

    private final class RowResultSet implements Row
    {
        private final String[] stringValues;
        private final boolean[] stringLoaded;
        private RowBuffer rowBuffer;

        private RowResultSet()
        {
            int size = RowCursorResultSet.this.columns.length;
            this.stringValues = new String[size];
            this.stringLoaded = new boolean[size];
            this.rowBuffer = null;
        }

        private void reset()
        {
            Arrays.fill(this.stringValues, null);
            Arrays.fill(this.stringLoaded, false);
            if (this.rowBuffer != null)
            {
                this.rowBuffer.clear();
            }
        }

        @Override
        public int size()
        {
            return RowCursorResultSet.this.columns.length;
        }

        @Override
        public boolean isEmpty()
        {
            return ensureRowBuffer().isEmpty();
        }

        @Override
        public boolean isSet(int fieldIndex)
        {
            return length(fieldIndex) > 0;
        }

        @Override
        public char[] chars()
        {
            return ensureRowBuffer().chars();
        }

        @Override
        public int end()
        {
            return ensureRowBuffer().end();
        }

        @Override
        public int start(int fieldIndex)
        {
            return ensureRowBuffer().start(fieldIndex);
        }

        @Override
        public int length(int fieldIndex)
        {
            return ensureRowBuffer().length(fieldIndex);
        }

        @Override
        public RowKey keyOf(int[] positions)
        {
            for (int position : positions)
            {
                readStringValue(position);
            }
            return RowKey.copyOf(this.stringValues, positions);
        }

        @Override
        public Row copy()
        {
            return ensureRowBuffer().copy();
        }

        private String readStringValue(int fieldIndex)
        {
            if (this.stringLoaded[fieldIndex])
            {
                return this.stringValues[fieldIndex];
            }
            try
            {
                String value = RowCursorResultSet.this.resultSet.getString(fieldIndex + 1);
                this.stringValues[fieldIndex] = value;
                this.stringLoaded[fieldIndex] = true;
                return value;
            }
            catch (SQLException e)
            {
                throw new TableException("Failed to read ResultSet value for column " + (fieldIndex + 1) + ".", e);
            }
        }

        private RowBuffer ensureRowBuffer()
        {
            if (this.rowBuffer == null)
            {
                this.rowBuffer = new RowBuffer();
            }
            if (this.rowBuffer.size() == size())
            {
                return this.rowBuffer;
            }
            try
            {
                this.rowBuffer.clear();
                for (int i = 1; i <= size(); ++i)
                {
                    RowCursorResultSet.this.appendColumnValue(this.rowBuffer, i);
                    this.rowBuffer.finishField();
                }
                return this.rowBuffer;
            }
            catch (SQLException e)
            {
                throw new TableException("Failed to read ResultSet row text.", e);
            }
            catch (IOException e)
            {
                throw new TableException("Failed to stream ResultSet character data.", e);
            }
        }
    }
}
