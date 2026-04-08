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

import app.babylon.io.DataSource;
import app.babylon.io.TestDataSources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.TableColumnar;

public class CsvTest
{
    @Test
    public void read_autoDetectByDefault_whenNoHeaderHintsProvided()
    {
        String csv = "" + "Account,12345678\n" + "SortCode,12-34-56\n" + "Date,Description,Amount\n"
                + "2026-01-01,Coffee,3.50\n" + "2026-01-02,Salary,1000.00\n";

        DataSource ds = TestDataSources.fromString(csv, "auto-default.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV();

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());

        ColumnObject<String> dates = table.getString(ColumnName.of("Date"));
        assertNotNull(dates);
        assertEquals("2026-01-01", dates.get(0));
    }

    @Test
    public void read_explicitHeaderRowIndex_ignoresScanLimit()
    {
        String csv = "" + "A,1\n" + "B,2\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";

        DataSource ds = TestDataSources.fromString(csv, "explicit-header-row.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withHeaderStrategy(new HeaderStrategyExplicitRow(2));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(1, table.getRowCount());
        assertNotNull(table.getString(ColumnName.of("Description")));
    }

    @Test
    public void read_expectedHeaders_withScanLimit()
    {
        String csv = "" + "Meta,Value\n" + "Account,123\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";

        DataSource ds = TestDataSources.fromString(csv, "expected-header.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withHeaderStrategy(new HeaderStrategyExpectedHeaders(10,
                ColumnName.of("Date"), ColumnName.of("Description"), ColumnName.of("Amount")));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(1, table.getRowCount());
    }

    @Test
    public void read_noHeaders_generatesColumnNamesAndTreatsAllRowsAsData()
    {
        String csv = "" + "john smith,london,uk\n" + "mary jones,paris\n";

        DataSource ds = TestDataSources.fromString(csv, "no-headers.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withHeaderStrategy(new HeaderStrategyNoHeaders(10));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());

        assertNotNull(table.getString(ColumnName.of("Column1")));
        assertNotNull(table.getString(ColumnName.of("Column2")));
        assertNotNull(table.getString(ColumnName.of("Column3")));
        assertEquals("john smith", table.getString(ColumnName.of("Column1")).get(0));
        assertEquals("uk", table.getString(ColumnName.of("Column3")).get(0));
        assertEquals("", table.getString(ColumnName.of("Column3")).get(1));
    }

    @Test
    public void read_noHeaders_ignoresSelectedHeaders()
    {
        String csv = "" + "john smith,london,uk\n" + "mary jones,paris,fr\n";

        DataSource ds = TestDataSources.fromString(csv, "no-headers-selected.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withHeaderStrategy(new HeaderStrategyNoHeaders(100))
                .withSelectedHeader(ColumnName.of("Date")).withSelectedHeader(ColumnName.of("Amount"));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());
        assertNotNull(table.getString(ColumnName.of("Column1")));
        assertNotNull(table.getString(ColumnName.of("Column2")));
        assertNotNull(table.getString(ColumnName.of("Column3")));
    }

    @Test
    public void read_widestNonEmptyRow_detectsHeaderAndDataRows()
    {
        String csv = "" + "Account,12345678\n" + "SortCode,12-34-56\n" + "Date,Description,Amount\n"
                + "2026-01-01,Coffee,3.50\n" + "2026-01-02,Salary,1000.00\n";

        DataSource ds = TestDataSources.fromString(csv, "widest-row.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(10));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());
        assertEquals("Coffee", table.getString(ColumnName.of("Description")).get(0));
    }

    @Test
    public void read_widestNonEmptyRow_respectsScanLimit()
    {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < 50; ++i)
        {
            csv.append("meta").append(i).append('\n');
        }
        csv.append("Date,Description,Amount\n");
        csv.append("2026-01-01,Coffee,3.50\n");

        DataSource ds = TestDataSources.fromString(csv.toString(), "widest-row-limit.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(50));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(1, table.getColumnCount());
        assertEquals(51, table.getRowCount());
        assertNotNull(table.getString(ColumnName.of("meta0")));
        assertEquals("meta1", table.getString(ColumnName.of("meta0")).get(0));
    }

    @Test
    public void read_widestNonEmptyRow_treatsTrimmedNaAsEmptyForHeaderScoring()
    {
        String csv = "" + "  n/a  ,  N/A  \n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";

        DataSource ds = TestDataSources.fromString(csv, "widest-row-na.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(10));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(1, table.getRowCount());
        assertEquals("Coffee", table.getString(ColumnName.of("Description")).get(0));
    }

    @Test
    public void read_includeResourceName_addsConstantColumnWithoutChangingRowCount()
    {
        String csv = "" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n" + "2026-01-02,Salary,1000.00\n";

        DataSource ds = TestDataSources.fromString(csv, "resource-name.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withIncludeResourceName(ColumnName.of("ResourceName"));

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(4, table.getColumnCount());
        assertEquals(2, table.getRowCount());
        assertEquals("resource-name.csv", table.getString(ColumnName.of("ResourceName")).get(0));
        assertEquals("resource-name.csv", table.getString(ColumnName.of("ResourceName")).get(1));
        assertEquals("Coffee", table.getString(ColumnName.of("Description")).get(0));
    }

    @Test
    public void read_autoDetect_padsMalformedDataRowsAfterProjection()
    {
        String csv = "" + "Account Name,Everyday Current Account\n" + "Account Number,12345678\n"
                + "Statement Period,2026-01\n" + "Date,Description,Amount\n"
                + "2026-01-01,\"Coffee, corner shop\",-3.50\n"
                + "2026-01-02,\"Salary \"\"Bonus\"\"\r\nAdjustment\",1000.00\n" + "2026-01-03,\"Card Payment\"\n"
                + "2026-01-04,\"Rent\",-500.00\n" + "2026-01-05 Only Date Present\n";

        DataSource ds = TestDataSources.fromString(csv, "bank-statement-malformed.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV();

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(3, table.getColumnCount());
        assertEquals(5, table.getRowCount());

        ColumnObject<String> dates = table.getString(ColumnName.of("Date"));
        ColumnObject<String> descriptions = table.getString(ColumnName.of("Description"));
        ColumnObject<String> amounts = table.getString(ColumnName.of("Amount"));

        assertEquals("2026-01-01", dates.get(0));
        assertEquals("Coffee, corner shop", descriptions.get(0));
        assertEquals("-3.50", amounts.get(0));

        assertEquals("2026-01-02", dates.get(1));
        assertEquals("Salary \"Bonus\"\r\nAdjustment", descriptions.get(1));
        assertEquals("1000.00", amounts.get(1));

        assertEquals("2026-01-03", dates.get(2));
        assertEquals("Card Payment", descriptions.get(2));
        assertEquals("", amounts.get(2));

        assertEquals("2026-01-05 Only Date Present", dates.get(4));
        assertEquals("", descriptions.get(4));
        assertEquals("", amounts.get(4));
    }

    @Test
    public void read_withStrippingFalse_preservesWhitespace()
    {
        String csv = "" + "Date,Description,Amount\n" + " 2026-01-01 , Coffee , 3.50 \n";

        DataSource ds = TestDataSources.fromString(csv, "no-strip.csv");
        ReadSettingsCSV settings = new ReadSettingsCSV().withStripping(false);

        TableColumnar table = Csv.read(ds, settings);
        assertNotNull(table);
        assertEquals(1, table.getRowCount());
        assertEquals(" 2026-01-01 ", table.getString(ColumnName.of("Date")).get(0));
        assertEquals(" Coffee ", table.getString(ColumnName.of("Description")).get(0));
        assertEquals(" 3.50 ", table.getString(ColumnName.of("Amount")).get(0));
    }
}
