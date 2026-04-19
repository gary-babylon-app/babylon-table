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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

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
import app.babylon.table.io.RowSourceCsv;
import app.babylon.table.io.RowSourceResultSet;
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
        ColumnObject.Builder<String> builder = ColumnObject.builder(CODE, app.babylon.table.column.ColumnTypes.STRING);
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
        ColumnObject.Builder<String> builder = ColumnObject.builder(CODE, app.babylon.table.column.ColumnTypes.STRING);
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

        TablePlanRead plan = new TablePlanRead().withColumnType(AMOUNT, ColumnTypes.DOUBLE).withColumnType(NAME,
                ColumnTypes.STRING);

        assertEquals(app.babylon.table.column.ColumnTypes.DOUBLE, plan.getColumnType(AMOUNT));
        assertEquals(app.babylon.table.column.ColumnTypes.STRING, plan.getColumnType(NAME));
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
                .withColumnType(AMOUNT, ColumnTypes.DOUBLE).withTransform(new TransformToUpperCase(CODE));

        TableColumnar table = plan.execute(StreamSources.fromString(csv, "values.csv"), reader);

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
                ColumnTypes.DECIMAL);

        TableColumnar table = plan.execute(StreamSources.fromString(csv, "values.csv"), reader);

        assertEquals(TableName.of("BuiltFromCsv"), table.getName());
        assertEquals("abc", table.getString(CODE).get(0));
        assertEquals(0, new BigDecimal("10.50").compareTo(table.getDecimal(AMOUNT).get(0)));
        assertFalse(table.getDecimal(AMOUNT).isSet(1));
    }

    @Test
    void shouldIncludeResourceNameColumnForStreamSourceReads()
    {
        final ColumnName RESOURCE_NAME = ColumnName.of("ResourceName");
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = "Code,Amount\nabc,10.5\nxyz,20.0\n";

        TabularRowReaderCsv reader = new TabularRowReaderCsv().withSeparator(',');
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromCsv"))
                .withIncludeResourceName(RESOURCE_NAME);

        TableColumnar table = plan.execute(StreamSources.fromString(csv, "values.csv"), reader);

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
        String csv = "Code,Amount\nabc,10.5\nxyz,20.0\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .build();
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromRowSource"))
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
        String csv = "Code,Amount\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .build();
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromRowSource"))
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
    void shouldReadFromRowSourceUsingSupplierTypesAndTransforms()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = "Code,Amount\nabc,10.5\nxyz,20.0\n";

        StreamSource streamSource = StreamSources.fromString(csv, "values.csv");
        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(streamSource)
                .withColumnType(AMOUNT, ColumnTypes.DOUBLE).build();
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromRowSource"))
                .withTransform(new TransformToUpperCase(CODE));

        TableColumnar table = plan.execute(rowSource);

        assertEquals(TableName.of("BuiltFromRowSource"), table.getName());
        assertEquals("ABC", table.getString(CODE).get(0));
        assertEquals("XYZ", table.getString(CODE).get(1));
        assertEquals(10.5d, table.getDouble(AMOUNT).get(0));
        assertEquals(20.0d, table.getDouble(AMOUNT).get(1));
    }

    @Test
    void shouldTrimTrailingBlankDetectedHeaderColumns()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = "Code,Amount,\nabc,10.5,\ndef,\nxyz,20.0,\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withHeaderStrategy(new HeaderStrategyExplicitRow(0)).withColumnType(AMOUNT, ColumnTypes.DECIMAL)
                .build();

        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromRowSource"));

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
        String csv = "abc,10.5,\nxyz,20.0,\n";

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "values.csv"))
                .withHeaderStrategy(new HeaderStrategyNoHeaders(10)).build();

        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromRowSource"));

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
        PreparedStatement preparedStatement = preparedStatement(resultSet(new String[]
        {"Code", "Amount"}, new String[]
        {"Code", "Amount"}, new int[]
        {Types.VARCHAR, Types.DECIMAL}, new String[][]
        {
                {"abc", "10.50"},
                {"xyz", "20.00"}}));
        RowSourceResultSet rowSource = RowSourceResultSet.builder().withPreparedStatement(preparedStatement)
                .withName("TradesQuery").build();
        TablePlanRead plan = new TablePlanRead().withTableName(TableName.of("BuiltFromResultSet"))
                .withTransform(new TransformToUpperCase(CODE));

        TableColumnar table = plan.execute(rowSource);

        assertEquals(TableName.of("BuiltFromResultSet"), table.getName());
        assertEquals("ABC", table.getString(CODE).get(0));
        assertEquals("XYZ", table.getString(CODE).get(1));
        assertEquals(0, new BigDecimal("10.50").compareTo(table.getDecimal(AMOUNT).get(0)));
        assertEquals(0, new BigDecimal("20.00").compareTo(table.getDecimal(AMOUNT).get(1)));
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
}
