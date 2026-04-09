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

public class TransformLeftTest
{
    @Test
    public void shouldTakeLeftCharacters()
    {
        ColumnName from = ColumnName.of("Code");
        ColumnName to = ColumnName.of("Left");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("ABC123");
        strings.add("XY");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformLeft(from, to, 3));

        ColumnObject<String> left = transformed.getString(to);
        assertEquals("ABC", left.get(0));
        assertEquals("XY", left.get(1));
        assertFalse(left.isSet(2));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Left", "Code", "Left", "3");

        assertTrue(transform instanceof TransformLeft);
    }
}
