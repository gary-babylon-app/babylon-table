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

public class TransformSplitTest
{
    @Test
    public void shouldSplitOneColumnIntoManyColumns()
    {
        ColumnName from = ColumnName.of("Pair");
        ColumnName left = ColumnName.of("Left");
        ColumnName middle = ColumnName.of("Middle");
        ColumnName right = ColumnName.of("Right");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("A|B|C");
        strings.add("D||F");
        strings.add("G|H");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformSplit(from, "|", left, middle, right));

        ColumnObject<String> lefts = transformed.getString(left);
        ColumnObject<String> middles = transformed.getString(middle);
        ColumnObject<String> rights = transformed.getString(right);

        assertEquals("A", lefts.get(0));
        assertEquals("B", middles.get(0));
        assertEquals("C", rights.get(0));

        assertEquals("D", lefts.get(1));
        assertEquals("", middles.get(1));
        assertEquals("F", rights.get(1));

        assertEquals("G", lefts.get(2));
        assertEquals("H", middles.get(2));
        assertFalse(rights.isSet(2));

        assertFalse(lefts.isSet(3));
        assertFalse(middles.isSet(3));
        assertFalse(rights.isSet(3));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Split", "Pair", "|", "Left", "Right");

        assertTrue(transform instanceof TransformSplit);
    }
}
