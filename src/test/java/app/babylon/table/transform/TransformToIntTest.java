package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformToIntTest
{
    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("1");
        builder.add("1 2");
        builder.add("abc");
        builder.addNull();
        builder.add("-2");
        builder.add("999999999999");
        ColumnObject<String> source = builder.build();

        ColumnInt transformed = new TransformToInt(AMOUNT).apply((Column) source);

        assertEquals(6, transformed.size());
        assertEquals(1, transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertFalse(transformed.isSet(2));
        assertFalse(transformed.isSet(3));
        assertEquals(-2, transformed.get(4));
        assertFalse(transformed.isSet(5));
    }

    @Test
    void applyShouldConvertCategoricalStringColumnToPrimitiveIntColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnInt transformed = new TransformToInt(AMOUNT).apply((Column) source);

        assertEquals(4, transformed.size());
        assertEquals(10, transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertEquals(10, transformed.get(2));
        assertFalse(transformed.isSet(3));
        assertTrue(!(transformed instanceof ColumnCategorical<?>));
    }

    @Test
    void applyTableShouldAddParsedIntColumnAndKeepExistingIntColumnsAsIs()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName OTHER = ColumnName.of("OTHER");
        final ColumnName EXISTING = ColumnName.of("EXISTING");
        final ColumnName PARSED = ColumnName.of("PARSED");
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        amountBuilder.add("1");
        amountBuilder.add("2");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(OTHER,
                app.babylon.table.column.ColumnTypes.STRING);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnInt.Builder existingIntBuilder = ColumnInt.builder(EXISTING);
        existingIntBuilder.add(3);
        existingIntBuilder.add(4);
        ColumnInt existingInt = existingIntBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingInt);

        TableColumnar transformed = table.apply(new TransformToInt(AMOUNT, PARSED));

        ColumnInt parsed = transformed.getInt(PARSED);
        assertEquals(1, parsed.get(0));
        assertEquals(2, parsed.get(1));

        assertEquals("x", transformed.getString(OTHER).get(0));
        assertSame(existingInt, transformed.getInt(EXISTING));
    }

    @Test
    void applyShouldCopyExistingIntColumnWhenAppliedDirectly()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName RENAMED = ColumnName.of("RENAMED");
        ColumnInt.Builder builder = ColumnInt.builder(AMOUNT);
        builder.add(1);
        builder.addNull();
        builder.add(3);
        ColumnInt source = builder.build();

        ColumnInt transformed = new TransformToInt(AMOUNT, RENAMED).apply((Column) source);

        assertNotSame(source, transformed);
        assertEquals(RENAMED, transformed.getName());
        assertEquals(1, transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertEquals(3, transformed.get(2));
    }

    @Test
    void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("ToInt", "amount", "parsed");

        assertTrue(transform instanceof TransformToInt);
    }
}
