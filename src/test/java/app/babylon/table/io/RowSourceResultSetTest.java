package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

class RowSourceResultSetTest
{
    @Test
    void shouldOpenRowsByExecutingPreparedStatementAndCloseOwnedResultSet() throws Exception
    {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean closed = new AtomicBoolean(false);
        ResultSet resultSet = resultSet(new String[]
        {"City", "Amount"}, new String[]
        {"City", "Amount"}, new int[]
        {Types.VARCHAR, Types.DECIMAL}, new boolean[]
        {false, false}, new String[][]
        {
                {"London", "10.25"}}, closed);
        PreparedStatement preparedStatement = preparedStatement(resultSet, executed);
        RowSourceResultSet rowSource = RowSourceResultSet.builder().withPreparedStatement(preparedStatement)
                .withName("TradesQuery").build();

        assertEquals("TradesQuery", rowSource.getName());
        try (RowSupplier supplier = rowSource.openRows())
        {
            assertTrue(executed.get());
            ColumnDefinition[] columns = supplier.columns();
            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(ColumnTypes.DECIMAL, columns[1].type().orElseThrow());
            assertTrue(supplier.next());
            assertEquals("London",
                    new String(supplier.current().chars(), supplier.current().start(0), supplier.current().length(0)));
            assertFalse(supplier.next());
        }
        assertTrue(closed.get());
    }

    private static PreparedStatement preparedStatement(ResultSet resultSet, AtomicBoolean executed)
    {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if (methodName.equals("executeQuery"))
            {
                executed.set(true);
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
        return (PreparedStatement) Proxy.newProxyInstance(RowSourceResultSetTest.class.getClassLoader(), new Class<?>[]
        {PreparedStatement.class}, handler);
    }

    private static ResultSet resultSet(String[] labels, String[] names, int[] jdbcTypes, boolean[] streamColumns,
            String[][] rows, AtomicBoolean closed)
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
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    String value = rows[this.rowIndex][columnIndex];
                    if (!streamColumns[columnIndex] || value == null)
                    {
                        return null;
                    }
                    return new StringReader(value);
                }
                if (methodName.equals("getString"))
                {
                    int columnIndex = ((Integer) args[0]).intValue() - 1;
                    return rows[this.rowIndex][columnIndex];
                }
                if (methodName.equals("close"))
                {
                    closed.set(true);
                    return null;
                }
                if (methodName.equals("isClosed"))
                {
                    return closed.get();
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
        return (ResultSet) Proxy.newProxyInstance(RowSourceResultSetTest.class.getClassLoader(), new Class<?>[]
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
        return (ResultSetMetaData) Proxy.newProxyInstance(RowSourceResultSetTest.class.getClassLoader(), new Class<?>[]
        {ResultSetMetaData.class}, handler);
    }
}
