package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.Column;
import app.babylon.table.ColumnCategorical;
import app.babylon.table.ColumnInt;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.Transform;

class TransformToIntTest
{
    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        builder.add("1");
        builder.add("1 2");
        builder.add("abc");
        builder.addNull();
        builder.add("-2");
        builder.add("999999999999");
        ColumnObject<String> source = builder.build();

        ColumnInt transformed = new TransformToInt(ColumnName.of("amount")).apply((Column) source);

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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("amount"), String.class);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnInt transformed = new TransformToInt(ColumnName.of("amount")).apply((Column) source);

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
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        amountBuilder.add("1");
        amountBuilder.add("2");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(ColumnName.of("other"), String.class);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnInt.Builder existingIntBuilder = ColumnInt.builder(ColumnName.of("existing"));
        existingIntBuilder.add(3);
        existingIntBuilder.add(4);
        ColumnInt existingInt = existingIntBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""),
                amountBuilder.build(),
                otherBuilder.build(),
                existingInt);

        TableColumnar transformed = table.apply(new TransformToInt(ColumnName.of("amount"), ColumnName.of("parsed")));

        ColumnInt parsed = transformed.getInt(ColumnName.of("parsed"));
        assertEquals(1, parsed.get(0));
        assertEquals(2, parsed.get(1));

        assertEquals("x", transformed.getString(ColumnName.of("other")).get(0));
        assertSame(existingInt, transformed.getInt(ColumnName.of("existing")));
    }

    @Test
    void applyShouldCopyExistingIntColumnWhenAppliedDirectly()
    {
        ColumnInt.Builder builder = ColumnInt.builder(ColumnName.of("amount"));
        builder.add(1);
        builder.addNull();
        builder.add(3);
        ColumnInt source = builder.build();

        ColumnInt transformed = new TransformToInt(ColumnName.of("amount"), ColumnName.of("renamed")).apply((Column) source);

        assertNotSame(source, transformed);
        assertEquals(ColumnName.of("renamed"), transformed.getName());
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
