package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.ColumnObject.Mode;

class TransformToPrimitiveTest
{
    @Test
    void shouldConvertStringColumnToPrimitiveIntColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("1");
        builder.add("1 2");
        builder.add("bad");
        builder.addNull();
        builder.add("-2");
        builder.add("2147483647");
        builder.add("2147483648");
        builder.add("-2147483648");
        builder.add("-2147483649");

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.INT, AMOUNT).build();
        Column transformed = transform.apply(builder.build());

        ColumnInt ints = (ColumnInt) transformed;
        assertEquals(ColumnTypes.INT, ints.getType());
        assertEquals(1, ints.get(0));
        assertFalse(ints.isSet(1));
        assertFalse(ints.isSet(2));
        assertFalse(ints.isSet(3));
        assertEquals(-2, ints.get(4));
        assertEquals(2147483647, ints.get(5));
        assertFalse(ints.isSet(6));
        assertEquals(-2147483648, ints.get(7));
        assertFalse(ints.isSet(8));
    }

    @Test
    void shouldConvertCategoricalStringColumnToPrimitiveIntColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.addNull();

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.INT, AMOUNT).build();
        ColumnInt ints = (ColumnInt) transform.apply(builder.build());

        assertEquals(4, ints.size());
        assertEquals(10, ints.get(0));
        assertFalse(ints.isSet(1));
        assertEquals(10, ints.get(2));
        assertFalse(ints.isSet(3));
        assertTrue(!(ints instanceof ColumnCategorical<?>));
    }

    @Test
    void shouldConvertStringColumnToPrimitiveLongColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
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

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.LONG, AMOUNT).build();
        ColumnLong longs = (ColumnLong) transform.apply(builder.build());

        assertEquals(10, longs.size());
        assertEquals(1L, longs.get(0));
        assertEquals(2147483648L, longs.get(1));
        assertFalse(longs.isSet(2));
        assertFalse(longs.isSet(3));
        assertFalse(longs.isSet(4));
        assertEquals(-2L, longs.get(5));
        assertEquals(9223372036854775807L, longs.get(6));
        assertFalse(longs.isSet(7));
        assertEquals(-9223372036854775808L, longs.get(8));
        assertFalse(longs.isSet(9));
    }

    @Test
    void shouldConvertStringColumnToPrimitiveDoubleColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("1.25");
        builder.add("USD 12.50");
        builder.add("1 2 3");
        builder.add("abc");
        builder.addNull();
        builder.add("(1,234.50)");
        builder.add("1e6");
        builder.add("1E-6");

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.DOUBLE, AMOUNT).build();
        ColumnDouble doubles = (ColumnDouble) transform.apply(builder.build());

        assertEquals(8, doubles.size());
        assertEquals(1.25d, doubles.get(0), 1.0e-12);
        assertFalse(doubles.isSet(1));
        assertFalse(doubles.isSet(2));
        assertFalse(doubles.isSet(3));
        assertFalse(doubles.isSet(4));
        assertFalse(doubles.isSet(5));
        assertEquals(1.0e6d, doubles.get(6), 1.0e-12);
        assertEquals(1.0e-6d, doubles.get(7), 1.0e-18);
    }

    @Test
    void shouldParseDoubleTextUsingPrimitiveBuilder()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("12.50");
        builder.add("abc");

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.DOUBLE, AMOUNT).build();
        ColumnDouble doubles = (ColumnDouble) transform.apply(builder.build());

        assertEquals(12.5d, doubles.get(0), 1.0e-12);
        assertFalse(doubles.isSet(1));
    }

    @Test
    void shouldCopyExistingPrimitiveColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName PARSED = ColumnName.of("Parsed");
        ColumnInt.Builder builder = ColumnInt.builder(AMOUNT);
        builder.add(1);
        builder.addNull();
        builder.add(3);

        ColumnInt source = builder.build();
        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.INT, AMOUNT).withNewColumnName(PARSED)
                .build();
        ColumnInt transformed = (ColumnInt) transform.apply(source);

        assertNotSame(source, transformed);
        assertEquals(PARSED, transformed.getName());
        assertEquals(1, transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertEquals(3, transformed.get(2));
    }

    @Test
    void shouldKeepExistingPrimitiveColumnWhenNoNewColumnName()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnInt.Builder builder = ColumnInt.builder(AMOUNT);
        builder.add(1);

        ColumnInt source = builder.build();
        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.INT, AMOUNT).build();

        assertSame(source, transform.apply(source));
    }

    @Test
    void shouldAddParsedPrimitiveColumnToTable()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName OTHER = ColumnName.of("Other");
        final ColumnName EXISTING = ColumnName.of("Existing");
        final ColumnName PARSED = ColumnName.of("Parsed");
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        amountBuilder.add("1");
        amountBuilder.add("2");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(OTHER, ColumnTypes.STRING);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnInt.Builder existingIntBuilder = ColumnInt.builder(EXISTING);
        existingIntBuilder.add(3);
        existingIntBuilder.add(4);
        ColumnInt existingInt = existingIntBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingInt);
        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.INT, AMOUNT).withNewColumnName(PARSED)
                .build();

        TableColumnar transformed = table.apply(transform);

        ColumnInt parsed = transformed.getInt(PARSED);
        assertEquals(1, parsed.get(0));
        assertEquals(2, parsed.get(1));
        assertEquals("x", transformed.getString(OTHER).get(0));
        assertSame(existingInt, transformed.getInt(EXISTING));
    }

    @Test
    void shouldReturnNullForNonStringNonPrimitiveMismatch()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING, Mode.ARRAY);
        builder.add("10");
        Column source = builder.buildAs(ColumnTypes.DECIMAL);

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.INT, AMOUNT).build();
        assertNull(transform.apply(source));
    }

    @Test
    void shouldRejectNonPrimitiveTargetType()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TransformToPrimitive.builder(ColumnTypes.STRING, AMOUNT).build());
        assertTrue(exception.getMessage().contains("primitive target type"));
    }
}
