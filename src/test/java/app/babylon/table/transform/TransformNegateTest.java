package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
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

        TableColumnar transformed = table.apply(TransformNegate.of(amount, signed));

        assertEquals(0, new BigDecimal("-2.50").compareTo(transformed.getDecimal(signed).get(0)));
        assertFalse(transformed.getDecimal(signed).isSet(1));
        assertEquals(0, new BigDecimal("4.25").compareTo(transformed.getDecimal(signed).get(2)));
    }

    @Test
    void shouldNegateOnlyWhenConditionMatches()
    {
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnName type = ColumnName.of("Type");
        ColumnName signed = ColumnName.of("SignedQuantity");
        ColumnObject.Builder<BigDecimal> quantities = ColumnObject.builderDecimal(quantity);
        quantities.add(new BigDecimal("10"));
        quantities.add(new BigDecimal("20"));
        quantities.add(new BigDecimal("30"));
        quantities.addNull();
        ColumnObject.Builder<String> types = ColumnObject.builder(type, ColumnTypes.STRING);
        types.add("Buy");
        types.add("Sell");
        types.addNull();
        types.add("Buy");
        TableColumnar table = Tables.newTable(TableName.of("t"), quantities.build(), types.build());

        TableColumnar transformed = table.apply(TransformNegate.when(quantity, signed, type, "Buy"));

        assertEquals(0, new BigDecimal("-10").compareTo(transformed.getDecimal(signed).get(0)));
        assertEquals(0, new BigDecimal("20").compareTo(transformed.getDecimal(signed).get(1)));
        assertEquals(0, new BigDecimal("30").compareTo(transformed.getDecimal(signed).get(2)));
        assertFalse(transformed.getDecimal(signed).isSet(3));
    }

    @Test
    void shouldParseConditionValueUsingConditionColumnType()
    {
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnName type = ColumnName.of("Type");
        ColumnName signed = ColumnName.of("SignedQuantity");
        ColumnObject.Builder<BigDecimal> quantities = ColumnObject.builderDecimal(quantity);
        quantities.add(new BigDecimal("10"));
        quantities.add(new BigDecimal("20"));
        Column.Type side = Column.Type.of(Side.class,
                (s, offset, length) -> Side.valueOf(s.subSequence(offset, offset + length).toString().toUpperCase()));
        ColumnObject.Builder<Side> types = ColumnObject.builder(type, side);
        types.add(Side.BUY);
        types.add(Side.SELL);
        TableColumnar table = Tables.newTable(TableName.of("t"), quantities.build(), types.build());

        TableColumnar transformed = table.apply(TransformNegate.when(quantity, signed, type, "Buy"));

        assertEquals(0, new BigDecimal("-10").compareTo(transformed.getDecimal(signed).get(0)));
        assertEquals(0, new BigDecimal("20").compareTo(transformed.getDecimal(signed).get(1)));
    }

    private enum Side
    {
        BUY, SELL
    }
}
