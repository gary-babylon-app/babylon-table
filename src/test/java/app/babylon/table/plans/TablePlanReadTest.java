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

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

import org.junit.jupiter.api.Test;

import app.babylon.io.DataSource;
import app.babylon.io.DataSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
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

    @Test
    void shouldReadFromRowSourceUsingSupplierTypesAndTransforms()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = "Code,Amount\nabc,10.5\nxyz,20.0\n";

        DataSource dataSource = DataSources.fromString(csv, "values.csv");
        RowSourceCsv rowSource = RowSourceCsv.builder().withDataSource(dataSource)
                .withColumnType(AMOUNT, app.babylon.table.column.ColumnTypes.DOUBLE).build();
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
                    return rows[this.rowIndex][columnIndex];
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
