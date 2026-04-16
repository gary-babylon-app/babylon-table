package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformConcatTest
{
    @Test
    void shouldConcatSourceColumnsUsingSeparator()
    {
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName LAST = ColumnName.of("Last");
        final ColumnName FULL = ColumnName.of("Full");

        ColumnObject.Builder<String> first = ColumnObject.builder(FIRST, app.babylon.table.column.ColumnTypes.STRING);
        first.add("Ada");
        first.add("Grace");

        ColumnObject.Builder<String> last = ColumnObject.builder(LAST, app.babylon.table.column.ColumnTypes.STRING);
        last.add("Lovelace");
        last.add("Hopper");

        TableColumnar table = Tables.newTable(TableName.of("t"), first.build(), last.build());

        TableColumnar transformed = table.apply(new TransformConcat(FULL, " ", FIRST, LAST));

        ColumnObject<String> full = transformed.getString(FULL);
        assertEquals("Ada Lovelace", full.get(0));
        assertEquals("Grace Hopper", full.get(1));
    }

    @Test
    void shouldBeAvailableFromFactory()
    {
        Transform transform = TransformConcat.of("Full", " ", "First", "Last");

        assertTrue(transform instanceof TransformConcat);
    }
}
