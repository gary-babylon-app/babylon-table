package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.transform.Transform;

public class TransformCleanWhitespaceTest
{
    @Test
    public void shouldTrimAndNormalizeInternalWhitespace()
    {
        ColumnName columnName = ColumnName.of("Name");

        ColumnObject.Builder<String> strings = ColumnObject.builder(columnName, String.class);
        strings.add("  Alice   Bob  ");
        strings.add("\tCarol\nDave\r\n");
        strings.add("   ");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformCleanWhitespace(columnName));

        ColumnObject<String> cleaned = transformed.getString(columnName);
        assertEquals("Alice Bob", cleaned.get(0));
        assertEquals("Carol Dave", cleaned.get(1));
        assertEquals("", cleaned.get(2));
        assertFalse(cleaned.isSet(3));
    }

    @Test
    public void shouldPreserveCategoricalShapeWhenWritingToNewColumn()
    {
        ColumnName from = ColumnName.of("Name");
        ColumnName to = ColumnName.of("Cleaned");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(from, String.class);
        strings.add("  Alice   Bob  ");
        strings.add("  Alice   Bob  ");
        strings.add(" Carol ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformCleanWhitespace(from, to));

        assertTrue(transformed.get(to) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> cleaned = transformed.getCategorical(to);
        assertEquals("Alice Bob", cleaned.get(0));
        assertEquals("Alice Bob", cleaned.get(1));
        assertEquals("Carol", cleaned.get(2));
        assertEquals(cleaned.getCategoryCode(0), cleaned.getCategoryCode(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("CleanWhitespace", "Name");

        assertTrue(transform instanceof TransformCleanWhitespace);
    }
}
