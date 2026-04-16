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
        ColumnObject.Builder<String> names = ColumnObject.builder(ColumnName.of("Name"), ColumnTypes.STRING);
        names.add("Paris");
        names.add("Berlin");
        names.add("Rome");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());
        ColumnsComparator comparator = new ColumnsComparator(table, ColumnName.of("Name"));

        assertTrue(comparator.compare(Integer.valueOf(0), Integer.valueOf(1)) > 0);
        assertTrue(comparator.compare(Integer.valueOf(1), Integer.valueOf(2)) < 0);
    }
}
