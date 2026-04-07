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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HeaderStrategyAutoScoreTest
{
    private static RowBuffer row(String... values)
    {
        RowBuffer row = new RowBuffer();
        for (String value : values)
        {
            if (value != null)
            {
                for (int i = 0; i < value.length(); ++i)
                {
                    row.append(value.charAt(i));
                }
            }
            row.finishField();
        }
        return row;
    }

    @Test
    public void headerScore_prefersTextHeaderOverDataRow()
    {
        double headerScore = HeaderStrategyAuto.headerScore(row("Date", "Symbol", "Price", "Volume"));
        double dataScore = HeaderStrategyAuto.headerScore(row("2026-03-21", "AAPL", "123.45", "1000"));

        assertTrue(headerScore > dataScore);
    }

    @Test
    public void headerScore_numericEdgeCasesStillClassifyAsNumeric()
    {
        double numericLikeScore = HeaderStrategyAuto.headerScore(row("+1", "-1", "1.0", "100000", "42"));
        double textualScore = HeaderStrategyAuto.headerScore(row("+1x", "1e3", ".1", "abc", "x42"));

        assertTrue(textualScore > numericLikeScore);
    }
}
