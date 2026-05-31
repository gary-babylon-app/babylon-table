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
 *             ByteStringSlices row = supplier.current();
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
    public ByteStringSlices current()
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

    private void appendColumnValue(StringSlices.Builder rowBuilder, int columnIndex) throws SQLException, IOException
    {
        Reader columnValueReader = null;
        try
        {
            columnValueReader = this.resultSet.getCharacterStream(columnIndex);
        }
        catch (SQLFeatureNotSupportedException e)
        {
            appendStringValue(rowBuilder, columnIndex);
            return;
        }

        if (columnValueReader == null)
        {
            appendStringValue(rowBuilder, columnIndex);
            return;
        }

        try (Reader ignored = columnValueReader)
        {
            rowBuilder.append(columnValueReader);
        }
    }

    private void appendStringValue(StringSlices.Builder rowBuilder, int columnIndex) throws SQLException
    {
        String value = this.resultSet.getString(columnIndex);
        if (value != null)
        {
            rowBuilder.append(value);
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
        private ByteStringSlices row;

        private RowResultSet()
        {
            this.row = null;
        }

        private void reset()
        {
            this.row = null;
        }

        private ByteStringSlices current()
        {
            if (this.row != null)
            {
                return this.row;
            }
            try
            {
                StringSlices.Builder builder = new StringSlices.Builder();
                for (int i = 1; i <= RowCursorResultSet.this.columns.length; ++i)
                {
                    RowCursorResultSet.this.appendColumnValue(builder, i);
                    builder.finishField();
                }
                this.row = builder.build().toByteStringSlices();
                return this.row;
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
