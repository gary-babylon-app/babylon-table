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
import java.util.Set;

import app.babylon.table.column.ColumnName;

public class HeaderStrategyNoHeaders implements HeaderStrategy
{
    private final int scanLimit;
    private final String columnPrefix;

    public HeaderStrategyNoHeaders(int scanLimit)
    {
        this(scanLimit, "Column");
    }

    public HeaderStrategyNoHeaders(int scanLimit, String columnPrefix)
    {
        if (scanLimit < 1)
        {
            throw new IllegalArgumentException("Header scan limit must be at least 1.");
        }
        if (columnPrefix == null || columnPrefix.isEmpty())
        {
            throw new IllegalArgumentException("Column prefix must not be empty.");
        }
        this.scanLimit = scanLimit;
        this.columnPrefix = columnPrefix;
    }

    public int getScanLimit()
    {
        return this.scanLimit;
    }

    public String getColumnPrefix()
    {
        return this.columnPrefix;
    }

    @Override
    public HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns)
            throws IOException
    {
        int maxWidth = 0;
        int rowsScanned = 0;
        while (rowsScanned < this.scanLimit && rowStream.next())
        {
            ++rowsScanned;
            Row rowBuffer = rowStream.current();
            if (rowBuffer.size() > maxWidth)
            {
                maxWidth = rowBuffer.size();
            }
        }
        if (maxWidth == 0)
        {
            return new HeaderDetection(new ColumnName[0], true);
        }
        ColumnName[] headers = new ColumnName[maxWidth];
        for (int i = 0; i < maxWidth; ++i)
        {
            headers[i] = ColumnName.of(this.columnPrefix + Integer.toString(i + 1));
        }
        return new HeaderDetection(headers, true);
    }
}
