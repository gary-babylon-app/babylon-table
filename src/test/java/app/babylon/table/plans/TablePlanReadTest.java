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

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.io.DataSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.io.TabularRowReaderCsv;
import app.babylon.table.transform.TransformAfter;
import app.babylon.table.transform.TransformPrefix;
import app.babylon.table.transform.TransformToUpperCase;

class TablePlanReadTest
{
    @Test
    void shouldApplyTransformsInInsertionOrder()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnObject.Builder<String> builder = ColumnObject.builder(CODE, String.class);
        builder.add("A/B");

        TablePlanRead plan = new TablePlanRead().withTransform(new TransformAfter(CODE, CODE, "/"))
                .withTransform(new TransformPrefix("X-", CODE));

        TableColumnar table = plan.execute(TableName.of("Codes"), builder.build());

        assertEquals("X-B", table.getString(CODE).get(0));
    }

    @Test
    void shouldUseConfiguredOutputMetadataWhenExecutingExistingTable()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnObject.Builder<String> builder = ColumnObject.builder(CODE, String.class);
        builder.add("ABC");

        TableColumnar source = Tables.newTable(TableName.of("Source"), new TableDescription("Source description"),
                builder.build());
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("Built"))
                .withTableDescription(new TableDescription("Built description"));

        TableColumnar built = plan.execute(source);

        assertEquals(TableName.of("Built"), built.getName());
        assertEquals("Built description", built.getDescription().getValue());
        assertEquals("ABC", built.getString(CODE).get(0));
    }

    @Test
    void shouldExposeConfiguredColumnTypes()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName NAME = ColumnName.of("Name");

        TablePlanRead plan = new TablePlanRead().withColumnType(AMOUNT, double.class).withColumnType(NAME,
                String.class);

        assertEquals(app.babylon.table.column.Column.Type.of(double.class), plan.getColumnType(AMOUNT));
        assertEquals(app.babylon.table.column.Column.Type.of(String.class), plan.getColumnType(NAME));
        assertEquals(2, plan.getColumnTypes().size());
    }

    @Test
    void shouldReadFromCsvDataSourceUsingPlanTypesAndTransforms()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = """
                Code,Amount
                abc,10.5
                xyz,20.0
                """;

        TabularRowReaderCsv reader = new TabularRowReaderCsv().withSeparator(',');
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromCsv"))
                .withColumnType(AMOUNT, double.class).withTransform(new TransformToUpperCase(CODE));

        TableColumnar table = plan.execute(DataSources.fromString(csv, "values.csv"), reader);

        assertEquals(TableName.of("BuiltFromCsv"), table.getName());
        assertEquals("ABC", table.getString(CODE).get(0));
        assertEquals("XYZ", table.getString(CODE).get(1));
        assertEquals(10.5d, table.getDouble(AMOUNT).get(0));
        assertEquals(20.0d, table.getDouble(AMOUNT).get(1));
    }

    @Test
    void shouldReadBigDecimalColumnsFromCsvUsingCharSliceBuilder()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = """
                Code,Amount
                abc,10.50
                xyz,
                """;

        TabularRowReaderCsv reader = new TabularRowReaderCsv().withSeparator(',');
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromCsv")).withColumnType(AMOUNT,
                BigDecimal.class);

        TableColumnar table = plan.execute(DataSources.fromString(csv, "values.csv"), reader);

        assertEquals(TableName.of("BuiltFromCsv"), table.getName());
        assertEquals("abc", table.getString(CODE).get(0));
        assertEquals(0, new BigDecimal("10.50").compareTo(table.getDecimal(AMOUNT).get(0)));
        assertFalse(table.getDecimal(AMOUNT).isSet(1));
    }
}
