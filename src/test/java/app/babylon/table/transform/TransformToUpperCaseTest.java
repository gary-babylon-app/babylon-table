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
        final ColumnName NAME = ColumnName.of("Name");

        ColumnObject.Builder<String> strings = ColumnObject.builder(NAME, String.class);
        strings.add("Alice");
        strings.add("Bob");
        strings.add("");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToUpperCase(NAME));

        ColumnObject<String> upper = transformed.getString(NAME);
        assertEquals("ALICE", upper.get(0));
        assertEquals("BOB", upper.get(1));
        assertEquals("", upper.get(2));
        assertFalse(upper.isSet(3));
    }

    @Test
    public void shouldPreserveCategoricalShapeWhenWritingToNewColumn()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName UPPER = ColumnName.of("Upper");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(NAME, String.class);
        strings.add("Alice");
        strings.add("Alice");
        strings.add("Bob");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToUpperCase(NAME, UPPER));

        assertTrue(transformed.get(UPPER) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> upper = transformed.getCategorical(UPPER);
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
