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

class TransformToLongTest
{
    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        builder.add("1");
        builder.add("2147483648");
        builder.add("1 2");
        builder.add("abc");
        builder.addNull();
        builder.add("-2");
        builder.add("999999999999999999999999");
        ColumnObject<String> source = builder.build();

        ColumnLong transformed = new TransformToLong(ColumnName.of("amount")).apply((Column) source);

        assertEquals(7, transformed.size());
        assertEquals(1L, transformed.get(0));
        assertEquals(2147483648L, transformed.get(1));
        assertFalse(transformed.isSet(2));
        assertFalse(transformed.isSet(3));
        assertFalse(transformed.isSet(4));
        assertEquals(-2L, transformed.get(5));
        assertFalse(transformed.isSet(6));
    }

    @Test
    void applyShouldConvertCategoricalStringColumnToPrimitiveLongColumn()
    {
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("amount"), String.class);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnLong transformed = new TransformToLong(ColumnName.of("amount")).apply((Column) source);

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
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        amountBuilder.add("1");
        amountBuilder.add("2147483648");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(ColumnName.of("other"), String.class);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnLong.Builder existingLongBuilder = ColumnLong.builder(ColumnName.of("existing"));
        existingLongBuilder.add(3L);
        existingLongBuilder.add(4L);
        ColumnLong existingLong = existingLongBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingLong);

        TableColumnar transformed = table.apply(new TransformToLong(ColumnName.of("amount"), ColumnName.of("parsed")));

        ColumnLong parsed = transformed.getLong(ColumnName.of("parsed"));
        assertEquals(1L, parsed.get(0));
        assertEquals(2147483648L, parsed.get(1));

        assertEquals("x", transformed.getString(ColumnName.of("other")).get(0));
        assertSame(existingLong, transformed.getLong(ColumnName.of("existing")));
    }

    @Test
    void applyShouldCopyExistingLongColumnWhenAppliedDirectly()
    {
        ColumnLong.Builder builder = ColumnLong.builder(ColumnName.of("amount"));
        builder.add(1L);
        builder.addNull();
        builder.add(3L);
        ColumnLong source = builder.build();

        ColumnLong transformed = new TransformToLong(ColumnName.of("amount"), ColumnName.of("renamed"))
                .apply((Column) source);

        assertNotSame(source, transformed);
        assertEquals(ColumnName.of("renamed"), transformed.getName());
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
