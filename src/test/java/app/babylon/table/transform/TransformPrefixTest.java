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
        ColumnName columnName = ColumnName.of("Code");
        ColumnObject.Builder<String> strings = ColumnObject.builder(columnName, String.class);
        strings.add("ABC");
        strings.add("");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformPrefix("X-", columnName));

        ColumnObject<String> values = transformed.getString(columnName);
        assertEquals("X-ABC", values.get(0));
        assertFalse(values.isSet(1));
        assertFalse(values.isSet(2));
    }

    @Test
    public void shouldReplaceExistingColumnWhenNewColumnNameIsOmitted()
    {
        ColumnName columnName = ColumnName.of("Code");
        ColumnObject.Builder<String> strings = ColumnObject.builder(columnName, String.class);
        strings.add("ABC");
        strings.add("XYZ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformPrefix("X-", columnName));

        ColumnObject<String> values = transformed.getString(columnName);
        assertEquals("X-ABC", values.get(0));
        assertEquals("X-XYZ", values.get(1));
    }

    @Test
    public void shouldPreserveCategoricalShape()
    {
        ColumnName columnName = ColumnName.of("Code");
        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(columnName, String.class);
        strings.add("ABC");
        strings.add("ABC");
        strings.add("XYZ");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());
        TableColumnar transformed = table.apply(new TransformPrefix("X-", columnName));

        assertTrue(transformed.get(columnName) instanceof ColumnCategorical<?>);
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Prefix", "X-", "Code");
        assertTrue(transform instanceof TransformPrefix);
    }
}
