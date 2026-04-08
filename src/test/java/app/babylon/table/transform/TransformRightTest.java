package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.transform.Transform;

public class TransformRightTest
{
    @Test
    public void shouldTakeRightCharacters()
    {
        ColumnName from = ColumnName.of("Code");
        ColumnName to = ColumnName.of("Right");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("ABC123");
        strings.add("XY");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformRight(from, to, 3));

        ColumnObject<String> right = transformed.getString(to);
        assertEquals("123", right.get(0));
        assertEquals("XY", right.get(1));
        assertFalse(right.isSet(2));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Right", "Code", "Right", "3");

        assertTrue(transform instanceof TransformRight);
    }
}
