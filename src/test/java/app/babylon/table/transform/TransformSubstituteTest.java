package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformSubstituteTest
{
    @Test
    void shouldSubstituteValuesAndUseDefaultForUnknownOrEmptySetRows()
    {
        final ColumnName STATUS = ColumnName.of("Status");
        final ColumnName NORMALISED = ColumnName.of("Normalised");

        ColumnObject.Builder<String> status = ColumnObject.builder(STATUS, ColumnTypes.STRING);
        status.add("A");
        status.add("B");
        status.add("C");
        status.add("");
        status.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), status.build());

        TableColumnar transformed = table.apply(TransformSubstitute.builder(STATUS).withNewColumnName(NORMALISED)
                .withDefaultValue("Other").withReplacement("A", "Alpha").withReplacement("B", "Beta").build());

        ColumnObject<String> normalised = transformed.getString(NORMALISED);
        assertEquals("Alpha", normalised.get(0));
        assertEquals("Beta", normalised.get(1));
        assertEquals("Other", normalised.get(2));
        assertEquals("Other", normalised.get(3));
        assertFalse(normalised.isSet(4));
    }

    @Test
    void shouldPreserveCategoricalEncodingWhenSubstituting()
    {
        final ColumnName STATUS = ColumnName.of("Status");
        final ColumnName NORMALISED = ColumnName.of("Normalised");

        ColumnCategorical.Builder<String> status = ColumnCategorical.builder(STATUS, ColumnTypes.STRING);
        status.add("A");
        status.add("A");
        status.add("B");
        status.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), status.build());

        TableColumnar transformed = table.apply(TransformSubstitute.builder(STATUS).withNewColumnName(NORMALISED)
                .withReplacement("A", "Active").withDefaultValue("Other").build());

        ColumnCategorical<String> normalised = transformed.getCategorical(NORMALISED);
        assertEquals("Active", normalised.get(0));
        assertEquals("Active", normalised.get(1));
        assertEquals("Other", normalised.get(2));
        assertFalse(normalised.isSet(3));
        assertEquals(normalised.getCategoryCode(0), normalised.getCategoryCode(1));
    }

    @Test
    void shouldLeaveMissingStringColumnUnchanged()
    {
        final ColumnName VALUE = ColumnName.of("Value");
        final ColumnName MISSING = ColumnName.of("Missing");
        final ColumnName NEW = ColumnName.of("New");
        ColumnObject.Builder<String> value = ColumnObject.builder(VALUE, ColumnTypes.STRING);
        value.add("x");

        TableColumnar table = Tables.newTable(TableName.of("t"), value.build());

        TableColumnar transformed = table.apply(
                TransformSubstitute.builder(MISSING).withNewColumnName(NEW).withReplacements(Map.of("x", "y")).build());

        assertFalse(transformed.contains(NEW));
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
