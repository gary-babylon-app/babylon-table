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
import app.babylon.table.column.ColumnTypes;

public class TransformCleanTest
{
    @Test
    public void shouldTrimAndNormalizeInternalWhitespace()
    {
        final ColumnName NAME = ColumnName.of("Name");

        ColumnObject.Builder<String> strings = ColumnObject.builder(NAME, ColumnTypes.STRING);
        strings.add("  Alice   Bob  ");
        strings.add("\tCarol\nDave\r\n");
        strings.add("\uFEFF Eve\u00A0\u00A0Finch \u200B");
        strings.add("   ");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformClean(NAME));

        ColumnObject<String> cleaned = transformed.getString(NAME);
        assertEquals("Alice Bob", cleaned.get(0));
        assertEquals("Carol Dave", cleaned.get(1));
        assertEquals("Eve Finch", cleaned.get(2));
        assertEquals("", cleaned.get(3));
        assertFalse(cleaned.isSet(4));
    }

    @Test
    public void shouldPreserveCategoricalShapeWhenWritingToNewColumn()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName CLEANED = ColumnName.of("Cleaned");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(NAME, ColumnTypes.STRING);
        strings.add("  Alice   Bob  ");
        strings.add("  Alice   Bob  ");
        strings.add(" Carol ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformClean(NAME, CLEANED));

        assertTrue(transformed.get(CLEANED) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> cleaned = transformed.getCategorical(CLEANED);
        assertEquals("Alice Bob", cleaned.get(0));
        assertEquals("Alice Bob", cleaned.get(1));
        assertEquals("Carol", cleaned.get(2));
        assertEquals(cleaned.getCategoryCode(0), cleaned.getCategoryCode(1));
    }

    @Test
    public void shouldRemoveConfiguredCharactersEverywhereAfterCleaningWhitespace()
    {
        final ColumnName ACCOUNT = ColumnName.of("Account");
        final ColumnName CLEANED = ColumnName.of("Cleaned");

        ColumnObject.Builder<String> strings = ColumnObject.builder(ACCOUNT, ColumnTypes.STRING);
        strings.add("  12-34\t56  ");
        strings.add("AB CD-EF");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformClean.of(ACCOUNT, CLEANED, " -"));

        ColumnObject<String> cleaned = transformed.getString(CLEANED);
        assertEquals("123456", cleaned.get(0));
        assertEquals("ABCDEF", cleaned.get(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Clean", "Name");

        assertTrue(transform instanceof TransformClean);
    }

    @Test
    public void shouldKeepCleanWhitespaceRegistryNameForBackwardCompatibility()
    {
        Transform transform = Transforms.registry().create("CleanWhitespace", "Name");

        assertTrue(transform instanceof TransformClean);
    }
}
