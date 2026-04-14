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

public class TransformSuffixTest
{
    @Test
    public void shouldAppendSuffix()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnObject.Builder<String> strings = ColumnObject.builder(CODE, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ABC");
        strings.add("");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformSuffix("-X", CODE));

        ColumnObject<String> values = transformed.getString(CODE);
        assertEquals("ABC-X", values.get(0));
        assertFalse(values.isSet(1));
        assertFalse(values.isSet(2));
    }

    @Test
    public void shouldPreserveCategoricalShape()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(CODE,
                app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ABC");
        strings.add("ABC");
        strings.add("XYZ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformSuffix("-X", CODE));

        assertTrue(transformed.get(CODE) instanceof ColumnCategorical<?>);
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Suffix", "-X", "Code");
        assertTrue(transform instanceof TransformSuffix);
    }
}
