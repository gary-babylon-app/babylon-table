package app.babylon.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TableIndexTest
{
    @Test
    void shouldExposeIndexedTableAndResolveRows()
    {
        ColumnObject.Builder<String> code = ColumnObject.builder(ColumnName.of("Code"), ColumnTypes.STRING);
        code.add("A");
        code.add("B");

        ColumnObject.Builder<String> name = ColumnObject.builder(ColumnName.of("Name"), ColumnTypes.STRING);
        name.add("Alpha");
        name.add("Beta");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), code.build(), name.build());
        TableIndex index = new TableIndex(table, ColumnName.of("Code"), ColumnName.of("Name"));

        assertSame(table, index.getTable());
        assertEquals(0, index.index("A", "Alpha"));
        assertEquals(1, index.index("B", "Beta"));
    }
}
