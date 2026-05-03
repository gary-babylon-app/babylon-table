package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
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
        builder.add("10");
        builder.add("bad");
        builder.addNull();
        builder.add("-20");

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.INT, AMOUNT).build();
        Column transformed = transform.apply(builder.build());

        ColumnInt ints = (ColumnInt) transformed;
        assertEquals(ColumnTypes.INT, ints.getType());
        assertEquals(10, ints.get(0));
        assertFalse(ints.isSet(1));
        assertFalse(ints.isSet(2));
        assertEquals(-20, ints.get(3));
    }

    @Test
    void shouldUseExistingDoubleExtractionBehavior()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("USD 12.50");
        builder.add("abc");

        TransformToPrimitive transform = TransformToPrimitive.builder(ColumnTypes.DOUBLE, AMOUNT)
                .withParseMode(TransformParseMode.ONLY_IN).build();
        ColumnDouble doubles = (ColumnDouble) transform.apply(builder.build());

        assertSame(TransformParseMode.ONLY_IN, transform.getParseMode());
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
