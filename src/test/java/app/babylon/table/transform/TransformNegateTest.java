package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformNegateTest
{
    @Test
    void shouldNegateDecimalValues()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName signed = ColumnName.of("SignedAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("2.50"));
        amounts.addNull();
        amounts.add(new BigDecimal("-4.25"));
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());

        TableColumnar transformed = table.apply(TransformNegate.builder(amount).withNewColumnName(signed).build());

        assertEquals(0, new BigDecimal("-2.50").compareTo(transformed.getDecimal(signed).get(0)));
        assertFalse(transformed.getDecimal(signed).isSet(1));
        assertEquals(0, new BigDecimal("4.25").compareTo(transformed.getDecimal(signed).get(2)));
    }

    @Test
    void shouldNegateOnlyWhenBooleanColumnIsTrue()
    {
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnName isBuy = ColumnName.of("IsBuy");
        ColumnName signed = ColumnName.of("SignedQuantity");
        ColumnObject.Builder<BigDecimal> quantities = ColumnObject.builderDecimal(quantity);
        quantities.add(new BigDecimal("10"));
        quantities.add(new BigDecimal("20"));
        quantities.add(new BigDecimal("30"));
        quantities.addNull();
        ColumnBoolean.Builder flags = ColumnBoolean.builder(isBuy);
        flags.add(true);
        flags.add(false);
        flags.addNull();
        flags.add(true);
        TableColumnar table = Tables.newTable(TableName.of("t"), quantities.build(), flags.build());

        TableColumnar transformed = table
                .apply(TransformNegate.builder(quantity).when(isBuy).withNewColumnName(signed).build());

        assertEquals(0, new BigDecimal("-10").compareTo(transformed.getDecimal(signed).get(0)));
        assertEquals(0, new BigDecimal("20").compareTo(transformed.getDecimal(signed).get(1)));
        assertEquals(0, new BigDecimal("30").compareTo(transformed.getDecimal(signed).get(2)));
        assertFalse(transformed.getDecimal(signed).isSet(3));
    }

    @Test
    void shouldRejectNonBooleanConditionColumn()
    {
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnName type = ColumnName.of("Type");
        ColumnObject.Builder<BigDecimal> quantities = ColumnObject.builderDecimal(quantity);
        quantities.add(new BigDecimal("10"));
        ColumnObject.Builder<String> types = ColumnObject.builder(type, ColumnTypes.STRING);
        types.add("Buy");
        TableColumnar table = Tables.newTable(TableName.of("t"), quantities.build(), types.build());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> table.apply(TransformNegate.builder(quantity).when(type).build()));

        assertTrue(exception.getMessage().contains("when requires Boolean column"));
    }

    @Test
    void shouldRejectNonDecimalSourceColumn()
    {
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnObject.Builder<String> quantities = ColumnObject.builder(quantity, ColumnTypes.STRING);
        quantities.add("10");
        TableColumnar table = Tables.newTable(TableName.of("t"), quantities.build());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> table.apply(TransformNegate.builder(quantity).build()));

        assertTrue(exception.getMessage().contains("requires Decimal column"));
    }
}
