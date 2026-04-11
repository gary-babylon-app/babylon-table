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

public class TransformClassifyTest
{
    @Test
    public void shouldClassifyPlainStringColumn()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName INDICATOR = ColumnName.of("Indicator");

        ColumnObject.Builder<String> strings = ColumnObject.builder(DESCRIPTION, String.class);
        strings.add("ABC (VEVE)");
        strings.add("No match");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformClassify(DESCRIPTION, INDICATOR, Pattern.compile("\\([^)]+\\)"), "Y", "N"));

        ColumnObject<String> classified = transformed.getString(INDICATOR);
        assertEquals("Y", classified.get(0));
        assertEquals("N", classified.get(1));
        assertFalse(classified.isSet(2));
    }

    @Test
    public void shouldPreserveCategoricalShape()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName INDICATOR = ColumnName.of("Indicator");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(DESCRIPTION, String.class);
        strings.add("ABC (VEVE)");
        strings.add("ABC (VEVE)");
        strings.add("No match");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformClassify(DESCRIPTION, INDICATOR, Pattern.compile("\\([^)]+\\)"), "Y", "N"));

        assertTrue(transformed.get(INDICATOR) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> classified = transformed.getCategorical(INDICATOR);
        assertEquals("Y", classified.get(0));
        assertEquals("Y", classified.get(1));
        assertEquals("N", classified.get(2));
        assertEquals(classified.getCategoryCode(0), classified.getCategoryCode(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Classify", "Description", "Indicator", "\\([^)]+\\)", "Y",
                "N");

        assertTrue(transform instanceof TransformClassify);
    }
}
