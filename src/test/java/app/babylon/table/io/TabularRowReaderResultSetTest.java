package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.column.ColumnName;

class TabularRowReaderResultSetTest
{
    @Test
    void shouldReadResultSetIntoTable()
    {
        final ColumnName CITY = ColumnName.of("City");
        final ColumnName NOTES = ColumnName.of("Notes");
        ResultSet resultSet = resultSet(new String[]
        {"City", "Notes"}, new String[]
        {"City", "Notes"}, new boolean[]
        {false, true}, new String[][]
        {
                {"London", "warm and dry"},
                {"Paris", "cool breeze"}});
        TabularRowReaderResultSet reader = new TabularRowReaderResultSet().withTransferBufferSize(4);

        TableRead read = readTable(reader, resultSet);

        assertEquals(TabularRowReader.Status.SUCCESS, read.result.getStatus());
        assertEquals(2, read.table.getRowCount());
        assertEquals("London", read.table.getString(CITY).get(0));
        assertEquals("cool breeze", read.table.getString(NOTES).get(1));
    }

    @Test
    void shouldFallbackToColumnNameAndApplyProjectionRenameAndFilter()
    {
        final ColumnName TRADE_DATE = ColumnName.of("Trade Date");
        final ColumnName DATE = ColumnName.of("Date");
        ResultSet resultSet = resultSet(new String[]
        {"", "Amount"}, new String[]
        {"Trade Date", "Amount"}, new boolean[]
        {true, false}, new String[][]
        {
                {"2026-01-01", "10.25"},
                {null, "99.99"}});
        TabularRowReaderResultSet reader = new TabularRowReaderResultSet().withSelectedColumn(TRADE_DATE)
                .withColumnRename(TRADE_DATE, DATE).withRowFilter(RowFilters.excludeEmpty(DATE));

        TableRead read = readTable(reader, resultSet);

        assertEquals(TabularRowReader.Status.SUCCESS, read.result.getStatus());
        assertEquals(1, read.table.getColumnCount());
        assertEquals(1, read.table.getRowCount());
        assertEquals("2026-01-01", read.table.getString(DATE).get(0));
    }

    @Test
    void shouldExposeTransferBufferConfiguration()
    {
        TabularRowReaderResultSet reader = new TabularRowReaderResultSet().withTransferBufferSize(128);

        assertEquals(128, reader.getTransferBufferSize());
    }

    private static TableRead readTable(TabularRowReaderResultSet reader, ResultSet resultSet)
    {
        RowConsumerCreateTable rowConsumer = RowConsumerCreateTable.create(TableName.of("ResultSetRead"), null);
        TabularRowReader.Result result = reader.read(resultSet, rowConsumer);
        return new TableRead(result, rowConsumer.build());
    }

    private static ResultSet resultSet(String[] labels, String[] names, boolean[] streamColumns, String[][] rows)
    {
        ResultSetMetaData metaData = resultSetMetaData(labels, names);
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
        return (ResultSet) Proxy.newProxyInstance(TabularRowReaderResultSetTest.class.getClassLoader(), new Class<?>[]
        {ResultSet.class}, handler);
    }

    private static ResultSetMetaData resultSetMetaData(String[] labels, String[] names)
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
        return (ResultSetMetaData) Proxy.newProxyInstance(TabularRowReaderResultSetTest.class.getClassLoader(),
                new Class<?>[]
                {ResultSetMetaData.class}, handler);
    }

    private record TableRead(TabularRowReader.Result result, TableColumnar table)
    {
    }
}
