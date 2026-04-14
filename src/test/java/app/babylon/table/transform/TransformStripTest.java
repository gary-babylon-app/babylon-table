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

public class TransformStripTest
{
    @Test
    public void shouldStripStringColumnInPlace()
    {
        final ColumnName NAME = ColumnName.of("Name");

        ColumnObject.Builder<String> strings = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("  Alice  ");
        strings.add("\tBob\n");
        strings.add("   ");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformStrip(NAME));

        ColumnObject<String> stripped = transformed.getString(NAME);
        assertEquals("Alice", stripped.get(0));
        assertEquals("Bob", stripped.get(1));
        assertEquals("", stripped.get(2));
        assertFalse(stripped.isSet(3));
    }

    @Test
    public void shouldPreserveCategoricalShapeWhenWritingToNewColumn()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName STRIPPED = ColumnName.of("Stripped");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(NAME,
                app.babylon.table.column.ColumnTypes.STRING);
        strings.add("  Alice  ");
        strings.add("  Alice  ");
        strings.add(" Bob ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformStrip(NAME, STRIPPED));

        assertTrue(transformed.get(STRIPPED) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> stripped = transformed.getCategorical(STRIPPED);
        assertEquals("Alice", stripped.get(0));
        assertEquals("Alice", stripped.get(1));
        assertEquals("Bob", stripped.get(2));
        assertEquals(stripped.getCategoryCode(0), stripped.getCategoryCode(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Strip", "Name");

        assertTrue(transform instanceof TransformStrip);
    }
}
