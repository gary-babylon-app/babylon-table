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

public class TransformAfterTest
{
    @Test
    public void shouldTakeTextAfterDelimiter()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName AFTER = ColumnName.of("After");

        ColumnObject.Builder<String> strings = ColumnObject.builder(CODE, String.class);
        strings.add("ABC-123");
        strings.add("NoDash");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformAfter(CODE, AFTER, "-"));

        ColumnObject<String> after = transformed.getString(AFTER);
        assertEquals("123", after.get(0));
        assertFalse(after.isSet(1));
        assertFalse(after.isSet(2));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("After", "Code", "After", "-");

        assertTrue(transform instanceof TransformAfter);
    }
}
