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

public class TransformCleanWhitespaceTest
{
    @Test
    public void shouldTrimAndNormalizeInternalWhitespace()
    {
        final ColumnName NAME = ColumnName.of("Name");

        ColumnObject.Builder<String> strings = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("  Alice   Bob  ");
        strings.add("\tCarol\nDave\r\n");
        strings.add("   ");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformCleanWhitespace(NAME));

        ColumnObject<String> cleaned = transformed.getString(NAME);
        assertEquals("Alice Bob", cleaned.get(0));
        assertEquals("Carol Dave", cleaned.get(1));
        assertEquals("", cleaned.get(2));
        assertFalse(cleaned.isSet(3));
    }

    @Test
    public void shouldPreserveCategoricalShapeWhenWritingToNewColumn()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName CLEANED = ColumnName.of("Cleaned");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(NAME,
                app.babylon.table.column.ColumnTypes.STRING);
        strings.add("  Alice   Bob  ");
        strings.add("  Alice   Bob  ");
        strings.add(" Carol ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformCleanWhitespace(NAME, CLEANED));

        assertTrue(transformed.get(CLEANED) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> cleaned = transformed.getCategorical(CLEANED);
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
