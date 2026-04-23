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
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformToLongTest
{
    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("1");
        builder.add("2147483648");
        builder.add("1 2");
        builder.add("abc");
        builder.addNull();
        builder.add("-2");
        builder.add("9223372036854775807");
        builder.add("9223372036854775808");
        builder.add("-9223372036854775808");
        builder.add("-9223372036854775809");
        ColumnObject<String> source = builder.build();

        ColumnLong transformed = new TransformToLong(AMOUNT).apply((Column) source);

        assertEquals(10, transformed.size());
        assertEquals(1L, transformed.get(0));
        assertEquals(2147483648L, transformed.get(1));
        assertFalse(transformed.isSet(2));
        assertFalse(transformed.isSet(3));
        assertFalse(transformed.isSet(4));
        assertEquals(-2L, transformed.get(5));
        assertEquals(9223372036854775807L, transformed.get(6));
        assertFalse(transformed.isSet(7));
        assertEquals(-9223372036854775808L, transformed.get(8));
        assertFalse(transformed.isSet(9));
    }

    @Test
    void applyShouldConvertCategoricalStringColumnToPrimitiveLongColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnLong transformed = new TransformToLong(AMOUNT).apply((Column) source);

        assertEquals(4, transformed.size());
        assertEquals(10L, transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertEquals(10L, transformed.get(2));
        assertFalse(transformed.isSet(3));
        assertTrue(!(transformed instanceof ColumnCategorical<?>));
    }

    @Test
    void applyTableShouldAddParsedLongColumnAndKeepExistingLongColumnsAsIs()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName OTHER = ColumnName.of("OTHER");
        final ColumnName EXISTING = ColumnName.of("EXISTING");
        final ColumnName PARSED = ColumnName.of("PARSED");
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        amountBuilder.add("1");
        amountBuilder.add("2147483648");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(OTHER, ColumnTypes.STRING);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnLong.Builder existingLongBuilder = ColumnLong.builder(EXISTING);
        existingLongBuilder.add(3L);
        existingLongBuilder.add(4L);
        ColumnLong existingLong = existingLongBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingLong);

        TableColumnar transformed = table.apply(new TransformToLong(AMOUNT, PARSED));

        ColumnLong parsed = transformed.getLong(PARSED);
        assertEquals(1L, parsed.get(0));
        assertEquals(2147483648L, parsed.get(1));

        assertEquals("x", transformed.getString(OTHER).get(0));
        assertSame(existingLong, transformed.getLong(EXISTING));
    }

    @Test
    void applyShouldCopyExistingLongColumnWhenAppliedDirectly()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName RENAMED = ColumnName.of("RENAMED");
        ColumnLong.Builder builder = ColumnLong.builder(AMOUNT);
        builder.add(1L);
        builder.addNull();
        builder.add(3L);
        ColumnLong source = builder.build();

        ColumnLong transformed = new TransformToLong(AMOUNT, RENAMED).apply((Column) source);

        assertNotSame(source, transformed);
        assertEquals(RENAMED, transformed.getName());
        assertEquals(1L, transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertEquals(3L, transformed.get(2));
    }

    @Test
    void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("ToLong", "amount", "parsed");

        assertTrue(transform instanceof TransformToLong);
    }
}
