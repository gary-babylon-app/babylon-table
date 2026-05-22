package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformDropWordTest
{
    @Test
    void shouldDropFirstWord()
    {
        ColumnName description = ColumnName.of("Description");
        ColumnName clean = ColumnName.of("Clean");
        ColumnObject.Builder<String> descriptions = ColumnObject.builder(description, ColumnTypes.STRING);
        descriptions.add("Bought Satrix MSCI Emerging Markets ETF 0.9989");
        descriptions.add("Bought");
        descriptions.addNull();

        TableColumnar transformed = Tables.newTable(TableName.of("t"), descriptions.build())
                .apply(TransformDropWord.first(description, clean));

        assertEquals("Satrix MSCI Emerging Markets ETF 0.9989", transformed.getString(clean).get(0));
        assertFalse(transformed.getString(clean).isSet(1));
        assertFalse(transformed.getString(clean).isSet(2));
    }

    @Test
    void shouldDropLastWord()
    {
        ColumnName description = ColumnName.of("Description");
        ColumnName clean = ColumnName.of("Clean");
        ColumnObject.Builder<String> descriptions = ColumnObject.builder(description, ColumnTypes.STRING);
        descriptions.add("Satrix MSCI Emerging Markets ETF 0.9989");
        descriptions.add("Satrix");
        descriptions.addNull();

        TableColumnar transformed = Tables.newTable(TableName.of("t"), descriptions.build())
                .apply(TransformDropWord.last(description, clean));

        assertEquals("Satrix MSCI Emerging Markets ETF", transformed.getString(clean).get(0));
        assertFalse(transformed.getString(clean).isSet(1));
        assertFalse(transformed.getString(clean).isSet(2));
    }

    @Test
    void factoriesShouldCreateWorkingTransforms()
    {
        ColumnName description = ColumnName.of("Description");
        ColumnName clean = ColumnName.of("Clean");

        assertNotNull(TransformDropWord.of(description, clean, TransformDropWord.Position.FIRST));
        assertNotNull(TransformDropWord.of(description, null, TransformDropWord.Position.FIRST));
        assertNotNull(TransformDropWord.of("Description", "Clean", "LAST"));

        assertNull(TransformDropWord.of((ColumnName) null, clean, TransformDropWord.Position.FIRST));
        assertNull(TransformDropWord.of(description, clean, null));
        assertNull(TransformDropWord.of(new String[0]));
    }

    @Test
    void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("DropWord", "Description", "Clean", "LAST");

        assertTrue(transform instanceof TransformDropWord);
    }
}
