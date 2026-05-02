package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformDecimalBinaryOperatorTest
{
    @Test
    void shouldApplyDecimalAdditionAndLeaveNullsUnset()
    {
        final ColumnName LEFT = ColumnName.of("Left");
        final ColumnName RIGHT = ColumnName.of("Right");
        final ColumnName TOTAL = ColumnName.of("Total");

        ColumnObject.Builder<BigDecimal> left = ColumnObject.builderDecimal(LEFT);
        left.add(new BigDecimal("1.25"));
        left.addNull();
        left.add(new BigDecimal("2.00"));

        ColumnObject.Builder<BigDecimal> right = ColumnObject.builderDecimal(RIGHT);
        right.add(new BigDecimal("3.50"));
        right.add(new BigDecimal("4.00"));
        right.add(new BigDecimal("-0.25"));

        TableColumnar table = Tables.newTable(TableName.of("t"), left.build(), right.build());

        TableColumnar transformed = table.apply(
                new TransformDecimalBinaryOperator(LEFT, TransformDecimalBinaryOperator.OPERATOR.Add, RIGHT, TOTAL));

        ColumnObject<BigDecimal> total = transformed.getDecimal(TOTAL);
        assertEquals(0, new BigDecimal("4.75").compareTo(total.get(0)));
        assertFalse(total.isSet(1));
        assertEquals(0, new BigDecimal("1.75").compareTo(total.get(2)));
    }

    @Test
    void shouldApplyDecimalOperatorWithLiteralOperand()
    {
        final ColumnName RATE = ColumnName.of("Rate");
        final ColumnName DECIMAL_RATE = ColumnName.of("DecimalRate");

        ColumnObject.Builder<BigDecimal> rate = ColumnObject.builderDecimal(RATE);
        rate.add(new BigDecimal("63"));
        rate.addNull();
        rate.add(new BigDecimal("12.5"));

        TableColumnar table = Tables.newTable(TableName.of("t"), rate.build());

        TableColumnar transformed = table
                .apply(TransformMultiply.of(TransformDecimalBinaryOperator.Operand.column(RATE),
                        TransformDecimalBinaryOperator.Operand.value(new BigDecimal("0.01")), DECIMAL_RATE));

        ColumnObject<BigDecimal> decimalRate = transformed.getDecimal(DECIMAL_RATE);
        assertEquals(0, new BigDecimal("0.63").compareTo(decimalRate.get(0)));
        assertFalse(decimalRate.isSet(1));
        assertEquals(0, new BigDecimal("0.125").compareTo(decimalRate.get(2)));
    }

    @Test
    void shouldStripTrailingZerosFromLiteralOperands()
    {
        TransformDecimalBinaryOperator.Operand operand = TransformDecimalBinaryOperator.Operand
                .value(new BigDecimal("0.0100"));

        assertEquals("0.01", operand.value().toPlainString());

        operand = TransformDecimalBinaryOperator.Operand.value(new BigDecimal("1000.000"));

        assertEquals("1000", operand.value().toPlainString());
        assertEquals(0, operand.value().scale());
    }

    @Test
    void shouldParseFromString()
    {
        TransformDecimalBinaryOperator transform = TransformDecimalBinaryOperator.of("Add(Left, Right, Total)");

        assertEquals("Add(Left, Right, Total)", transform.toString());
    }
}
