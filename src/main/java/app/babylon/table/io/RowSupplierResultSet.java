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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;

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
 * This supplier does not create or close the supplied {@link ResultSet}. The
 * caller remains responsible for managing the lifecycle of the underlying JDBC
 * resources.
 */
public class RowSupplierResultSet implements RowSupplier
{
    private final ResultSet resultSet;
    private final RowBuffer rowBuffer;
    private ColumnDefinition[] columns;
    private boolean currentAvailable;

    public RowSupplierResultSet(ResultSet resultSet)
    {
        this.resultSet = ArgumentCheck.nonNull(resultSet);
        this.rowBuffer = new RowBuffer();
        this.columns = resolveColumns(resultSet);
        this.currentAvailable = false;
    }

    @Override
    public ColumnDefinition[] columns()
    {
        return this.columns.clone();
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
            populateRowBuffer();
            this.currentAvailable = true;
            return true;
        }
        catch (SQLException e)
        {
            throw new TableException("Failed to advance ResultSet row supplier.", e);
        }
        catch (IOException e)
        {
            throw new TableException("Failed to read character data from ResultSet.", e);
        }
    }

    @Override
    public Row current()
    {
        if (!this.currentAvailable)
        {
            throw new IllegalStateException("current row is not available until next() succeeds");
        }
        return this.rowBuffer;
    }

    private void populateRowBuffer() throws SQLException, IOException
    {
        this.rowBuffer.clear();
        for (int i = 1; i <= this.columns.length; ++i)
        {
            appendColumnValue(i);
            this.rowBuffer.finishField();
        }
    }

    private void appendColumnValue(int columnIndex) throws SQLException, IOException
    {
        Reader columnValueReader = null;
        try
        {
            columnValueReader = this.resultSet.getCharacterStream(columnIndex);
        }
        catch (SQLFeatureNotSupportedException e)
        {
            appendStringValue(columnIndex);
            return;
        }

        if (columnValueReader == null)
        {
            appendStringValue(columnIndex);
            return;
        }

        try (Reader ignored = columnValueReader)
        {
            this.rowBuffer.append(columnValueReader);
        }
    }

    private void appendStringValue(int columnIndex) throws SQLException
    {
        String value = this.resultSet.getString(columnIndex);
        if (value != null)
        {
            this.rowBuffer.append(value);
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
}
