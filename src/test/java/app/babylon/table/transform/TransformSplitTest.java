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
    public void shouldSplitOnFirstDelimiter()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName FIRST = ColumnName.of("FirstName");
        final ColumnName REST = ColumnName.of("Rest");

        ColumnObject.Builder<String> strings = ColumnObject.builder(NAME, ColumnTypes.STRING);
        strings.add("Ada Lovelace Byron");
        strings.add("Plato");
        strings.addNull();
        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformSplit(NAME, " ", TransformSplit.Mode.FIRST, FIRST, REST));

        ColumnObject<String> first = transformed.getString(FIRST);
        ColumnObject<String> rest = transformed.getString(REST);
        assertEquals("Ada", first.get(0));
        assertEquals("Lovelace Byron", rest.get(0));
        assertEquals("Plato", first.get(1));
        assertFalse(rest.isSet(1));
        assertFalse(first.isSet(2));
        assertFalse(rest.isSet(2));
    }

    @Test
    public void shouldSplitOnLastDelimiter()
    {
        final ColumnName PATH = ColumnName.of("Path");
        final ColumnName DIRECTORY = ColumnName.of("Directory");
        final ColumnName FILE = ColumnName.of("File");

        ColumnObject.Builder<String> strings = ColumnObject.builder(PATH, ColumnTypes.STRING);
        strings.add("/tmp/reports/file.csv");
        strings.add("file.csv");
        strings.add("/tmp/reports/");
        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformSplit(PATH, "/", TransformSplit.Mode.LAST, DIRECTORY, FILE));

        ColumnObject<String> directory = transformed.getString(DIRECTORY);
        ColumnObject<String> file = transformed.getString(FILE);
        assertEquals("/tmp/reports", directory.get(0));
        assertEquals("file.csv", file.get(0));
        assertEquals("file.csv", directory.get(1));
        assertFalse(file.isSet(1));
        assertEquals("/tmp/reports", directory.get(2));
        assertEquals("", file.get(2));
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
    public void ofShouldParseModeStringParameters()
    {
        TransformSplit transform = TransformSplit.of(new String[]
        {"Path", "/", "last", "Directory", "File"});

        assertEquals(ColumnName.of("Path"), transform.getColumnToSplit());
        assertEquals("/", transform.getSplitOn());
        assertEquals(TransformSplit.Mode.LAST, transform.getMode());
        assertEquals(ColumnName.of("Directory"), transform.getSplitColumnNames()[0]);
        assertEquals(ColumnName.of("File"), transform.getSplitColumnNames()[1]);
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

    @Test
    public void shouldRejectFirstOrLastModeWithOtherThanTwoColumns()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TransformSplit(ColumnName.of("Path"), "/", TransformSplit.Mode.LAST, ColumnName.of("Only")));

        assertEquals("Split mode last requires exactly two output columns.", exception.getMessage());
    }
}
