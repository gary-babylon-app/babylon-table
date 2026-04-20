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
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> code = ColumnObject.builder(CODE, ColumnTypes.STRING);
        code.add("A");
        code.add("B");

        ColumnObject.Builder<String> name = ColumnObject.builder(NAME, ColumnTypes.STRING);
        name.add("Alpha");
        name.add("Beta");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), code.build(), name.build());
        TableIndex index = new TableIndex(table, CODE, NAME);

        assertSame(table, index.getTable());
        assertEquals(0, index.index("A", "Alpha"));
        assertEquals(1, index.index("B", "Beta"));
    }
}
