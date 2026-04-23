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

public class HeaderStrategyExplicitRow implements HeaderStrategy
{
    private final int headerRowIndex;

    public HeaderStrategyExplicitRow(int headerRowIndex)
    {
        if (headerRowIndex < 0)
        {
            throw new IllegalArgumentException("Header row index must be non-negative.");
        }
        this.headerRowIndex = headerRowIndex;
    }

    public int getHeaderRowIndex()
    {
        return this.headerRowIndex;
    }

    @Override
    public HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns)
            throws IOException
    {
        int rowIndex = 0;
        while (rowStream.next())
        {
            RowBuffer rowBuffer = (RowBuffer) rowStream.current();
            if (rowIndex == this.headerRowIndex)
            {
                rowStream.mark(rowIndex);
                return new HeaderDetection(rowBuffer.toStringArray());
            }
            ++rowIndex;
        }
        throw new RuntimeException("Can not find explicit header row index " + this.headerRowIndex + ".");
    }
}
