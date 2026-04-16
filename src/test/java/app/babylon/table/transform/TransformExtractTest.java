package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        final ColumnName DESCRIPTION = ColumnName.of("Description");

        ColumnObject.Builder<String> strings = ColumnObject.builder(DESCRIPTION,
                app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ABC (VEVE)");
        strings.add("No match");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformExtract.of(DESCRIPTION, Pattern.compile(".*\\(([^)]+)\\)")));

        ColumnObject<String> extracted = transformed.getString(DESCRIPTION);
        assertEquals("VEVE", extracted.get(0));
        assertFalse(extracted.isSet(1));
        assertFalse(extracted.isSet(2));
    }

    @Test
    public void shouldKeepOriginalColumnNameWhenNoNewColumnNameIsProvided()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");

        ColumnObject.Builder<String> strings = ColumnObject.builder(DESCRIPTION,
                app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ABC (VEVE)");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformExtract.of(DESCRIPTION, Pattern.compile(".*\\(([^)]+)\\)")));

        ColumnObject<String> extracted = transformed.getString(DESCRIPTION);
        assertEquals(DESCRIPTION, extracted.getName());
        assertEquals("VEVE", extracted.get(0));
    }

    @Test
    public void shouldPreserveCategoricalShape()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName SYMBOL = ColumnName.of("Symbol");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(DESCRIPTION,
                app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ABC (VEVE)");
        strings.add("ABC (VEVE)");
        strings.addNull();
        strings.add("XYZ (SGLN)");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformExtract.of("Description", "Symbol", ".*\\(([^)]+)\\)"));

        assertTrue(transformed.get(SYMBOL) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> extracted = transformed.getCategorical(SYMBOL);
        assertEquals("VEVE", extracted.get(0));
        assertEquals("VEVE", extracted.get(1));
        assertFalse(extracted.isSet(2));
        assertEquals("SGLN", extracted.get(3));
        assertEquals(extracted.getCategoryCode(0), extracted.getCategoryCode(1));
    }

    @Test
    public void shouldExtractFromArrayBackedStringColumnWithUnsetRows()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName SYMBOL = ColumnName.of("Symbol");

        ColumnObject.Builder<String> strings = ColumnObject.builder(DESCRIPTION,
                app.babylon.table.column.ColumnTypes.STRING, ColumnObject.Mode.ARRAY);
        strings.add("ABC (VEVE)");
        strings.addNull();
        strings.add("XYZ (SGLN)");
        strings.add("LMN (NOVA)");

        ColumnObject<String> source = strings.build();
        assertFalse(source instanceof ColumnCategorical<?>);

        TableColumnar table = Tables.newTable(TableName.of("t"), source);

        TableColumnar transformed = table.apply(TransformExtract.of("Description", "Symbol", ".*\\(([^)]+)\\)"));

        ColumnObject<String> extracted = transformed.getString(SYMBOL);
        assertEquals("VEVE", extracted.get(0));
        assertFalse(extracted.isSet(1));
        assertEquals("SGLN", extracted.get(2));
        assertEquals("NOVA", extracted.get(3));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Extract", "Description", "Symbol", ".*\\(([^)]+)\\)");

        assertTrue(transform instanceof TransformExtract);
    }

    @Test
    public void factoriesShouldRejectNullInputs()
    {
        assertNull(TransformExtract.of((ColumnName) null, Pattern.compile("x")));
        assertNull(TransformExtract.of(ColumnName.of("Description"), null));
        assertNull(TransformExtract.of(new String[0]));
    }
}
