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
import app.babylon.text.Strings;

/**
 * Detects header rows and selected columns for an input stream according to a
 * specific header interpretation strategy.
 */
public interface HeaderStrategy
{
    int DEFAULT_SCAN_LIMIT = 25;

    default int getScanLimit()
    {
        return DEFAULT_SCAN_LIMIT;
    }

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

        List<String> selectedHeaders = new ArrayList<>();
        List<Integer> selectedPositions = new ArrayList<>();
        String[] headersFound = normalizedDetection.getHeadersFound();
        int[] normalizedPositions = normalizedDetection.getSelectedPositions();
        for (int i = 0; i < headersFound.length; ++i)
        {
            String headerFound = headersFound[i];
            if (Strings.isEmpty(headerFound))
            {
                continue;
            }
            if (selectedColumns.contains(ColumnName.of(headerFound)))
            {
                selectedHeaders.add(headerFound);
                selectedPositions.add(normalizedPositions[i]);
            }
        }
        return new HeaderDetection(headersFound, false, selectedHeaders.toArray(new String[selectedHeaders.size()]),
                toIntArray(selectedPositions));
    }

    HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns) throws IOException;

    private static HeaderDetection normalise(HeaderDetection detection)
    {
        String[] headersFound = detection.getHeadersFound();
        int trimmedLength = headersFound.length;
        while (trimmedLength > 0 && isBlankHeader(headersFound[trimmedLength - 1]))
        {
            --trimmedLength;
        }

        String[] normalizedHeaders = new String[trimmedLength];
        int[] normalizedPositions = new int[trimmedLength];
        for (int i = 0; i < trimmedLength; ++i)
        {
            String header = headersFound[i];
            normalizedHeaders[i] = isBlankHeader(header) ? "Column" + (i + 1) : header;
            normalizedPositions[i] = i;
        }
        return new HeaderDetection(normalizedHeaders, false, normalizedHeaders, normalizedPositions);
    }

    private static boolean isBlankHeader(String header)
    {
        return Strings.isEmpty(Strings.stripx(header));
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
