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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import app.babylon.table.column.ColumnName;
import app.babylon.table.transform.DateFormatInference;

public class HeaderStrategyAuto implements HeaderStrategy
{
    private static final String DEFAULT_SYNTHETIC_COLUMN_PREFIX = "Column";
    private static final int DATA_CONTRAST_SCAN_LIMIT = 5;
    private static final double MINIMUM_DATA_CONTRAST = 0.20;
    private static final double MINIMUM_HEADER_SCORE = 2.75;
    private static final double MINIMUM_SCORE_MARGIN = 0.35;

    private enum CellType
    {
        TEXT, NUM, DATE, BLANK
    }

    private static final class RowFacts
    {
        private final int total;
        private final int nonBlank;
        private final int textCnt;
        private final int numCnt;
        private final int dateCnt;
        private final int lenSum;
        private final int distinctCnt;

        private RowFacts(int total, int nonBlank, int textCnt, int numCnt, int dateCnt, int lenSum, int distinctCnt)
        {
            this.total = total;
            this.nonBlank = nonBlank;
            this.textCnt = textCnt;
            this.numCnt = numCnt;
            this.dateCnt = dateCnt;
            this.lenSum = lenSum;
            this.distinctCnt = distinctCnt;
        }

        private boolean isEmpty()
        {
            return this.nonBlank == 0;
        }

        private double textShare()
        {
            return this.textCnt / (double) this.nonBlank;
        }

        private double numDateShare()
        {
            return (this.numCnt + this.dateCnt) / (double) this.nonBlank;
        }

        private double distinctShare()
        {
            return this.distinctCnt / (double) this.nonBlank;
        }

        private double avgLen()
        {
            return this.lenSum / (double) this.nonBlank;
        }

        private double widthFrac()
        {
            return this.nonBlank / (double) Math.max(1, this.total);
        }
    }

    private static final class HeaderCandidate
    {
        private final int index;
        private final double score;
        private final double nextBestScore;
        private final double selectedColumnBonus;

        private HeaderCandidate(int index, double score, double nextBestScore, double selectedColumnBonus)
        {
            this.index = index;
            this.score = score;
            this.nextBestScore = nextBestScore;
            this.selectedColumnBonus = selectedColumnBonus;
        }
    }

    private final int scanLimit;

    public HeaderStrategyAuto()
    {
        this(HeaderStrategy.DEFAULT_SCAN_LIMIT);
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
    public HeaderDetection detectFoundHeaders(RowStreamMarkable rowStream, Set<ColumnName> selectedColumns)
            throws IOException
    {
        List<RowBuffer> scannedRows = new ArrayList<>();
        while (scannedRows.size() < this.scanLimit && rowStream.next())
        {
            scannedRows.add(new RowBuffer((RowBuffer) rowStream.current()));
        }
        if (scannedRows.isEmpty())
        {
            return new HeaderDetection(new ColumnName[0]);
        }
        HeaderCandidate candidate = detectHeaderCandidate(scannedRows, selectedColumns);
        if (!isConfidentHeader(scannedRows, candidate))
        {
            return new HeaderDetection(syntheticHeaders(scannedRows), true);
        }

        int headerRowIndex = candidate.index;
        rowStream.mark(headerRowIndex);
        return new HeaderDetection(HeaderStrategy.toColumnNames(scannedRows.get(headerRowIndex)));
    }

    static double headerScore(RowBuffer rowValues)
    {
        if (rowValues == null || rowValues.size() == 0)
        {
            return Double.NEGATIVE_INFINITY;
        }

        RowFacts facts = rowFacts(rowValues);
        if (facts.isEmpty())
        {
            return Double.NEGATIVE_INFINITY;
        }

        double score = 0.0;
        score += 2.0 * facts.textShare();
        score -= 1.5 * facts.numDateShare();
        score += 1.0 * facts.distinctShare();
        score -= 0.02 * Math.max(0.0, facts.avgLen() - 20.0);
        score += Math.min(0.5, 0.5 * facts.widthFrac());
        score += Math.min(0.8, 0.2 * Math.max(0, facts.nonBlank - 1));

        return score;
    }

    static int detectHeaderRowIndex(List<RowBuffer> rows)
    {
        return detectHeaderRowIndex(rows, null);
    }

    static int detectHeaderRowIndex(List<RowBuffer> rows, Set<ColumnName> selectedColumns)
    {
        HeaderCandidate candidate = detectHeaderCandidate(rows, selectedColumns);
        return candidate == null ? -1 : candidate.index;
    }

    private static HeaderCandidate detectHeaderCandidate(List<RowBuffer> rows, Set<ColumnName> selectedColumns)
    {
        if (rows == null || rows.isEmpty())
        {
            return null;
        }

        int bestIndex = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        double nextBestScore = Double.NEGATIVE_INFINITY;
        double bestSelectedColumnBonus = 0.0;
        for (int i = 0; i < rows.size(); ++i)
        {
            RowBuffer row = rows.get(i);
            if (row == null || row.isEmpty())
            {
                continue;
            }
            double score = headerScore(row);
            score += uniquenessContrastBonus(rows, i);
            double columnBonus = selectedColumnBonus(row, selectedColumns);
            score += columnBonus;
            if (score > bestScore)
            {
                nextBestScore = bestScore;
                bestScore = score;
                bestIndex = i;
                bestSelectedColumnBonus = columnBonus;
            }
            else if (score > nextBestScore)
            {
                nextBestScore = score;
            }
        }
        if (bestIndex < 0)
        {
            return null;
        }
        return new HeaderCandidate(bestIndex, bestScore, nextBestScore, bestSelectedColumnBonus);
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
        if (current == null || current.isEmpty())
        {
            return 0.0;
        }
        double currentUniqueRatio = uniqueNonBlankRatio(current);
        double bonus = 0.25 * currentUniqueRatio;

        int nextIndex = rowIndex + 1;
        if (nextIndex < rows.size())
        {
            RowBuffer next = rows.get(nextIndex);
            if (next != null && !next.isEmpty())
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
        for (int i = 0; i < row.size(); ++i)
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

    private static double selectedColumnBonus(RowBuffer row, Set<ColumnName> selectedColumns)
    {
        if (row == null || row.isEmpty() || selectedColumns == null || selectedColumns.isEmpty())
        {
            return 0.0;
        }

        int matchedCount = 0;
        int nonBlank = 0;
        for (int i = 0; i < row.size(); ++i)
        {
            String raw = row.getString(i);
            String value = raw == null ? "" : raw.strip();
            if (value.isEmpty())
            {
                continue;
            }
            ++nonBlank;
            if (selectedColumns.contains(ColumnName.of(value)))
            {
                ++matchedCount;
            }
        }
        if (matchedCount == 0 || nonBlank == 0)
        {
            return 0.0;
        }

        double rowMatchRatio = matchedCount / (double) nonBlank;
        double selectedMatchRatio = matchedCount / (double) Math.max(1, selectedColumns.size());
        return 0.75 * rowMatchRatio + 0.50 * selectedMatchRatio;
    }

    private static boolean isConfidentHeader(List<RowBuffer> rows, HeaderCandidate candidate)
    {
        if (candidate == null)
        {
            return false;
        }
        if (candidate.selectedColumnBonus > 0.0)
        {
            return true;
        }
        if (candidate.score < MINIMUM_HEADER_SCORE)
        {
            return false;
        }
        if (candidate.nextBestScore > Double.NEGATIVE_INFINITY
                && candidate.score - candidate.nextBestScore < MINIMUM_SCORE_MARGIN)
        {
            return false;
        }
        return hasFollowingDataContrast(rows, candidate.index);
    }

    private static boolean hasFollowingDataContrast(List<RowBuffer> rows, int candidateIndex)
    {
        RowFacts candidate = rowFacts(rows.get(candidateIndex));
        if (candidate.isEmpty())
        {
            return false;
        }

        int comparedRows = 0;
        double followingNumDateShare = 0.0;
        double followingTextShare = 0.0;
        int limit = Math.min(rows.size(), candidateIndex + 1 + DATA_CONTRAST_SCAN_LIMIT);
        for (int i = candidateIndex + 1; i < limit; ++i)
        {
            RowFacts following = rowFacts(rows.get(i));
            if (following.isEmpty())
            {
                continue;
            }
            ++comparedRows;
            followingNumDateShare += following.numDateShare();
            followingTextShare += following.textShare();
        }
        if (comparedRows == 0)
        {
            return true;
        }

        double averageFollowingNumDateShare = followingNumDateShare / comparedRows;
        double averageFollowingTextShare = followingTextShare / comparedRows;
        return averageFollowingNumDateShare - candidate.numDateShare() >= MINIMUM_DATA_CONTRAST
                || candidate.textShare() - averageFollowingTextShare >= MINIMUM_DATA_CONTRAST;
    }

    private static RowFacts rowFacts(RowBuffer rowValues)
    {
        if (rowValues == null)
        {
            return new RowFacts(0, 0, 0, 0, 0, 0, 0);
        }

        int nonBlank = 0, textCnt = 0, numCnt = 0, dateCnt = 0, lenSum = 0;
        Set<String> distinct = new HashSet<>();

        for (int i = 0; i < rowValues.size(); ++i)
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
        return new RowFacts(rowValues.size(), nonBlank, textCnt, numCnt, dateCnt, lenSum, distinct.size());
    }

    private static ColumnName[] syntheticHeaders(List<RowBuffer> rows)
    {
        int maxWidth = 0;
        for (RowBuffer row : rows)
        {
            if (row != null && row.size() > maxWidth)
            {
                maxWidth = row.size();
            }
        }

        ColumnName[] headers = new ColumnName[maxWidth];
        for (int i = 0; i < maxWidth; ++i)
        {
            headers[i] = ColumnName.of(DEFAULT_SYNTHETIC_COLUMN_PREFIX + Integer.toString(i + 1));
        }
        return headers;
    }
}
