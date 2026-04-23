package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;

class ColumnsComparatorTest
{
    @Test
    void comparesBoxedRowIndexes()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Paris");
        names.add("Berlin");
        names.add("Rome");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());
        ColumnsComparator comparator = new ColumnsComparator(table, NAME);

        assertTrue(comparator.compare(Integer.valueOf(0), Integer.valueOf(1)) > 0);
        assertTrue(comparator.compare(Integer.valueOf(1), Integer.valueOf(2)) < 0);
    }
}
