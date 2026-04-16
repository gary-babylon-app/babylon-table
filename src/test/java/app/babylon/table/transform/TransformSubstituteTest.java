package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformSubstituteTest
{
    @Test
    void shouldSubstituteValuesAndUseDefaultForUnknownOrEmptyRows()
    {
        final ColumnName STATUS = ColumnName.of("Status");
        final ColumnName NORMALISED = ColumnName.of("Normalised");

        ColumnObject.Builder<String> status = ColumnObject.builder(STATUS, app.babylon.table.column.ColumnTypes.STRING);
        status.add("A");
        status.add("B");
        status.add("C");
        status.add("");
        status.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), status.build());

        TableColumnar transformed = table
                .apply(new TransformSubstitute("Other", STATUS, NORMALISED, "A", "Alpha", "B", "Beta"));

        ColumnObject<String> normalised = transformed.getString(NORMALISED);
        assertEquals("Alpha", normalised.get(0));
        assertEquals("Beta", normalised.get(1));
        assertEquals("Other", normalised.get(2));
        assertEquals("Other", normalised.get(3));
        assertEquals("Other", normalised.get(4));
    }

    @Test
    void shouldLeaveMissingStringColumnUnchanged()
    {
        final ColumnName VALUE = ColumnName.of("Value");
        ColumnObject.Builder<String> value = ColumnObject.builder(VALUE, app.babylon.table.column.ColumnTypes.STRING);
        value.add("x");

        TableColumnar table = Tables.newTable(TableName.of("t"), value.build());

        TableColumnar transformed = table.apply(
                new TransformSubstitute(ColumnName.of("Missing"), ColumnName.of("New"), java.util.Map.of("x", "y")));

        assertFalse(transformed.contains(ColumnName.of("New")));
    }

    @Test
    void shouldCreateFromFactoryParameters()
    {
        TransformSubstitute transform = TransformSubstitute.of(new String[]
        {"Status", "Normalised", "Other", "A", "Alpha", "B", "Beta"});

        assertNotNull(transform);
        assertNull(TransformSubstitute.of(new String[]
        {"Status", "Normalised", "Other"}));
    }
}
