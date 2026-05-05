package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Currency;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

class TransformExtractFromColumnNameTest
{
    @Test
    void shouldExtractCurrencyFromColumnName()
    {
        final ColumnName AMOUNT_USD = ColumnName.of("AmountUSD");
        final ColumnName CURRENCY = ColumnName.of("Currency");

        ColumnInt.Builder amounts = ColumnInt.builder(AMOUNT_USD);
        amounts.add(10);
        amounts.add(20);

        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());
        TableColumnar transformed = table.apply(TransformExtractFromColumnName.of(AMOUNT_USD,
                Pattern.compile("([A-Z]{3})$"), ColumnTypes.CURRENCY, CURRENCY));

        ColumnCategorical<Currency> currency = transformed.getCategorical(CURRENCY);
        assertEquals(Currency.getInstance("USD"), currency.get(0));
        assertEquals(Currency.getInstance("USD"), currency.get(1));
    }

    @Test
    void shouldCreateNullConstantWhenColumnNameDoesNotMatch()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName CURRENCY = ColumnName.of("Currency");

        ColumnInt.Builder amounts = ColumnInt.builder(AMOUNT);
        amounts.add(10);

        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());
        TableColumnar transformed = table.apply(TransformExtractFromColumnName.of(AMOUNT,
                Pattern.compile("([A-Z]{3})$"), ColumnTypes.CURRENCY, CURRENCY));

        assertFalse(transformed.getCategorical(CURRENCY).isSet(0));
    }
}
