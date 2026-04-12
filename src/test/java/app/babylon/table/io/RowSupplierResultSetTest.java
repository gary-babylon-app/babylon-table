package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

class RowSupplierResultSetTest
{
    @Test
    void shouldExposeMetadataColumnsWithOptionalTypes()
    {
        RowSupplierResultSet supplier = new RowSupplierResultSet(resultSet(new String[]
        {"City", "Amount", "Count"}, new String[]
        {"City", "Amount", "Count"}, new int[]
        {Types.VARCHAR, Types.DECIMAL, Types.INTEGER}, new boolean[]
        {false, false}, new String[][]
        {
                {"London", "10.25", "3"}}));

        ColumnDefinition[] columns = supplier.columns();

        assertEquals(ColumnName.of("City"), columns[0].name());
        assertEquals(ColumnTypes.STRING, columns[0].type().orElseThrow());
        assertEquals(ColumnTypes.DECIMAL, columns[1].type().orElseThrow());
        assertEquals(ColumnTypes.INT, columns[2].type().orElseThrow());
    }

    @Test
    void shouldReadResultSetRowsUsingCharacterStreamsAndFallbackStrings()
    {
        RowSupplierResultSet supplier = new RowSupplierResultSet(resultSet(new String[]
        {"City", ""}, new String[]
        {"City", "Notes"}, new int[]
        {Types.VARCHAR, Types.LONGVARCHAR}, new boolean[]
        {false, true}, new String[][]
        {
                {"London", "warm and dry"},
                {"Paris", "cool breeze"}}));

        assertThrows(IllegalStateException.class, supplier::current);

        assertTrue(supplier.next());
        assertArrayEquals(new String[]
        {"London", "warm and dry"}, values(supplier.current()));

        assertTrue(supplier.next());
        assertArrayEquals(new String[]
        {"Paris", "cool breeze"}, values(supplier.current()));

        assertFalse(supplier.next());
    }

    private static String[] values(Row row)
    {
        String[] values = new String[row.fieldCount()];
        char[] chars = row.chars();
        for (int i = 0; i < row.fieldCount(); ++i)
        {
            values[i] = new String(chars, row.start(i), row.length(i));
        }
        return values;
    }

    private static ResultSet resultSet(String[] labels, String[] names, int[] jdbcTypes, boolean[] streamColumns,
            String[][] rows)
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
        return (ResultSet) Proxy.newProxyInstance(RowSupplierResultSetTest.class.getClassLoader(), new Class<?>[]
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
        return (ResultSetMetaData) Proxy.newProxyInstance(RowSupplierResultSetTest.class.getClassLoader(),
                new Class<?>[]
                {ResultSetMetaData.class}, handler);
    }
}
