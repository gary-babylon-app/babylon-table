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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;

/**
 * Detects header rows and selected columns for an input stream according to a
 * specific header interpretation strategy.
 */
public interface HeaderStrategy
{
    /** Default number of rows to scan when inferring headers. */
    int DEFAULT_SCAN_LIMIT = 25;

    /**
     * Returns the maximum number of rows to scan during header detection.
     *
     * @return scan limit
     */
    default int getScanLimit()
    {
        return DEFAULT_SCAN_LIMIT;
    }

    /**
     * Detects headers and optional selected columns from a markable row stream.
     *
     * @param rowStream
     *            row stream to inspect
     * @param selectedColumns
     *            requested selected columns
     * @return detected header information
     * @throws IOException
     *             if stream inspection fails
     */
    default HeaderDetection detect(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns) throws IOException
    {
        HeaderDetection foundDetection = detectFoundHeaders(rowStream, selectedColumns);
        if (foundDetection.isSyntheticHeaders())
        {
            return foundDetection;
        }
        HeaderDetection normalizedDetection = normalise(foundDetection);

        if (Is.empty(selectedColumns))
        {
            return normalizedDetection;
        }

        List<ColumnName> selectedHeaders = new ArrayList<>();
        List<Integer> selectedPositions = new ArrayList<>();
        ColumnName[] headersFound = normalizedDetection.getHeadersFound();
        int[] normalizedPositions = normalizedDetection.getSelectedPositions();
        for (int i = 0; i < headersFound.length; ++i)
        {
            ColumnName headerFound = headersFound[i];
            if (headerFound == null)
            {
                continue;
            }
            if (selectedColumns.contains(headerFound))
            {
                selectedHeaders.add(headerFound);
                selectedPositions.add(normalizedPositions[i]);
            }
        }
        return new HeaderDetection(headersFound, false, selectedHeaders.toArray(new ColumnName[selectedHeaders.size()]),
                toIntArray(selectedPositions));
    }

    /**
     * Detects the raw headers present in the source.
     *
     * @param rowStream
     *            row stream to inspect
     * @param selectedColumns
     *            requested selected columns
     * @return detected raw header information
     * @throws IOException
     *             if stream inspection fails
     */
    HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns) throws IOException;

    private static HeaderDetection normalise(HeaderDetection detection)
    {
        ColumnName[] headersFound = detection.getHeadersFound();
        int trimmedLength = headersFound.length;
        while (trimmedLength > 0 && headersFound[trimmedLength - 1] == null)
        {
            --trimmedLength;
        }

        ColumnName[] normalizedHeaders = new ColumnName[trimmedLength];
        int[] normalizedPositions = new int[trimmedLength];
        for (int i = 0; i < trimmedLength; ++i)
        {
            ColumnName header = headersFound[i];
            normalizedHeaders[i] = header == null ? ColumnName.of("Column" + (i + 1)) : header;
            normalizedPositions[i] = i;
        }
        return new HeaderDetection(normalizedHeaders, false, normalizedHeaders, normalizedPositions);
    }

    static ColumnName[] toColumnNames(Row row)
    {
        if (row == null)
        {
            return new ColumnName[0];
        }
        ColumnName[] columnNames = new ColumnName[row.size()];
        for (int i = 0; i < row.size(); ++i)
        {
            columnNames[i] = ColumnName.parse(row, row.start(i), row.length(i));
        }
        return columnNames;
    }

    private static int[] toIntArray(List<Integer> values)
    {
        int[] x = new int[values.size()];
        for (int i = 0; i < values.size(); ++i)
        {
            x[i] = values.get(i);
        }
        return x;
    }
}
