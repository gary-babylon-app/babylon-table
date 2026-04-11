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

public class TransformPrefixTest
{
    @Test
    public void shouldPrependPrefix()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnObject.Builder<String> strings = ColumnObject.builder(CODE, String.class);
        strings.add("ABC");
        strings.add("");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformPrefix("X-", CODE));

        ColumnObject<String> values = transformed.getString(CODE);
        assertEquals("X-ABC", values.get(0));
        assertFalse(values.isSet(1));
        assertFalse(values.isSet(2));
    }

    @Test
    public void shouldReplaceExistingColumnWhenNewColumnNameIsOmitted()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnObject.Builder<String> strings = ColumnObject.builder(CODE, String.class);
        strings.add("ABC");
        strings.add("XYZ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformPrefix("X-", CODE));

        ColumnObject<String> values = transformed.getString(CODE);
        assertEquals("X-ABC", values.get(0));
        assertEquals("X-XYZ", values.get(1));
    }

    @Test
    public void shouldPreserveCategoricalShape()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(CODE, String.class);
        strings.add("ABC");
        strings.add("ABC");
        strings.add("XYZ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformPrefix("X-", CODE));

        assertTrue(transformed.get(CODE) instanceof ColumnCategorical<?>);
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Prefix", "X-", "Code");
        assertTrue(transform instanceof TransformPrefix);
    }
}
