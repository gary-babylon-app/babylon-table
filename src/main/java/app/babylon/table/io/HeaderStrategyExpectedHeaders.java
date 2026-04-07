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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import app.babylon.table.ColumnName;

public class HeaderStrategyExpectedHeaders implements HeaderStrategy
{
    private final int scanLimit;
    private final Collection<ColumnName> expectedHeaders;

    public HeaderStrategyExpectedHeaders(int scanLimit)
    {
        this(scanLimit, java.util.List.of());
    }

    public HeaderStrategyExpectedHeaders(int scanLimit, ColumnName... expectedHeaders)
    {
        this(scanLimit, toColumnNames(expectedHeaders));
    }

    public HeaderStrategyExpectedHeaders(int scanLimit, Collection<ColumnName> expectedHeaders)
    {
        if (scanLimit < 1)
        {
            throw new IllegalArgumentException("Header scan limit must be at least 1.");
        }
        this.scanLimit = scanLimit;
        this.expectedHeaders = new LinkedHashSet<>(Objects.requireNonNull(expectedHeaders));
    }

    public int getScanLimit()
    {
        return this.scanLimit;
    }

    public Collection<ColumnName> getExpectedHeaders(Collection<ColumnName> x)
    {
        Collection<ColumnName> target = x == null ? new ArrayList<>() : x;
        target.addAll(this.expectedHeaders);
        return target;
    }

    @Override
    public HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, ReadSettings readSettings) throws IOException
    {
        if (this.expectedHeaders.isEmpty())
        {
            throw new RuntimeException("No expected headers configured for HeaderStrategyExpectedHeaders.");
        }

        int enoughForMatch = 1 + this.expectedHeaders.size() / 2;
        int rowsScanned = 0;
        while (rowsScanned < this.scanLimit && rowStream.next())
        {
            ++rowsScanned;
            RowBuffer rowBuffer = (RowBuffer) rowStream.current();
            int matchedHeaderCount = 0;
            for (int i = 0; i < rowBuffer.fieldCount(); ++i)
            {
                String item = rowBuffer.getString(i);
                if (item == null || item.strip().isEmpty())
                {
                    continue;
                }
                ColumnName itemAsColumn = ColumnName.of(item);
                if (this.expectedHeaders.contains(itemAsColumn))
                {
                    ++matchedHeaderCount;
                }
            }
            if (matchedHeaderCount >= enoughForMatch)
            {
                rowStream.mark(rowsScanned - 1);
                return new HeaderDetection(rowBuffer.toStringArray());
            }
        }
        throw new RuntimeException(
            "Can not find headers from expected names within " + this.scanLimit + " rows.");
    }

    private static Collection<ColumnName> toColumnNames(ColumnName[] expectedHeaders)
    {
        if (expectedHeaders == null || expectedHeaders.length == 0)
        {
            return List.of();
        }
        List<ColumnName> values = new ArrayList<>(expectedHeaders.length);
        for (ColumnName expectedHeader : expectedHeaders)
        {
            if (expectedHeader != null)
            {
                values.add(expectedHeader);
            }
        }
        return values;
    }
}
