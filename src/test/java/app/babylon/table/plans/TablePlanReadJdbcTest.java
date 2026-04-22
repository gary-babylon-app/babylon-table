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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.io.RowCursor;

class TablePlanReadJdbcTest
{
    @Test
    void shouldExecuteResultSetUsingJdbcPlan()
    {
        final ColumnName ID = ColumnName.of("Id");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final TableName TRADES = TableName.of("Trades");
        final TableDescription DESCRIPTION = new TableDescription("JDBC plan");

        ResultSet resultSet = resultSet(new String[]
        {"Id", "Amount"}, new String[]
        {"Id", "Amount"}, new int[]
        {Types.INTEGER, Types.DECIMAL}, new Object[][]
        {
                {1, new BigDecimal("10.50")},
                {2, new BigDecimal("20.00")}});

        TableColumnar table = new TablePlanReadJdbc().withTableName(TRADES).withTableDescription(DESCRIPTION)
                .execute(resultSet);

        assertEquals(TRADES, table.getName());
        assertEquals(DESCRIPTION, table.getDescription());
        assertEquals(ColumnTypes.INT, table.getType(ID));
        assertEquals(ColumnTypes.DECIMAL, table.getType(AMOUNT));
        assertEquals(1, table.getInt(ID).get(0));
        assertEquals(0, new BigDecimal("10.50").compareTo(table.getDecimal(AMOUNT).get(0)));
    }

    @Test
    void shouldSelectColumnsAndApplyStringObjectTypes()
    {
        final ColumnName CCY = ColumnName.of("Ccy");
        final ColumnName TENOR = ColumnName.of("Tenor");
        final ColumnName IGNORED = ColumnName.of("Ignored");
        final TableName TRADES = TableName.of("Trades");

        ResultSet resultSet = resultSet(new String[]
        {"Ccy", "Tenor", "Ignored"}, new String[]
        {"Ccy", "Tenor", "Ignored"}, new int[]
        {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}, new Object[][]
        {
                {"USD", "3M", "x"},
                {"EUR", "1Y", "y"}});

        TableColumnar table = new TablePlanReadJdbc().withTableName(TRADES).withSelectedColumn(TENOR)
                .withSelectedColumn(CCY).withColumnType(CCY, ColumnTypes.CURRENCY)
                .withColumnType(TENOR, ColumnTypes.PERIOD).execute(resultSet);

        assertArrayEquals(new ColumnName[]
        {CCY, TENOR}, table.getColumnNames());
        assertEquals(ColumnTypes.CURRENCY, table.getType(CCY));
        assertEquals(ColumnTypes.PERIOD, table.getType(TENOR));
        assertEquals(Currency.getInstance("USD"), table.getObject(CCY, ColumnTypes.CURRENCY).get(0));
        assertEquals(Period.ofMonths(3), table.getObject(TENOR, ColumnTypes.PERIOD).get(0));
        assertNull(table.get(IGNORED));
    }

    @Test
    void shouldReadTemporalMetadataTypes()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName TRADE_TIME = ColumnName.of("TradeTime");
        final ColumnName LAST_UPDATED = ColumnName.of("LastUpdated");
        final TableName TRADES = TableName.of("Trades");

        ResultSet resultSet = resultSet(new String[]
        {"TradeDate", "TradeTime", "LastUpdated"}, new String[]
        {"TradeDate", "TradeTime", "LastUpdated"}, new int[]
        {Types.DATE, Types.TIME, Types.TIMESTAMP}, new Object[][]
        {
                {LocalDate.of(2026, 4, 21), LocalTime.of(10, 15, 30), LocalDateTime.of(2026, 4, 21, 10, 15, 30)}});

        TableColumnar table = new TablePlanReadJdbc().withTableName(TRADES).execute(resultSet);

        assertEquals(ColumnTypes.LOCALDATE, table.getType(TRADE_DATE));
        assertEquals(ColumnTypes.LOCAL_TIME, table.getType(TRADE_TIME));
        assertEquals(ColumnTypes.LOCAL_DATE_TIME, table.getType(LAST_UPDATED));
        assertEquals(LocalDate.of(2026, 4, 21), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalTime.of(10, 15, 30), table.getObject(TRADE_TIME, ColumnTypes.LOCAL_TIME).get(0));
        assertEquals(LocalDateTime.of(2026, 4, 21, 10, 15, 30),
                table.getObject(LAST_UPDATED, ColumnTypes.LOCAL_DATE_TIME).get(0));
    }

    @Test
    void shouldRejectUnsupportedMetadataTypes()
    {
        final ColumnName COMMENT = ColumnName.of("Comment");
        final TableName TRADES = TableName.of("Trades");

        ResultSet resultSet = resultSet(new String[]
        {"Comment"}, new String[]
        {"Comment"}, new int[]
        {Types.CLOB}, new Object[][]
        {
                {"hello"}});

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new TablePlanReadJdbc().withTableName(TRADES).execute(resultSet));

        assertEquals("Unsupported ResultSet column type CLOB for column " + COMMENT, error.getMessage());
    }

    @Test
    void shouldRejectPrimitiveOverrideForStringSourceColumn()
    {
        final ColumnName ID = ColumnName.of("Id");
        final TableName TRADES = TableName.of("Trades");

        ResultSet resultSet = resultSet(new String[]
        {"Id"}, new String[]
        {"Id"}, new int[]
        {Types.VARCHAR}, new Object[][]
        {
                {"1"}});

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new TablePlanReadJdbc()
                .withTableName(TRADES).withColumnType(ID, ColumnTypes.INT).execute(resultSet));

        assertEquals("Explicit ResultSet primitive column type int for column " + ID
                + " is not supported from STRING source columns.", error.getMessage());
    }

    @Test
    void shouldRejectRowCursorExecution()
    {
        UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
                () -> new TablePlanReadJdbc().execute((RowCursor) null));

        assertEquals("TablePlanReadJdbc.execute(RowCursor) is not supported.", error.getMessage());
    }

    private static ResultSet resultSet(String[] labels, String[] names, int[] jdbcTypes, Object[][] rows)
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
                if (methodName.equals("getInt"))
                {
                    Object value = rows[this.rowIndex][((Integer) args[0]).intValue() - 1];
                    this.wasNull = value == null;
                    return this.wasNull ? 0 : ((Number) value).intValue();
                }
                if (methodName.equals("getLong"))
                {
                    Object value = rows[this.rowIndex][((Integer) args[0]).intValue() - 1];
                    this.wasNull = value == null;
                    return this.wasNull ? 0L : ((Number) value).longValue();
                }
                if (methodName.equals("getDouble"))
                {
                    Object value = rows[this.rowIndex][((Integer) args[0]).intValue() - 1];
                    this.wasNull = value == null;
                    return this.wasNull ? 0.0d : ((Number) value).doubleValue();
                }
                if (methodName.equals("getBigDecimal"))
                {
                    Object value = rows[this.rowIndex][((Integer) args[0]).intValue() - 1];
                    this.wasNull = value == null;
                    return value;
                }
                if (methodName.equals("getString"))
                {
                    Object value = rows[this.rowIndex][((Integer) args[0]).intValue() - 1];
                    this.wasNull = value == null;
                    return value == null ? null : value.toString();
                }
                if (methodName.equals("getObject") && args.length == 2)
                {
                    Object value = rows[this.rowIndex][((Integer) args[0]).intValue() - 1];
                    this.wasNull = value == null;
                    return value;
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
                if (returnType == double.class)
                {
                    return 0.0d;
                }
                return null;
            }
        };
        return (ResultSet) Proxy.newProxyInstance(TablePlanReadJdbcTest.class.getClassLoader(), new Class<?>[]
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
        return (ResultSetMetaData) Proxy.newProxyInstance(TablePlanReadJdbcTest.class.getClassLoader(), new Class<?>[]
        {ResultSetMetaData.class}, handler);
    }
}
