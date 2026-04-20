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
import app.babylon.table.column.ColumnTypes;

public class TransformSubstringTest
{
    @Test
    public void shouldExtractSubstringIntoNewColumn()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName PREFIX = ColumnName.of("Prefix");

        ColumnObject.Builder<String> strings = ColumnObject.builder(CODE, ColumnTypes.STRING);
        strings.add("ABC123");
        strings.add("XY");
        strings.add("");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformSubstring(CODE, PREFIX, 0, 3));

        ColumnObject<String> prefixes = transformed.getString(PREFIX);
        assertEquals("ABC", prefixes.get(0));
        assertFalse(prefixes.isSet(1));
        assertFalse(prefixes.isSet(2));
        assertFalse(prefixes.isSet(3));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Substring", "Code", "Prefix", "0", "3");

        assertTrue(transform instanceof TransformSubstring);
    }
}
