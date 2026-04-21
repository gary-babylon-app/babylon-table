package app.babylon.table;

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

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

class TableColumnarDbTest
{
    @Test
    void shouldReadResultSetDirectlyIntoTableUsingMetadataTypes()
    {
        final ColumnName ID = ColumnName.of("Id");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName NOTIONAL = ColumnName.of("Notional");
        final ColumnName RATIO = ColumnName.of("Ratio");
        final ColumnName CODE = ColumnName.of("Code");
        final TableName TRADES = TableName.of("Trades");

        ResultSet resultSet = resultSet(new String[]
        {"Id", "Amount", "Notional", "Ratio", "Code"}, new String[]
        {"Id", "Amount", "Notional", "Ratio", "Code"}, new int[]
        {Types.INTEGER, Types.DECIMAL, Types.BIGINT, Types.DOUBLE, Types.VARCHAR}, new Object[][]
        {
                {1, new BigDecimal("10.50"), 100L, 0.25d, "ABC"},
                {2, new BigDecimal("20.00"), 200L, 0.50d, "XYZ"}});

        TableColumnar table = Tables.fromResultSet(TRADES, resultSet);

        assertEquals(TRADES, table.getName());
        assertEquals(ColumnTypes.INT, table.getType(ID));
        assertEquals(ColumnTypes.DECIMAL, table.getType(AMOUNT));
        assertEquals(ColumnTypes.LONG, table.getType(NOTIONAL));
        assertEquals(ColumnTypes.DOUBLE, table.getType(RATIO));
        assertEquals(ColumnTypes.STRING, table.getType(CODE));
        assertEquals(1, table.getInt(ID).get(0));
        assertEquals(2, table.getInt(ID).get(1));
        assertEquals(0, new BigDecimal("10.50").compareTo(table.getDecimal(AMOUNT).get(0)));
        assertEquals(0, new BigDecimal("20.00").compareTo(table.getDecimal(AMOUNT).get(1)));
        assertEquals(100L, table.getLong(NOTIONAL).get(0));
        assertEquals(200L, table.getLong(NOTIONAL).get(1));
        assertEquals(0.25d, table.getDouble(RATIO).get(0));
        assertEquals(0.50d, table.getDouble(RATIO).get(1));
        assertEquals("ABC", table.getString(CODE).get(0));
        assertEquals("XYZ", table.getString(CODE).get(1));
    }

    @Test
    void shouldBuildTableColumnarDbUsingBuilder()
    {
        final ColumnName ID = ColumnName.of("Id");
        final TableName TRADES = TableName.of("Trades");
        final TableDescription DESCRIPTION = new TableDescription("Direct JDBC");

        ResultSet resultSet = resultSet(new String[]
        {"Id"}, new String[]
        {"Id"}, new int[]
        {Types.INTEGER}, new Object[][]
        {
                {1},
                {2}});

        TableColumnarDb table = TableColumnarDb.builder().withTableName(TRADES).withTableDescription(DESCRIPTION)
                .withResultSet(resultSet).build();

        assertEquals(TRADES, table.getName());
        assertEquals(DESCRIPTION, table.getDescription());
        assertEquals(ColumnTypes.INT, table.getType(ID));
        assertEquals(1, table.getInt(ID).get(0));
        assertEquals(2, table.getInt(ID).get(1));
    }

    @Test
    void shouldReadOnlySelectedColumnsUsingMetadataOrder()
    {
        final ColumnName ID = ColumnName.of("Id");
        final ColumnName RATIO = ColumnName.of("Ratio");
        final ColumnName CODE = ColumnName.of("Code");
        final TableName TRADES = TableName.of("Trades");

        ResultSet resultSet = resultSet(new String[]
        {"Id", "Amount", "Ratio", "Code"}, new String[]
        {"Id", "Amount", "Ratio", "Code"}, new int[]
        {Types.INTEGER, Types.DECIMAL, Types.DOUBLE, Types.VARCHAR}, new Object[][]
        {
                {1, new BigDecimal("10.50"), 0.25d, "ABC"},
                {2, new BigDecimal("20.00"), 0.50d, "XYZ"}});

        TableColumnarDb table = TableColumnarDb.builder().withTableName(TRADES).withResultSet(resultSet)
                .withSelectedColumn(CODE).withSelectedColumn(ID).build();

        assertEquals(2, table.getColumnNames().length);
        assertArrayEquals(new ColumnName[]
        {ID, CODE}, table.getColumnNames());
        assertEquals(ColumnTypes.INT, table.getType(ID));
        assertEquals(ColumnTypes.STRING, table.getType(CODE));
        assertEquals(1, table.getInt(ID).get(0));
        assertEquals(2, table.getInt(ID).get(1));
        assertEquals("ABC", table.getString(CODE).get(0));
        assertEquals("XYZ", table.getString(CODE).get(1));
        assertNull(table.get(RATIO));
    }

    @Test
    void shouldFailFastForUnsupportedMetadataTypes()
    {
        final ColumnName COMMENT = ColumnName.of("Comment");

        ResultSet resultSet = resultSet(new String[]
        {"Comment"}, new String[]
        {"Comment"}, new int[]
        {Types.CLOB}, new Object[][]
        {
                {"hello"}});

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Tables.fromResultSet(TableName.of("Dates"), resultSet));

        assertEquals("Unsupported ResultSet column type CLOB for column " + COMMENT, error.getMessage());
    }

    @Test
    void shouldReadLocalDateFromResultSetMetadata()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final TableName DATES = TableName.of("Dates");

        ResultSet resultSet = resultSet(new String[]
        {"TradeDate"}, new String[]
        {"TradeDate"}, new int[]
        {Types.DATE}, new Object[][]
        {
                {LocalDate.of(2026, 4, 21)},
                {LocalDate.of(2026, 4, 22)}});

        TableColumnar table = Tables.fromResultSet(DATES, resultSet);

        assertEquals(ColumnTypes.LOCALDATE, table.getType(TRADE_DATE));
        assertEquals(LocalDate.of(2026, 4, 21), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 22), table.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldReadLocalTimeFromResultSetMetadata()
    {
        final ColumnName TRADE_TIME = ColumnName.of("TradeTime");
        final TableName TIMES = TableName.of("Times");

        ResultSet resultSet = resultSet(new String[]
        {"TradeTime"}, new String[]
        {"TradeTime"}, new int[]
        {Types.TIME}, new Object[][]
        {
                {LocalTime.of(10, 15, 30)},
                {LocalTime.of(11, 45, 0)}});

        TableColumnar table = Tables.fromResultSet(TIMES, resultSet);

        assertEquals(ColumnTypes.LOCAL_TIME, table.getType(TRADE_TIME));
        assertEquals(LocalTime.of(10, 15, 30), table.getObject(TRADE_TIME, ColumnTypes.LOCAL_TIME).get(0));
        assertEquals(LocalTime.of(11, 45, 0), table.getObject(TRADE_TIME, ColumnTypes.LOCAL_TIME).get(1));
    }

    @Test
    void shouldReadLocalDateTimeFromResultSetMetadata()
    {
        final ColumnName LAST_UPDATED = ColumnName.of("LastUpdated");
        final TableName TIMESTAMPS = TableName.of("Timestamps");

        ResultSet resultSet = resultSet(new String[]
        {"LastUpdated"}, new String[]
        {"LastUpdated"}, new int[]
        {Types.TIMESTAMP}, new Object[][]
        {
                {LocalDateTime.of(2026, 4, 21, 10, 15, 30)},
                {LocalDateTime.of(2026, 4, 22, 11, 45, 0)}});

        TableColumnar table = Tables.fromResultSet(TIMESTAMPS, resultSet);

        assertEquals(ColumnTypes.LOCAL_DATE_TIME, table.getType(LAST_UPDATED));
        assertEquals(LocalDateTime.of(2026, 4, 21, 10, 15, 30),
                table.getObject(LAST_UPDATED, ColumnTypes.LOCAL_DATE_TIME).get(0));
        assertEquals(LocalDateTime.of(2026, 4, 22, 11, 45, 0),
                table.getObject(LAST_UPDATED, ColumnTypes.LOCAL_DATE_TIME).get(1));
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
        return (ResultSet) Proxy.newProxyInstance(TableColumnarDbTest.class.getClassLoader(), new Class<?>[]
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
        return (ResultSetMetaData) Proxy.newProxyInstance(TableColumnarDbTest.class.getClassLoader(), new Class<?>[]
        {ResultSetMetaData.class}, handler);
    }
}
