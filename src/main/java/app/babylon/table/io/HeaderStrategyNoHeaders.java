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
    public HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, ReadSettings readSettings) throws IOException
    {
        int maxWidth = 0;
        int rowsScanned = 0;
        while (rowsScanned < this.scanLimit && rowStream.next())
        {
            ++rowsScanned;
            Row rowBuffer = rowStream.current();
            if (rowBuffer.fieldCount() > maxWidth)
            {
                maxWidth = rowBuffer.fieldCount();
            }
        }
        if (maxWidth == 0)
        {
            return new HeaderDetection(new String[0], true);
        }
        String[] headers = new String[maxWidth];
        for (int i = 0; i < maxWidth; ++i)
        {
            headers[i] = this.columnPrefix + Integer.toString(i + 1);
        }
        return new HeaderDetection(headers, true);
    }
}
