package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnDouble.KeyedDouble;
import app.babylon.table.column.ColumnInt.KeyedInt;
import app.babylon.table.column.ColumnObject.KeyedObject;

class ColumnKeyedValueTest
{
    private static final ColumnName NAME = ColumnName.of("Value");

    @Test
    void getKeyedValue_intPreservesUnsetState()
    {
        ColumnInt.Builder builder = ColumnInt.builder(NAME);
        builder.add(7);
        builder.addNull();
        ColumnInt column = builder.build();

        KeyedInt set = column.getKeyedValue(0);
        assertEquals(NAME, set.key());
        assertEquals(7, set.value());
        assertTrue(set.isSet());

        KeyedInt unset = column.getKeyedValue(1);
        assertEquals(NAME, unset.key());
        assertEquals(0, unset.value());
        assertFalse(unset.isSet());
    }

    @Test
    void firstKeyedAndLastKeyed_returnPrimitiveValues()
    {
        ColumnDouble.Builder builder = ColumnDouble.builder(NAME);
        builder.add(1.5);
        builder.add(2.5);
        ColumnDouble column = builder.build();

        KeyedDouble first = column.firstKeyed();
        KeyedDouble last = column.lastKeyed();

        assertEquals(1.5, first.value());
        assertTrue(first.isSet());
        assertEquals(2.5, last.value());
        assertTrue(last.isSet());
    }

    @Test
    void getKeyedValue_objectDerivesUnsetStateFromValue()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(NAME);
        builder.add("A");
        builder.addNull();
        ColumnObject<String> column = builder.build();

        KeyedObject<String> set = column.getKeyedValue(0);
        assertEquals(NAME, set.key());
        assertEquals("A", set.value());
        assertTrue(set.isSet());

        KeyedObject<String> unset = column.getKeyedValue(1);
        assertEquals(NAME, unset.key());
        assertEquals(null, unset.value());
        assertFalse(unset.isSet());
    }

    @Test
    void keyedValues_requireKey()
    {
        assertThrows(NullPointerException.class, () -> new KeyedInt(null, 1, true));
        assertThrows(NullPointerException.class, () -> new KeyedObject<>(null, "A"));
    }
}
