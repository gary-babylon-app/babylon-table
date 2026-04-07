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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class HeaderStrategyAutoDetectionTest
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

    private static List<RowBuffer> rows(RowBuffer... rows)
    {
        return List.of(rows);
    }

    @Test
    public void detectHeaderRowIndex_headerIsWidestAndTextual()
    {
        List<RowBuffer> rows = rows(
            row("Date", "Symbol", "Price", "Volume"),
            row("2026-01-02", "AAA", "10.25", "1000")
        );
        assertEquals(0, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_widerDataRowThanHeader()
    {
        List<RowBuffer> rows = rows(
            row("Date", "Symbol"),
            row("2026-01-02", "AAA", "10.25", "1000", "2000")
        );
        assertEquals(0, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_preambleThenHeaderThenData()
    {
        List<RowBuffer> rows = rows(
            row("25569", "25570"),
            row("2026-03-23", "2026-03-24"),
            row("Date", "Symbol", "Price"),
            row("2026-01-02", "AAA", "10.25")
        );
        assertEquals(2, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_numericLikeRowBeforeTextHeader()
    {
        List<RowBuffer> rows = rows(
            row("25570", "25571", "25572"),
            row("Date", "Symbol", "Close"),
            row("2026-01-02", "AAA", "10.25")
        );
        assertEquals(1, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_sparseHeader()
    {
        List<RowBuffer> rows = rows(
            row("Date", "", "Price", ""),
            row("2026-01-02", "", "10.25", "")
        );
        assertEquals(0, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_quotedCommaStyleValuesInData()
    {
        List<RowBuffer> rows = rows(
            row("Date", "Issuer", "Amount"),
            row("2026-01-02", "Acme, Inc", "100.00")
        );
        assertEquals(0, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_excelSerialDatesAsData()
    {
        List<RowBuffer> rows = rows(
            row("TradeDate", "SettlementDate", "Symbol"),
            row("45500", "45503", "AAA")
        );
        assertEquals(0, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_tiePrefersFirst()
    {
        List<RowBuffer> rows = rows(
            row("ColA", "ColB", "ColC"),
            row("ColA", "ColB", "ColC"),
            row("2026-01-02", "AAA", "10.25")
        );
        assertEquals(0, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_bankStatementKeyValuePreambleThenHeader()
    {
        List<RowBuffer> rows = rows(
            row("Account", "12345678"),
            row("SortCode", "12-34-56"),
            row("Period", "2026-01-01 to 2026-01-31"),
            row("Currency", "GBP"),
            row("Date", "Description", "Debit", "Credit", "Balance"),
            row("2026-01-02", "Card Purchase", "10.00", "", "990.00"),
            row("2026-01-03", "Salary", "", "2500.00", "3490.00")
        );
        assertEquals(4, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }

    @Test
    public void detectHeaderRowIndex_contactExportTextual_usesUniquenessContrast()
    {
        List<RowBuffer> rows = rows(
            row("Name", "Email", "EmailType", "PhoneType"),
            row("John Smith", "john@x.com", "Home", "Home"),
            row("Mary Jones", "mary@x.com", "Work", "Work")
        );
        assertEquals(0, HeaderStrategyAuto.detectHeaderRowIndex(rows));
    }
}
