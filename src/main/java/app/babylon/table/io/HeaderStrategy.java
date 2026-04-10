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

import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

/**
 * Detects header rows and selected columns for an input stream according to a
 * specific header interpretation strategy.
 */
public interface HeaderStrategy
{
    default int getScanLimit()
    {
        return Csv.DEFAULT_HEADER_SCAN_LIMIT;
    }

    default HeaderDetection detect(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns) throws IOException
    {
        HeaderDetection foundDetection = detectFoundHeaders(rowStream, selectedColumns);
        if (foundDetection.isSyntheticHeaders())
        {
            return foundDetection;
        }

        if (selectedColumns == null || selectedColumns.isEmpty())
        {
            return foundDetection;
        }

        List<String> selectedHeaders = new ArrayList<>();
        List<Integer> selectedPositions = new ArrayList<>();
        String[] headersFound = foundDetection.getHeadersFound();
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
                selectedPositions.add(i);
            }
        }
        return new HeaderDetection(headersFound, false, selectedHeaders.toArray(new String[selectedHeaders.size()]),
                toIntArray(selectedPositions));
    }

    HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns) throws IOException;

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
