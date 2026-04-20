package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformToDecimalAbsTest
{
    private static void assertDecimalEquals(String expected, BigDecimal actual)
    {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void applyShouldConvertToAbsoluteDecimalValues()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("-1.25");
        builder.add("$-2,000.00");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformed = TransformToDecimalAbs.of(AMOUNT).apply((Column) source);

        assertDecimalEquals("1.25", transformed.get(0));
        assertDecimalEquals("2000.00", transformed.get(1));
        assertFalse(transformed.isSet(2));
    }

    @Test
    void constructorsAndFactoriesShouldCreateWorkingAbsoluteDecimalTransforms()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName ABSOLUTE = ColumnName.of("Absolute");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("-12.5");
        builder.add("$1,234.50");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> absoluteWithCustomName = TransformToDecimalAbs.of("Amount", "Absolute")
                .apply((Column) source);
        ColumnObject<BigDecimal> absoluteFromColumns = TransformToDecimalAbs.of(AMOUNT).apply((Column) source);
        TransformToDecimalAbs fromStringFactory = TransformToDecimalAbs.of("ToDecimalAbs(Amount)");
        TransformToDecimalAbs fromParamsFactory = TransformToDecimalAbs.of("Amount", "Absolute");

        assertEquals(ABSOLUTE, absoluteWithCustomName.getName());
        assertDecimalEquals("12.5", absoluteWithCustomName.get(0));
        assertDecimalEquals("1234.50", absoluteWithCustomName.get(1));

        assertDecimalEquals("12.5", absoluteFromColumns.get(0));
        assertDecimalEquals("1234.50", absoluteFromColumns.get(1));

        assertEquals("ToDecimalAbs(Amount)", fromStringFactory.toString());
        assertDecimalEquals("12.5", fromStringFactory.apply((Column) source).get(0));

        assertTrue(fromParamsFactory instanceof TransformToDecimalAbs);
        ColumnObject<BigDecimal> absoluteFromParams = fromParamsFactory.apply((Column) source);
        assertEquals(ABSOLUTE, absoluteFromParams.getName());
        assertDecimalEquals("12.5", absoluteFromParams.get(0));
        assertDecimalEquals("1234.50", absoluteFromParams.get(1));

        assertNull(TransformToDecimalAbs.of(new String[]
        {"Amount"}));
        assertNull(TransformToDecimalAbs.of((ColumnName) null));
    }
}
