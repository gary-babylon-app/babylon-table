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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.io.StreamSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TabularRowReaderResultSet extends TabularRowReaderCommon<TabularRowReaderResultSet>
{
    private static final int DEFAULT_TRANSFER_BUFFER_SIZE = 4096;

    private int transferBufferSize;

    public TabularRowReaderResultSet()
    {
        this.transferBufferSize = DEFAULT_TRANSFER_BUFFER_SIZE;
    }

    public TabularRowReaderResultSet withTransferBufferSize(int transferBufferSize)
    {
        if (transferBufferSize <= 0)
        {
            throw new IllegalArgumentException("transferBufferSize must be greater than zero");
        }
        this.transferBufferSize = transferBufferSize;
        return this;
    }

    public int getTransferBufferSize()
    {
        return this.transferBufferSize;
    }

    public TabularRowReader.Result read(ResultSet resultSet, RowConsumer rowConsumer)
    {
        ResultSet checkedResultSet = ArgumentCheck.nonNull(resultSet);
        RowConsumer checkedRowConsumer = ArgumentCheck.nonNull(rowConsumer);
        try
        {
            ResultSetMetaData metaData = checkedResultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            if (columnCount <= 0)
            {
                return TabularRowReader.Result.empty("ResultSet contains no columns.");
            }

            HeaderDetection headerDetection = createHeaderDetection(metaData);
            ColumnName[] projectedColumnNames = createProjectedColumnNames(headerDetection);
            RowProjected projectedRow = new RowProjectedDefault(headerDetection.getSelectedPositions());
            Predicate<Row> boundRowFilter = getBoundRowFilter(projectedColumnNames);

            checkedRowConsumer.start(projectedColumnNames);

            RowBuffer rowBuffer = new RowBuffer(columnCount * 32, columnCount);
            char[] transferBuffer = new char[this.transferBufferSize];

            while (checkedResultSet.next())
            {
                populateRowBuffer(checkedResultSet, rowBuffer, columnCount, transferBuffer);
                Row row = projectedRow.with(rowBuffer);
                if (boundRowFilter != null && !boundRowFilter.test(row))
                {
                    continue;
                }
                checkedRowConsumer.accept(row);
            }
            return TabularRowReader.Result.success();
        }
        catch (TableException e)
        {
            return TabularRowReader.Result.exception("Failed to read tabular data from ResultSet.", e);
        }
        catch (SQLException e)
        {
            return TabularRowReader.Result.exception("Failed to read tabular data from ResultSet.",
                    new TableException("Failed to read rows from ResultSet.", e));
        }
        catch (IOException e)
        {
            return TabularRowReader.Result.exception("Failed to read tabular data from ResultSet.",
                    new TableException("Failed to stream character data from ResultSet.", e));
        }
        catch (RuntimeException e)
        {
            return TabularRowReader.Result.exception("Failed to read tabular data from ResultSet.", e);
        }
    }

    @Override
    public TabularRowReader.Result read(StreamSource streamSource, RowConsumer rowConsumer)
    {
        throw new UnsupportedOperationException(
                "TabularRowReaderResultSet does not read from StreamSource. Use read(ResultSet, RowConsumer).");
    }

    @Override
    protected TabularRowReaderResultSet self()
    {
        return this;
    }

    private HeaderDetection createHeaderDetection(ResultSetMetaData metaData) throws SQLException
    {
        int columnCount = metaData.getColumnCount();
        ColumnName[] headersFound = new ColumnName[columnCount];
        for (int i = 1; i <= columnCount; ++i)
        {
            headersFound[i - 1] = ColumnName.of(resolveHeaderName(metaData, i));
        }

        Set<ColumnName> selectedColumns = getSelectedColumns(null);
        if (selectedColumns == null || selectedColumns.isEmpty())
        {
            return new HeaderDetection(headersFound);
        }

        List<ColumnName> selectedHeaders = new ArrayList<>();
        List<Integer> selectedPositions = new ArrayList<>();
        for (int i = 0; i < headersFound.length; ++i)
        {
            ColumnName header = headersFound[i];
            if (header == null)
            {
                continue;
            }
            if (selectedColumns.contains(header))
            {
                selectedHeaders.add(header);
                selectedPositions.add(i);
            }
        }

        return new HeaderDetection(headersFound, false, selectedHeaders.toArray(new ColumnName[selectedHeaders.size()]),
                toIntArray(selectedPositions));
    }

    private String resolveHeaderName(ResultSetMetaData metaData, int columnIndex) throws SQLException
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

    private ColumnName[] createProjectedColumnNames(HeaderDetection headerDetection)
    {
        ColumnName[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnName[] columnNames = new ColumnName[selectedHeaders.length];
        for (int i = 0; i < selectedHeaders.length; ++i)
        {
            columnNames[i] = getRenameColumnName(selectedHeaders[i]);
        }
        return columnNames;
    }

    private Predicate<Row> getBoundRowFilter(ColumnName[] projectedColumnNames)
    {
        return getRowFilter() == null ? null : getRowFilter().bind(projectedColumnNames);
    }

    private ColumnName getRenameColumnName(ColumnName original)
    {
        if (original == null)
        {
            return null;
        }
        return getColumnReName(original);
    }

    private void populateRowBuffer(ResultSet resultSet, RowBuffer rowBuffer, int columnCount, char[] transferBuffer)
            throws SQLException, IOException
    {
        rowBuffer.clear();
        for (int i = 1; i <= columnCount; ++i)
        {
            appendColumnValue(resultSet, rowBuffer, i, transferBuffer);
            rowBuffer.finishField();
        }
    }

    private void appendColumnValue(ResultSet resultSet, RowBuffer rowBuffer, int columnIndex, char[] transferBuffer)
            throws SQLException, IOException
    {
        Reader columnValueReader = null;
        try
        {
            columnValueReader = resultSet.getCharacterStream(columnIndex);
        }
        catch (SQLFeatureNotSupportedException e)
        {
            appendStringValue(resultSet, rowBuffer, columnIndex);
            return;
        }

        if (columnValueReader == null)
        {
            appendStringValue(resultSet, rowBuffer, columnIndex);
            return;
        }

        try (Reader ignored = columnValueReader)
        {
            int read;
            while ((read = columnValueReader.read(transferBuffer, 0, transferBuffer.length)) != -1)
            {
                rowBuffer.append(transferBuffer, 0, read);
            }
        }
    }

    private void appendStringValue(ResultSet resultSet, RowBuffer rowBuffer, int columnIndex) throws SQLException
    {
        String value = resultSet.getString(columnIndex);
        if (value != null)
        {
            rowBuffer.append(value);
        }
    }

    private static int[] toIntArray(List<Integer> values)
    {
        int[] x = new int[values.size()];
        for (int i = 0; i < values.size(); ++i)
        {
            x[i] = values.get(i).intValue();
        }
        return x;
    }
}
