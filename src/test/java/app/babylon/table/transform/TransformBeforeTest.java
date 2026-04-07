package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.Transform;

public class TransformBeforeTest
{
    @Test
    public void shouldTakeTextBeforeDelimiter()
    {
        ColumnName from = ColumnName.of("Code");
        ColumnName to = ColumnName.of("Before");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("ABC-123");
        strings.add("NoDash");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformBefore(from, to, "-"));

        ColumnObject<String> before = transformed.getString(to);
        assertEquals("ABC", before.get(0));
        assertFalse(before.isSet(1));
        assertFalse(before.isSet(2));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Before", "Code", "Before", "-");

        assertTrue(transform instanceof TransformBefore);
    }
}
