package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformToUpperCaseTest
{
    @Test
    public void shouldConvertStringsToUpperCase()
    {
        ColumnName columnName = ColumnName.of("Name");

        ColumnObject.Builder<String> strings = ColumnObject.builder(columnName, String.class);
        strings.add("Alice");
        strings.add("Bob");
        strings.add("");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToUpperCase(columnName));

        ColumnObject<String> upper = transformed.getString(columnName);
        assertEquals("ALICE", upper.get(0));
        assertEquals("BOB", upper.get(1));
        assertEquals("", upper.get(2));
        assertFalse(upper.isSet(3));
    }

    @Test
    public void shouldPreserveCategoricalShapeWhenWritingToNewColumn()
    {
        ColumnName from = ColumnName.of("Name");
        ColumnName to = ColumnName.of("Upper");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(from, String.class);
        strings.add("Alice");
        strings.add("Alice");
        strings.add("Bob");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToUpperCase(from, to));

        assertTrue(transformed.get(to) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> upper = transformed.getCategorical(to);
        assertEquals("ALICE", upper.get(0));
        assertEquals("ALICE", upper.get(1));
        assertEquals("BOB", upper.get(2));
        assertEquals(upper.getCategoryCode(0), upper.getCategoryCode(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("ToUpperCase", "Name");

        assertTrue(transform instanceof TransformToUpperCase);
    }
}
