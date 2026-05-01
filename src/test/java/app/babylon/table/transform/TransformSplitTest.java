package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class TransformSplitTest
{
    @Test
    public void shouldSplitOneColumnIntoManyColumns()
    {
        final ColumnName SPLIT = ColumnName.of("Split");
        final ColumnName QUANTITY_BEFORE = ColumnName.of("QuantityBefore");
        final ColumnName QUANTITY_AFTER = ColumnName.of("QuantityAfter");

        ColumnObject.Builder<String> strings = ColumnObject.builder(SPLIT, ColumnTypes.STRING);
        strings.add("1/20");
        strings.add(" 3 / 2 ");
        strings.add("1/");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformSplit(SPLIT, "/", QUANTITY_BEFORE, QUANTITY_AFTER));

        ColumnObject<String> quantitiesBefore = transformed.getString(QUANTITY_BEFORE);
        ColumnObject<String> quantitiesAfter = transformed.getString(QUANTITY_AFTER);

        assertEquals("1", quantitiesBefore.get(0));
        assertEquals("20", quantitiesAfter.get(0));

        assertEquals("3", quantitiesBefore.get(1));
        assertEquals("2", quantitiesAfter.get(1));

        assertEquals("1", quantitiesBefore.get(2));
        assertEquals("", quantitiesAfter.get(2));

        assertFalse(quantitiesBefore.isSet(3));
        assertFalse(quantitiesAfter.isSet(3));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Split", "Split", "/", "QuantityBefore", "QuantityAfter");

        assertTrue(transform instanceof TransformSplit);
    }

    @Test
    public void ofShouldParseStringParameters()
    {
        TransformSplit transform = TransformSplit.of(new String[]
        {"Split", "/", "QuantityBefore", "QuantityAfter"});

        assertEquals(ColumnName.of("Split"), transform.getColumnToSplit());
        assertEquals("/", transform.getSplitOn());
        assertEquals(ColumnName.of("QuantityBefore"), transform.getSplitColumnNames()[0]);
        assertEquals(ColumnName.of("QuantityAfter"), transform.getSplitColumnNames()[1]);
    }

    @Test
    public void shouldDefensivelyCopySplitColumnNames()
    {
        ColumnName[] splitColumnNames = new ColumnName[]
        {ColumnName.of("QuantityBefore"), ColumnName.of("QuantityAfter")};

        TransformSplit transform = new TransformSplit(ColumnName.of("Split"), "/", splitColumnNames);
        splitColumnNames[0] = ColumnName.of("Mutated");
        ColumnName[] actual = transform.getSplitColumnNames();
        actual[1] = ColumnName.of("AlsoMutated");

        assertEquals(ColumnName.of("QuantityBefore"), transform.getSplitColumnNames()[0]);
        assertEquals(ColumnName.of("QuantityAfter"), transform.getSplitColumnNames()[1]);
    }

    @Test
    public void shouldRejectMultiCharacterDelimiter()
    {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new TransformSplit(ColumnName.of("AccountKey"), "::", ColumnName.of("AccountType")));

        assertEquals("Split split delimiter must be exactly one character.", exception.getMessage());
    }
}
