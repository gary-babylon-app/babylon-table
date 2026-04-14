package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformBeforeTest
{
    @Test
    public void shouldTakeTextBeforeDelimiter()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName BEFORE = ColumnName.of("Before");

        ColumnObject.Builder<String> strings = ColumnObject.builder(CODE, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ABC-123");
        strings.add("NoDash");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformBefore(CODE, BEFORE, "-"));

        ColumnObject<String> before = transformed.getString(BEFORE);
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
