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
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.transform.TransformTakeToType.Operation;
import app.babylon.text.Sentence.ParseMode;

public class TransformTakeToTypeTest
{
    @Test
    public void shouldTakeQuantityAndPriceFromTradeDescription()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName QUANTITY = ColumnName.of("Quantity");
        final ColumnName PRICE = ColumnName.of("Price");

        ColumnObject.Builder<String> strings = ColumnObject.builder(DESCRIPTION, ColumnTypes.STRING);
        strings.add("buy 100 AAPL @ 123");
        strings.add("sell 25 MSFT @ USD 456.78");
        strings.add("No price");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(TransformTakeToType.delimited(Operation.BEFORE, DESCRIPTION, QUANTITY, ColumnTypes.DECIMAL,
                        ParseMode.LAST_IN, "@"))
                .apply(TransformTakeToType.delimited(Operation.AFTER, DESCRIPTION, PRICE, ColumnTypes.DECIMAL,
                        ParseMode.FIRST_IN, "@"));

        ColumnObject<BigDecimal> quantities = transformed.getDecimal(QUANTITY);
        assertEquals(0, new BigDecimal("100").compareTo(quantities.get(0)));
        assertEquals(0, new BigDecimal("25").compareTo(quantities.get(1)));
        assertFalse(quantities.isSet(2));
        assertFalse(quantities.isSet(3));

        ColumnObject<BigDecimal> prices = transformed.getDecimal(PRICE);
        assertEquals(0, new BigDecimal("123").compareTo(prices.get(0)));
        assertEquals(0, new BigDecimal("456.78").compareTo(prices.get(1)));
        assertFalse(prices.isSet(2));
        assertFalse(prices.isSet(3));
    }
}
