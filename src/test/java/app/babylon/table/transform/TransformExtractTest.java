package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformExtractTest
{
    @Test
    public void shouldExtractFirstGroupFromStringColumn()
    {
        ColumnName from = ColumnName.of("Description");
        ColumnName to = ColumnName.of("Symbol");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("ABC (VEVE)");
        strings.add("No match");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformExtract(from, Pattern.compile(".*\\(([^)]+)\\)"), to));

        ColumnObject<String> extracted = transformed.getString(to);
        assertEquals("VEVE", extracted.get(0));
        assertFalse(extracted.isSet(1));
        assertFalse(extracted.isSet(2));
    }

    @Test
    public void shouldPreserveCategoricalShape()
    {
        ColumnName from = ColumnName.of("Description");
        ColumnName to = ColumnName.of("Symbol");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(from, String.class);
        strings.add("ABC (VEVE)");
        strings.add("ABC (VEVE)");
        strings.add("XYZ (SGLN)");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformExtract(from, Pattern.compile(".*\\(([^)]+)\\)"), to));

        assertTrue(transformed.get(to) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> extracted = transformed.getCategorical(to);
        assertEquals("VEVE", extracted.get(0));
        assertEquals("VEVE", extracted.get(1));
        assertEquals("SGLN", extracted.get(2));
        assertEquals(extracted.getCategoryCode(0), extracted.getCategoryCode(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Extract", "Description", "Symbol", ".*\\(([^)]+)\\)");

        assertTrue(transform instanceof TransformExtract);
    }
}
