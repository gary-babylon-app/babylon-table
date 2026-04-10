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

public class HeaderStrategyWidestNonEmptyRow implements HeaderStrategy
{
    private static final int DEFAULT_SCAN_LIMIT = 50;

    private final int scanLimit;

    public HeaderStrategyWidestNonEmptyRow()
    {
        this(DEFAULT_SCAN_LIMIT);
    }

    public HeaderStrategyWidestNonEmptyRow(int scanLimit)
    {
        if (scanLimit < 1)
        {
            throw new IllegalArgumentException("Header scan limit must be at least 1.");
        }
        this.scanLimit = scanLimit;
    }

    public int getScanLimit()
    {
        return this.scanLimit;
    }

    @Override
    public HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns)
            throws IOException
    {
        List<RowBuffer> scannedRows = new ArrayList<>();
        int headerRowIndex = -1;
        int maxNonEmptyCount = 0;
        while (scannedRows.size() < this.scanLimit && rowStream.next())
        {
            RowBuffer row = new RowBuffer((RowBuffer) rowStream.current());
            scannedRows.add(row);

            int nonEmptyCount = countNonEmptyValues(row);
            if (nonEmptyCount > maxNonEmptyCount)
            {
                maxNonEmptyCount = nonEmptyCount;
                headerRowIndex = scannedRows.size() - 1;
            }
        }

        if (scannedRows.isEmpty() || headerRowIndex < 0)
        {
            return new HeaderDetection(new String[0]);
        }
        rowStream.mark(headerRowIndex);
        return new HeaderDetection(scannedRows.get(headerRowIndex).toStringArray());
    }

    private static int countNonEmptyValues(RowBuffer row)
    {
        int count = 0;
        for (int i = 0; i < row.fieldCount(); ++i)
        {
            String value = row.getString(i);
            String trimmed = value == null ? "" : value.strip();
            if (!trimmed.isEmpty() && !"n/a".equalsIgnoreCase(trimmed))
            {
                ++count;
            }
        }
        return count;
    }
}
