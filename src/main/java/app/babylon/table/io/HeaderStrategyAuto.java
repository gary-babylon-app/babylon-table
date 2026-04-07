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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.table.DateFormatInference;

public class HeaderStrategyAuto implements HeaderStrategy
{
    private enum CellType
    { TEXT, NUM, DATE, BLANK }

    private final int scanLimit;

    public HeaderStrategyAuto()
    {
        this(ReadSettingsCommon.DEFAULT_HEADER_SCAN_LIMIT);
    }
    public HeaderStrategyAuto(int scanLimit)
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
    public HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, ReadSettings readSettings) throws IOException
    {
        List<RowBuffer> scannedRows = new ArrayList<>();
        while (scannedRows.size() < this.scanLimit && rowStream.next())
        {
            scannedRows.add(new RowBuffer((RowBuffer) rowStream.current()));
        }
        if (scannedRows.isEmpty())
        {
            return new HeaderDetection(new String[0]);
        }
        int headerRowIndex = detectHeaderRowIndex(scannedRows, readSettings);
        rowStream.mark(headerRowIndex);
        return new HeaderDetection(scannedRows.get(headerRowIndex).toStringArray());
    }

    static double headerScore(RowBuffer rowValues)
    {
        if (rowValues == null || rowValues.fieldCount() == 0)
        {
            return Double.NEGATIVE_INFINITY;
        }

        int total = rowValues.fieldCount();
        int nonBlank = 0, textCnt = 0, numCnt = 0, dateCnt = 0, lenSum = 0;
        Set<String> distinct = new HashSet<>();

        for (int i = 0; i < rowValues.fieldCount(); ++i)
        {
            String raw = rowValues.getString(i);
            String s = raw == null ? "" : raw.strip();
            CellType t = classify(s);
            if (t != CellType.BLANK)
            {
                nonBlank++;
                lenSum += s.length();
                distinct.add(s);
                if (t == CellType.TEXT)
                {
                    textCnt++;
                }
                else if (t == CellType.NUM)
                {
                    numCnt++;
                }
                else if (t == CellType.DATE)
                {
                    dateCnt++;
                }
            }
        }

        if (nonBlank == 0)
        {
            return Double.NEGATIVE_INFINITY;
        }

        double textShare = textCnt / (double) nonBlank;
        double numDateShare = (numCnt + dateCnt) / (double) nonBlank;
        double distinctShare = distinct.size() / (double) nonBlank;
        double avgLen = lenSum / (double) nonBlank;
        double widthFrac = nonBlank / (double) Math.max(1, total);

        double score = 0.0;
        score += 2.0 * textShare;
        score -= 1.5 * numDateShare;
        score += 1.0 * distinctShare;
        score -= 0.02 * Math.max(0.0, avgLen - 20.0);
        score += Math.min(0.5, 0.5 * widthFrac);
        score += Math.min(0.8, 0.2 * Math.max(0, nonBlank - 1));

        return score;
    }

    static int detectHeaderRowIndex(List<RowBuffer> rows)
    {
        return detectHeaderRowIndex(rows, null);
    }

    static int detectHeaderRowIndex(List<RowBuffer> rows, ReadSettings readSettings)
    {
        if (rows == null || rows.isEmpty())
        {
            return -1;
        }

        Collection<ColumnName> requestedHeaders = requestedHeaders(readSettings);

        int bestIndex = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < rows.size(); ++i)
        {
            RowBuffer row = rows.get(i);
            if (row == null || row.fieldCount() == 0)
            {
                continue;
            }
            double score = headerScore(row);
            score += uniquenessContrastBonus(rows, i);
            score += requestedHeaderBonus(row, requestedHeaders);
            score += nextRowTypeCompatibilityBonus(rows, i, readSettings);
            if (score > bestScore)
            {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static CellType classify(String s)
    {
        if (s == null || s.isEmpty() || "n/a".equals(s))
        {
            return CellType.BLANK;
        }
        if (looksDate(s))
        {
            return CellType.DATE;
        }
        if (looksNumericStrict(s))
        {
            return CellType.NUM;
        }
        return CellType.TEXT;
    }

    private static boolean looksNumericStrict(String s)
    {
        return DateFormatInference.isStrictInteger(s) || DateFormatInference.isStrictDecimal(s);
    }

    private static boolean looksDate(String s)
    {
        return DateFormatInference.isLikelyDate(s);
    }

    private static double uniquenessContrastBonus(List<RowBuffer> rows, int rowIndex)
    {
        RowBuffer current = rows.get(rowIndex);
        if (current == null || current.fieldCount() == 0)
        {
            return 0.0;
        }
        double currentUniqueRatio = uniqueNonBlankRatio(current);
        double bonus = 0.25 * currentUniqueRatio;

        int nextIndex = rowIndex + 1;
        if (nextIndex < rows.size())
        {
            RowBuffer next = rows.get(nextIndex);
            if (next != null && next.fieldCount() > 0)
            {
                double nextUniqueRatio = uniqueNonBlankRatio(next);
                double contrast = currentUniqueRatio - nextUniqueRatio;
                if (contrast > 0.0)
                {
                    bonus += 0.60 * contrast;
                }
            }
        }
        return bonus;
    }

    private static double uniqueNonBlankRatio(RowBuffer row)
    {
        Set<String> distinct = new HashSet<>();
        int nonBlank = 0;
        for (int i = 0; i < row.fieldCount(); ++i)
        {
            String raw = row.getString(i);
            String s = raw == null ? "" : raw.strip();
            if (s.isEmpty() || "n/a".equals(s))
            {
                continue;
            }
            ++nonBlank;
            distinct.add(s);
        }
        if (nonBlank == 0)
        {
            return 0.0;
        }
        return distinct.size() / (double) nonBlank;
    }

    private static double requestedHeaderBonus(RowBuffer row, Collection<ColumnName> requestedHeaders)
    {
        if (row == null || row.fieldCount() == 0 || requestedHeaders == null || requestedHeaders.isEmpty())
        {
            return 0.0;
        }

        int matchedCount = 0;
        int nonBlank = 0;
        for (int i = 0; i < row.fieldCount(); ++i)
        {
            String raw = row.getString(i);
            String value = raw == null ? "" : raw.strip();
            if (value.isEmpty())
            {
                continue;
            }
            ++nonBlank;
            if (requestedHeaders.contains(ColumnName.of(value)))
            {
                ++matchedCount;
            }
        }
        if (matchedCount == 0 || nonBlank == 0)
        {
            return 0.0;
        }

        double rowMatchRatio = matchedCount / (double) nonBlank;
        double requestedMatchRatio = matchedCount / (double) Math.max(1, requestedHeaders.size());
        return 0.75 * rowMatchRatio + 0.50 * requestedMatchRatio;
    }

    private static double nextRowTypeCompatibilityBonus(List<RowBuffer> rows, int rowIndex, ReadSettings readSettings)
    {
        if (readSettings == null || rowIndex + 1 >= rows.size())
        {
            return 0.0;
        }

        RowBuffer headerRow = rows.get(rowIndex);
        RowBuffer nextRow = rows.get(rowIndex + 1);
        if (headerRow == null || nextRow == null || headerRow.fieldCount() == 0 || nextRow.fieldCount() == 0)
        {
            return 0.0;
        }

        int typedChecks = 0;
        int typedMatches = 0;
        int width = Math.min(headerRow.fieldCount(), nextRow.fieldCount());
        for (int i = 0; i < width; ++i)
        {
            String headerValue = strip(headerRow.getString(i));
            if (headerValue.isEmpty())
            {
                continue;
            }
            Column.Type columnType = readSettings.getColumnType(ColumnName.of(headerValue));
            if (columnType == null)
            {
                continue;
            }
            ++typedChecks;
            String nextValue = strip(nextRow.getString(i));
            if (matchesType(nextValue, columnType))
            {
                ++typedMatches;
            }
        }
        if (typedChecks == 0)
        {
            return 0.0;
        }
        return 1.10 * (typedMatches / (double) typedChecks);
    }

    private static Collection<ColumnName> requestedHeaders(ReadSettings readSettings)
    {
        if (readSettings == null)
        {
            return List.of();
        }
        return readSettings.getRequestedHeaders(new ArrayList<>());
    }

    private static boolean matchesType(String value, Column.Type columnType)
    {
        if (value == null || value.isEmpty())
        {
            return false;
        }
        if (Column.Type.of(String.class).equals(columnType))
        {
            return true;
        }
        if (Column.Type.of(double.class).equals(columnType) || Column.Type.of(Double.class).equals(columnType))
        {
            return DateFormatInference.isStrictInteger(value) || DateFormatInference.isStrictDecimal(value);
        }
        if (Column.Type.of(int.class).equals(columnType) || Column.Type.of(Integer.class).equals(columnType))
        {
            return DateFormatInference.isStrictInteger(value);
        }
        if (Column.Type.of(long.class).equals(columnType) || Column.Type.of(Long.class).equals(columnType))
        {
            return DateFormatInference.isStrictInteger(value);
        }
        if (Column.Type.of(BigDecimal.class).equals(columnType))
        {
            return DateFormatInference.isStrictInteger(value) || DateFormatInference.isStrictDecimal(value);
        }
        if (Column.Type.of(java.time.LocalDate.class).equals(columnType))
        {
            return DateFormatInference.isLikelyDate(value);
        }
        return false;
    }

    private static String strip(String value)
    {
        return value == null ? "" : value.strip();
    }
}
