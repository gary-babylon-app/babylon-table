/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.plans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import app.babylon.io.StreamSource;
import app.babylon.io.StreamSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.io.HeaderStrategyExplicitRow;
import app.babylon.table.io.HeaderStrategyNoHeaders;
import app.babylon.table.io.RowFilters;
import app.babylon.table.io.RowSourceCsv;
import app.babylon.table.io.RowSourceResultSet;
import app.babylon.table.transform.DateFormat;

class TablePlanReadTest
{
    @Test
    void shouldUseConfiguredOutputMetadataWhenExecutingExistingTable()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final TableName SOURCE = TableName.of("Source");
        final TableDescription SOURCE_DESCRIPTION = new TableDescription("Source description");
        final TableName BUILT = TableName.of("Built");
        final TableDescription BUILT_DESCRIPTION = new TableDescription("Built description");
        ColumnObject.Builder<String> builder = ColumnObject.builder(CODE, ColumnTypes.STRING);
        builder.add("ABC");

        TableColumnar source = Tables.newTable(SOURCE, SOURCE_DESCRIPTION, builder.build());
        TablePlanRead plan = new TablePlanRead().withTableName(BUILT).withTableDescription(BUILT_DESCRIPTION);

        TableColumnar built = plan.execute(source);

        assertEquals(BUILT, built.getName());
        assertEquals("Built description", built.getDescription().getValue());
        assertEquals("ABC", built.getString(CODE).get(0));
    }

    @Test
    void shouldExposeConfiguredColumnTypes()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName NAME = ColumnName.of("Name");

        TablePlanRead plan = new TablePlanRead().withColumnType(AMOUNT, ColumnTypes.DOUBLE).withColumnType(NAME,
                ColumnTypes.STRING);

        assertEquals(ColumnTypes.DOUBLE, plan.getColumnType(AMOUNT));
        assertEquals(ColumnTypes.STRING, plan.getColumnType(NAME));
        assertEquals(2, plan.getColumnTypes().size());
    }

    @Test
    void shouldReadFromCsvStreamSourceUsingPlanTypes()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_CSV = TableName.of("BuiltFromCsv");
        String csv = """
                        Code,Amount
                        abc,10.5
                        xyz,20.0
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_CSV).withColumnType(AMOUNT,
                ColumnTypes.DOUBLE);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(BUILT_FROM_CSV, table.getName());
        assertEquals(ColumnTypes.STRING, table.getType(CODE));
        assertEquals(ColumnTypes.DOUBLE, table.getType(AMOUNT));
        ColumnObject<String> codes = table.getString(CODE);
        assertEquals("abc", codes.get(0));
        assertEquals("xyz", codes.get(1));
        assertEquals(10.5d, table.getDouble(AMOUNT).get(0));
        assertEquals(20.0d, table.getDouble(AMOUNT).get(1));
    }

    @Test
    void shouldReadBigDecimalColumnsFromCsvUsingCharSliceBuilder()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_CSV = TableName.of("BuiltFromCsv");
        String csv = """
                Code,Amount
                abc,10.50
                xyz,
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_CSV).withColumnType(AMOUNT,
                ColumnTypes.DECIMAL);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(BUILT_FROM_CSV, table.getName());
        assertEquals(ColumnTypes.STRING, table.getType(CODE));
        assertEquals(ColumnTypes.DECIMAL, table.getType(AMOUNT));
        ColumnObject<String> codes = table.getString(CODE);
        ColumnObject<BigDecimal> amounts = table.getDecimal(AMOUNT);
        assertEquals("abc", codes.get(0));
        assertEquals(0, new BigDecimal("10.50").compareTo(amounts.get(0)));
        assertFalse(amounts.isSet(1));
    }

    @Test
    void shouldReadIntColumnsWhenSpecifiedOnRowSource()
    {
        final ColumnName UNIQUE_ID = ColumnName.of("UniqueId");
        final ColumnName BUCKET = ColumnName.of("Bucket");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        final String csv = """
                UniqueId,Bucket
                101,7
                102,9
                103,7
                104,7
                105,9
                """;

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withSeparator(',').withColumnType(UNIQUE_ID, ColumnTypes.INT).withColumnType(BUCKET, ColumnTypes.INT)
                .build();
        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(ColumnTypes.INT, table.getType(UNIQUE_ID));
        assertEquals(ColumnTypes.INT, table.getType(BUCKET));
        assertEquals(101, table.getInt(UNIQUE_ID).get(0));
        assertEquals(102, table.getInt(UNIQUE_ID).get(1));
        assertEquals(103, table.getInt(UNIQUE_ID).get(2));
        assertEquals(104, table.getInt(UNIQUE_ID).get(3));
        assertEquals(105, table.getInt(UNIQUE_ID).get(4));
        assertEquals(7, table.getInt(BUCKET).get(0));
        assertEquals(9, table.getInt(BUCKET).get(1));
        assertEquals(7, table.getInt(BUCKET).get(2));
        assertEquals(7, table.getInt(BUCKET).get(3));
        assertEquals(9, table.getInt(BUCKET).get(4));
    }

    @Test
    void shouldReadIntColumnsWhenSpecifiedOnTablePlanRead()
    {
        final ColumnName UNIQUE_ID = ColumnName.of("UniqueId");
        final ColumnName BUCKET = ColumnName.of("Bucket");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        final String csv = """
                UniqueId,Bucket
                101,7
                102,9
                103,7
                104,7
                105,9
                """;

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withSeparator(',').build();
        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS).withColumnType(UNIQUE_ID, ColumnTypes.INT)
                .withColumnType(BUCKET, ColumnTypes.INT);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(ColumnTypes.INT, table.getType(UNIQUE_ID));
        assertEquals(ColumnTypes.INT, table.getType(BUCKET));
        assertEquals(101, table.getInt(UNIQUE_ID).get(0));
        assertEquals(102, table.getInt(UNIQUE_ID).get(1));
        assertEquals(103, table.getInt(UNIQUE_ID).get(2));
        assertEquals(104, table.getInt(UNIQUE_ID).get(3));
        assertEquals(105, table.getInt(UNIQUE_ID).get(4));
        assertEquals(7, table.getInt(BUCKET).get(0));
        assertEquals(9, table.getInt(BUCKET).get(1));
        assertEquals(7, table.getInt(BUCKET).get(2));
        assertEquals(7, table.getInt(BUCKET).get(3));
        assertEquals(9, table.getInt(BUCKET).get(4));
    }

    @Test
    void shouldReadConstantCurrencyAndNotionalWithVaryingAmounts()
    {
        final ColumnName CCY = ColumnName.of("Currency");
        final ColumnName NOTIONAL = ColumnName.of("Notional");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        final BigDecimal EXPECTED_NOTIONAL = new BigDecimal("100000000");
        String csv = """
                Currency,Notional,Amount
                USD,100000000,1000000
                USD,100000000,1200000
                USD,100000000,1400000
                USD,100000000,1600000
                USD,100000000,1800000
                USD,100000000,2000000
                USD,100000000,2200000
                USD,100000000,2400000
                USD,100000000,2600000
                USD,100000000,2800000
                """;

        // @formatter:off
        TablePlanRead plan = new TablePlanRead()
                                .withTableName(CASHFLOWS)
                                .withColumnType(CCY, ColumnTypes.CURRENCY)
                                .withColumnTypes(ColumnTypes.DECIMAL, NOTIONAL, AMOUNT);
        // @formatter:on

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(ColumnTypes.CURRENCY, table.getType(CCY));
        assertEquals(ColumnTypes.DECIMAL, table.getType(NOTIONAL));
        assertEquals(ColumnTypes.DECIMAL, table.getType(AMOUNT));

        ColumnObject<Currency> currency = table.getObject(CCY, ColumnTypes.CURRENCY);
        ColumnObject<BigDecimal> notional = table.getDecimal(NOTIONAL);
        ColumnObject<BigDecimal> amounts = table.getDecimal(AMOUNT);

        assertEquals(10, table.getRowCount());
        assertTrue(currency.isConstant());
        assertTrue(notional.isConstant());
        assertFalse(amounts.isConstant());

        assertEquals(Currency.getInstance("USD"), currency.get(0));
        assertEquals(0, EXPECTED_NOTIONAL.compareTo(notional.get(0)));
        assertEquals(0, EXPECTED_NOTIONAL.compareTo(notional.get(9)));
        assertEquals(0, new BigDecimal("1000000").compareTo(amounts.get(0)));
        assertEquals(0, new BigDecimal("2800000").compareTo(amounts.get(9)));
    }

    @Test
    void shouldReadLocalDateColumnsUsingDateInferenceAfterBuild()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName CODE = ColumnName.of("Code");
        final TableName BUILT_FROM_CSV = TableName.of("BuiltFromCsv");
        String csv = """
                TradeDate,Code
                15/02/2026,abc
                16/02/2026,xyz
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_CSV).withColumnType(TRADE_DATE,
                ColumnTypes.LOCALDATE);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(BUILT_FROM_CSV, table.getName());
        assertEquals(ColumnTypes.LOCALDATE, table.getType(TRADE_DATE));
        assertEquals(ColumnTypes.STRING, table.getType(CODE));
        assertEquals(LocalDate.of(2026, 2, 15), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 2, 16), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals("abc", table.getString(CODE).get(0));
        assertEquals("xyz", table.getString(CODE).get(1));
    }

    @Test
    void shouldInferAmbiguousTradeDateFromSettleDateColumn()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName SETTLE_DATE = ColumnName.of("SettleDate");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                TradeDate,SettleDate
                12/02/2026,13/02/2026
                12/03/2026,14/03/2026
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS)
                .withColumnType(TRADE_DATE, ColumnTypes.LOCALDATE).withColumnType(SETTLE_DATE, ColumnTypes.LOCALDATE);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(ColumnTypes.LOCALDATE, table.getType(TRADE_DATE));
        assertEquals(ColumnTypes.LOCALDATE, table.getType(SETTLE_DATE));
        assertEquals(LocalDate.of(2026, 2, 12), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 3, 12), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 2, 13), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 3, 14), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldReadThreeLetterMonthNamesWhenAllDaysAreAtMostTwelve()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName SETTLE_DATE = ColumnName.of("SettleDate");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                TradeDate,SettleDate
                02-Mar-2026,05-Mar-2026
                07-Apr-2026,10-Apr-2026
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS)
                .withColumnType(TRADE_DATE, ColumnTypes.LOCALDATE).withColumnType(SETTLE_DATE, ColumnTypes.LOCALDATE);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(ColumnTypes.LOCALDATE, table.getType(TRADE_DATE));
        assertEquals(ColumnTypes.LOCALDATE, table.getType(SETTLE_DATE));
        assertEquals(LocalDate.of(2026, 3, 2), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 7), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 3, 5), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 10), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldInferIsoDatesWhenAllDaysAreAmbiguous()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName SETTLE_DATE = ColumnName.of("SettleDate");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                TradeDate,SettleDate
                2026-03-01,2026-03-03
                2026-04-01,2026-04-03
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS)
                .withColumnType(TRADE_DATE, ColumnTypes.LOCALDATE).withColumnType(SETTLE_DATE, ColumnTypes.LOCALDATE);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(ColumnTypes.LOCALDATE, table.getType(TRADE_DATE));
        assertEquals(ColumnTypes.LOCALDATE, table.getType(SETTLE_DATE));
        assertEquals(LocalDate.of(2026, 3, 1), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 1), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 3, 3), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 3), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldThrowWhenAllDmyDatesAreAmbiguous()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName SETTLE_DATE = ColumnName.of("SettleDate");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                TradeDate,SettleDate
                01/03/2026,03/03/2026
                01/04/2026,03/04/2026
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS)
                .withColumnType(TRADE_DATE, ColumnTypes.LOCALDATE).withColumnType(SETTLE_DATE, ColumnTypes.LOCALDATE);

        assertThrows(IllegalArgumentException.class, () -> plan.execute(csvRowSource(csv)));
    }

    @Test
    void shouldUseConfiguredLocalDateFallbackForAmbiguousDmyDates()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName SETTLE_DATE = ColumnName.of("SettleDate");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                TradeDate,SettleDate
                01/03/2026,03/03/2026
                01/04/2026,03/04/2026
                """;

        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS)
                .withColumnType(TRADE_DATE, ColumnTypes.LOCALDATE).withColumnType(SETTLE_DATE, ColumnTypes.LOCALDATE)
                .withLocalDateFormat(DateFormat.DMY);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(ColumnTypes.LOCALDATE, table.getType(TRADE_DATE));
        assertEquals(ColumnTypes.LOCALDATE, table.getType(SETTLE_DATE));
        assertEquals(LocalDate.of(2026, 3, 1), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 1), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 3, 3), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 3), table.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldIncludeResourceNameColumnForStreamSourceReads()
    {
        final ColumnName RESOURCE_NAME = ColumnName.of("ResourceName");
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_CSV = TableName.of("BuiltFromCsv");
        String csv = "Code,Amount\nabc,10.5\nxyz,20.0\n";

        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_CSV).withIncludeResourceName(RESOURCE_NAME);

        TableColumnar table = plan.execute(csvRowSource(csv));

        assertEquals(3, table.getColumnCount());
        assertEquals(RESOURCE_NAME, table.getColumnNames()[2]);
        assertEquals("values.csv", table.getString(RESOURCE_NAME).get(0));
        assertEquals("values.csv", table.getString(RESOURCE_NAME).get(1));
        assertEquals("abc", table.getString(CODE).get(0));
        assertEquals("10.5", table.getString(AMOUNT).get(0));
    }

    @Test
    void shouldIncludeResourceNameColumnForRowSourceReads()
    {
        final ColumnName RESOURCE_NAME = ColumnName.of("ResourceName");
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_ROW_SOURCE = TableName.of("BuiltFromRowSource");
        String csv = "Code,Amount\nabc,10.5\nxyz,20.0\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .build();
        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_ROW_SOURCE)
                .withIncludeResourceName(RESOURCE_NAME);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(3, table.getColumnCount());
        assertEquals(RESOURCE_NAME, table.getColumnNames()[table.getColumnCount() - 1]);
        assertEquals("values.csv", table.getString(RESOURCE_NAME).get(0));
        assertEquals("values.csv", table.getString(RESOURCE_NAME).get(1));
        assertEquals("abc", table.getString(CODE).get(0));
        assertEquals("10.5", table.getString(AMOUNT).get(0));
    }

    @Test
    void shouldIncludeEmptyResourceNameColumnForZeroRowTables()
    {
        final ColumnName RESOURCE_NAME = ColumnName.of("ResourceName");
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_ROW_SOURCE = TableName.of("BuiltFromRowSource");
        String csv = "Code,Amount\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .build();
        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_ROW_SOURCE)
                .withIncludeResourceName(RESOURCE_NAME);

        TableColumnar table = plan.execute(rowSource);

        ColumnName[] columnNames = table.getColumnNames();

        assertEquals(3, table.getColumnCount());
        assertEquals(0, table.getRowCount());
        assertEquals(CODE, columnNames[0]);
        assertEquals(AMOUNT, columnNames[1]);
        assertEquals(RESOURCE_NAME, columnNames[2]);
    }

    @Test
    void shouldReadFromRowSourceUsingSupplierTypes()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_ROW_SOURCE = TableName.of("BuiltFromRowSource");
        String csv = "Code,Amount\nabc,10.5\nxyz,20.0\n";

        StreamSource streamSource = StreamSources.fromString(csv, "values.csv");
        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(streamSource)
                .withColumnType(AMOUNT, ColumnTypes.DOUBLE).build();
        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_ROW_SOURCE);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(BUILT_FROM_ROW_SOURCE, table.getName());
        assertEquals(ColumnTypes.STRING, table.getType(CODE));
        assertEquals(ColumnTypes.DOUBLE, table.getType(AMOUNT));
        ColumnObject<String> codes = table.getString(CODE);
        assertEquals("abc", codes.get(0));
        assertEquals("xyz", codes.get(1));
        assertEquals(10.5d, table.getDouble(AMOUNT).get(0));
        assertEquals(20.0d, table.getDouble(AMOUNT).get(1));
    }

    @Test
    void shouldReadSelectedAndRenamedColumnsFromRowSource()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName IDENTIFIER = ColumnName.of("Identifier");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                Code,Amount,Unused
                abc,10.5,x
                xyz,20.0,y
                """;

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withSelectedColumns(CODE, AMOUNT).withColumnRename(CODE, IDENTIFIER)
                .withColumnType(AMOUNT, ColumnTypes.DOUBLE).build();
        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(CASHFLOWS, table.getName());
        assertEquals(2, table.getColumnCount());
        assertEquals(IDENTIFIER, table.getColumnNames()[0]);
        assertEquals(AMOUNT, table.getColumnNames()[1]);
        assertEquals(ColumnTypes.STRING, table.getType(IDENTIFIER));
        assertEquals(ColumnTypes.DOUBLE, table.getType(AMOUNT));
        assertEquals("abc", table.getString(IDENTIFIER).get(0));
        assertEquals("xyz", table.getString(IDENTIFIER).get(1));
        assertEquals(10.5d, table.getDouble(AMOUNT).get(0));
        assertEquals(20.0d, table.getDouble(AMOUNT).get(1));
    }

    @Test
    void shouldApplyRowFilterBeforeReadingTable()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                Code,Amount
                abc,10.5
                ,
                xyz,20.0
                """;

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withRowFilter(RowFilters.excludeEmpty(CODE, AMOUNT)).build();
        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(2, table.getRowCount());
        assertEquals("abc", table.getString(CODE).get(0));
        assertEquals("xyz", table.getString(CODE).get(1));
        assertEquals("10.5", table.getString(AMOUNT).get(0));
        assertEquals("20.0", table.getString(AMOUNT).get(1));
    }

    @Test
    void shouldReadSelectedRenamedAndFilteredColumnsWithExplicitHeaderRow()
    {
        final ColumnName TRADE_CODE = ColumnName.of("TradeCode");
        final ColumnName NOTIONAL = ColumnName.of("Notional");
        final ColumnName ID = ColumnName.of("Id");
        final TableName CASHFLOWS = TableName.of("Cashflows");
        String csv = """
                ignored,ignored,ignored
                TradeCode,Notional,Comment
                T1,100,keep
                ,200,drop
                T3,300,keep
                """;

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withHeaderStrategy(new HeaderStrategyExplicitRow(1)).withSelectedColumns(TRADE_CODE, NOTIONAL)
                .withColumnRename(TRADE_CODE, ID).withRowFilter(RowFilters.excludeEmpty(ID))
                .withColumnType(NOTIONAL, ColumnTypes.INT).build();
        TablePlanRead plan = new TablePlanRead().withTableName(CASHFLOWS);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(2, table.getColumnCount());
        assertEquals(ID, table.getColumnNames()[0]);
        assertEquals(NOTIONAL, table.getColumnNames()[1]);
        assertEquals(ColumnTypes.STRING, table.getType(ID));
        assertEquals(ColumnTypes.INT, table.getType(NOTIONAL));
        assertEquals(2, table.getRowCount());
        assertEquals("T1", table.getString(ID).get(0));
        assertEquals("T3", table.getString(ID).get(1));
        assertEquals(100, table.getInt(NOTIONAL).get(0));
        assertEquals(300, table.getInt(NOTIONAL).get(1));
    }

    @Test
    void shouldTrimTrailingBlankDetectedHeaderColumns()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_ROW_SOURCE = TableName.of("BuiltFromRowSource");
        String csv = "Code,Amount,\nabc,10.5,\ndef,\nxyz,20.0,\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withHeaderStrategy(new HeaderStrategyExplicitRow(0)).withColumnType(AMOUNT, ColumnTypes.DECIMAL)
                .build();

        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_ROW_SOURCE);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(2, table.getColumnCount());

        ColumnObject<String> codes = table.getString(CODE);
        ColumnObject<BigDecimal> amounts = table.getDecimal(AMOUNT);

        assertEquals("abc", codes.first());
        assertEquals("xyz", codes.last());
        assertEquals("10.5", amounts.first().toPlainString());
        assertEquals("20", amounts.last().toPlainString());
        int shortRow = 1;
        assertTrue(codes.isSet(shortRow));
        assertFalse(amounts.isSet(shortRow));
    }

    @Test
    void shouldKeepTrailingEmptyColumnWithoutHeadersUntilPruned()
    {
        final ColumnName COLUMN_1 = ColumnName.of("Column1");
        final ColumnName COLUMN_2 = ColumnName.of("Column2");
        final ColumnName COLUMN_3 = ColumnName.of("Column3");
        final TableName BUILT_FROM_ROW_SOURCE = TableName.of("BuiltFromRowSource");
        String csv = "abc,10.5,\nxyz,20.0,\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withHeaderStrategy(new HeaderStrategyNoHeaders(10)).build();

        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_ROW_SOURCE);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(3, table.getColumnCount());
        assertEquals("abc", table.getString(COLUMN_1).get(0));
        assertEquals("xyz", table.getString(COLUMN_1).get(1));
        assertEquals("10.5", table.getString(COLUMN_2).get(0));
        assertEquals("20.0", table.getString(COLUMN_2).get(1));
        assertFalse(table.getString(COLUMN_3).isSet(0));
        assertFalse(table.getString(COLUMN_3).isSet(1));

        TableColumnar pruned = table.prune();

        assertEquals(2, pruned.getColumnCount());
        assertEquals(COLUMN_1, pruned.getColumnNames()[0]);
        assertEquals(COLUMN_2, pruned.getColumnNames()[1]);
    }

    @Test
    void shouldReadFromResultSetRowSource()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName BUILT_FROM_RESULT_SET = TableName.of("BuiltFromResultSet");
        PreparedStatement preparedStatement = preparedStatement(resultSet(new String[]
        {"Code", "Amount"}, new String[]
        {"Code", "Amount"}, new int[]
        {Types.VARCHAR, Types.DECIMAL}, new String[][]
        {
                {"abc", "10.50"},
                {"xyz", "20.00"}}));
        RowSourceResultSet rowSource = RowSourceResultSet.builder().withPreparedStatement(preparedStatement)
                .withName("TradesQuery").build();
        TablePlanRead plan = new TablePlanRead().withTableName(BUILT_FROM_RESULT_SET);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(BUILT_FROM_RESULT_SET, table.getName());
        assertEquals(ColumnTypes.STRING, table.getType(CODE));
        assertEquals(ColumnTypes.DECIMAL, table.getType(AMOUNT));
        ColumnObject<String> codes = table.getString(CODE);
        ColumnObject<BigDecimal> amounts = table.getDecimal(AMOUNT);
        assertEquals("abc", codes.get(0));
        assertEquals("xyz", codes.get(1));
        assertEquals(0, new BigDecimal("10.50").compareTo(amounts.get(0)));
        assertEquals(0, new BigDecimal("20.00").compareTo(amounts.get(1)));
    }

    private static PreparedStatement preparedStatement(ResultSet resultSet)
    {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if (methodName.equals("executeQuery"))
            {
                return resultSet;
            }
            if (methodName.equals("close"))
            {
                return null;
            }
            if (methodName.equals("isClosed"))
            {
                return false;
            }
            if (methodName.equals("unwrap"))
            {
                return null;
            }
            if (methodName.equals("isWrapperFor"))
            {
                return false;
            }
            if (methodName.equals("toString"))
            {
                return "MockPreparedStatement";
            }
            if (methodName.equals("hashCode"))
            {
                return System.identityHashCode(proxy);
            }
            if (methodName.equals("equals"))
            {
                return proxy == args[0];
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class)
            {
                return false;
            }
            if (returnType == int.class)
            {
                return 0;
            }
            if (returnType == long.class)
            {
                return 0L;
            }
            return null;
        };
        return (PreparedStatement) Proxy.newProxyInstance(TablePlanReadTest.class.getClassLoader(), new Class<?>[]
        {PreparedStatement.class}, handler);
    }

    private static ResultSet resultSet(String[] labels, String[] names, int[] jdbcTypes, String[][] rows)
    {
        ResultSetMetaData metaData = resultSetMetaData(labels, names, jdbcTypes);
        InvocationHandler handler = new InvocationHandler()
        {
            private int rowIndex = -1;
            private boolean wasNull = false;

            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable
            {
                String methodName = method.getName();
                if (methodName.equals("next"))
                {
                    this.rowIndex++;
                    return this.rowIndex < rows.length;
                }
                if (methodName.equals("getMetaData"))
                {
                    return metaData;
                }
                if (methodName.equals("getCharacterStream"))
                {
                    return null;
                }
                if (methodName.equals("getString"))
                {
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    String value = rows[this.rowIndex][columnIndex];
                    this.wasNull = value == null;
                    return value;
                }
                if (methodName.equals("getInt"))
                {
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    String value = rows[this.rowIndex][columnIndex];
                    this.wasNull = value == null;
                    return value == null ? 0 : Integer.parseInt(value);
                }
                if (methodName.equals("getLong"))
                {
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    String value = rows[this.rowIndex][columnIndex];
                    this.wasNull = value == null;
                    return value == null ? 0L : Long.parseLong(value);
                }
                if (methodName.equals("getDouble"))
                {
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    String value = rows[this.rowIndex][columnIndex];
                    this.wasNull = value == null;
                    return value == null ? 0d : Double.parseDouble(value);
                }
                if (methodName.equals("getByte"))
                {
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    String value = rows[this.rowIndex][columnIndex];
                    this.wasNull = value == null;
                    return value == null ? (byte) 0 : Byte.parseByte(value);
                }
                if (methodName.equals("getBigDecimal"))
                {
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    String value = rows[this.rowIndex][columnIndex];
                    this.wasNull = value == null;
                    return value == null ? null : new BigDecimal(value);
                }
                if (methodName.equals("wasNull"))
                {
                    return this.wasNull;
                }
                if (methodName.equals("close"))
                {
                    return null;
                }
                if (methodName.equals("isClosed"))
                {
                    return false;
                }
                if (methodName.equals("unwrap"))
                {
                    return null;
                }
                if (methodName.equals("isWrapperFor"))
                {
                    return false;
                }
                if (methodName.equals("toString"))
                {
                    return "MockResultSet";
                }
                if (methodName.equals("hashCode"))
                {
                    return System.identityHashCode(proxy);
                }
                if (methodName.equals("equals"))
                {
                    return proxy == args[0];
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class)
                {
                    return false;
                }
                if (returnType == int.class)
                {
                    return 0;
                }
                if (returnType == long.class)
                {
                    return 0L;
                }
                return null;
            }
        };
        return (ResultSet) Proxy.newProxyInstance(TablePlanReadTest.class.getClassLoader(), new Class<?>[]
        {ResultSet.class}, handler);
    }

    private static ResultSetMetaData resultSetMetaData(String[] labels, String[] names, int[] jdbcTypes)
    {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if (methodName.equals("getColumnCount"))
            {
                return labels.length;
            }
            if (methodName.equals("getColumnLabel"))
            {
                return labels[((Integer) args[0]).intValue() - 1];
            }
            if (methodName.equals("getColumnName"))
            {
                return names[((Integer) args[0]).intValue() - 1];
            }
            if (methodName.equals("getColumnType"))
            {
                return jdbcTypes[((Integer) args[0]).intValue() - 1];
            }
            if (methodName.equals("unwrap"))
            {
                return null;
            }
            if (methodName.equals("isWrapperFor"))
            {
                return false;
            }
            if (methodName.equals("toString"))
            {
                return "MockResultSetMetaData";
            }
            if (methodName.equals("hashCode"))
            {
                return System.identityHashCode(proxy);
            }
            if (methodName.equals("equals"))
            {
                return proxy == args[0];
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class)
            {
                return false;
            }
            if (returnType == int.class)
            {
                return 0;
            }
            return null;
        };
        return (ResultSetMetaData) Proxy.newProxyInstance(TablePlanReadTest.class.getClassLoader(), new Class<?>[]
        {ResultSetMetaData.class}, handler);
    }

    private static RowSourceCsv csvRowSource(String csv)
    {
        return RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv")).withSeparator(',')
                .build();
    }
}
