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

public class TransformToLowerCaseTest
{
    @Test
    public void shouldConvertStringsToLowerCase()
    {
        final ColumnName NAME = ColumnName.of("Name");

        ColumnObject.Builder<String> strings = ColumnObject.builder(NAME, String.class);
        strings.add("ALICE");
        strings.add("Bob");
        strings.add("");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToLowerCase(NAME));

        ColumnObject<String> lower = transformed.getString(NAME);
        assertEquals("alice", lower.get(0));
        assertEquals("bob", lower.get(1));
        assertEquals("", lower.get(2));
        assertFalse(lower.isSet(3));
    }

    @Test
    public void shouldPreserveCategoricalShapeWhenWritingToNewColumn()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName LOWER = ColumnName.of("Lower");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(NAME, String.class);
        strings.add("ALICE");
        strings.add("ALICE");
        strings.add("Bob");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToLowerCase(NAME, LOWER));

        assertTrue(transformed.get(LOWER) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> lower = transformed.getCategorical(LOWER);
        assertEquals("alice", lower.get(0));
        assertEquals("alice", lower.get(1));
        assertEquals("bob", lower.get(2));
        assertEquals(lower.getCategoryCode(0), lower.getCategoryCode(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("ToLowerCase", "Name");

        assertTrue(transform instanceof TransformToLowerCase);
    }
}
