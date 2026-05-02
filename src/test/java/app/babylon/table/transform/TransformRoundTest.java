package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformRoundTest
{
    @Test
    void shouldUseBankersRoundingByDefault()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("2.5"));
        amounts.add(new BigDecimal("3.5"));
        amounts.addNull();
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());

        TableColumnar transformed = table.apply(TransformRound.of(amount, 0));

        assertEquals(0, new BigDecimal("2").compareTo(transformed.getDecimal(amount).get(0)));
        assertEquals(0, new BigDecimal("4").compareTo(transformed.getDecimal(amount).get(1)));
        assertFalse(transformed.getDecimal(amount).isSet(2));
    }

    @Test
    void shouldRoundWithConfiguredModeAndTargetColumn()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName rounded = ColumnName.of("RoundedAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("2.345"));
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());

        TableColumnar transformed = table.apply(TransformRound.of(amount, rounded, 2, RoundingMode.HALF_UP));

        assertEquals(0, new BigDecimal("2.35").compareTo(transformed.getDecimal(rounded).get(0)));
    }

    @Test
    void shouldParseFriendlyRoundingModeNames()
    {
        assertSame(RoundingMode.HALF_UP, TransformRound.parseRoundingMode("halfUp"));
        assertSame(RoundingMode.HALF_DOWN, TransformRound.parseRoundingMode("halfDown"));
        assertSame(RoundingMode.HALF_EVEN, TransformRound.parseRoundingMode("bankers"));
        assertSame(RoundingMode.UNNECESSARY, TransformRound.parseRoundingMode("noLoss"));
        assertEquals("halfUp", TransformRound.roundingModeName(RoundingMode.HALF_UP));
        assertEquals("halfDown", TransformRound.roundingModeName(RoundingMode.HALF_DOWN));
        assertEquals("bankers", TransformRound.roundingModeName(RoundingMode.HALF_EVEN));
        assertEquals("noLoss", TransformRound.roundingModeName(RoundingMode.UNNECESSARY));
    }

    @Test
    void shouldRoundUsingCurrencyScaleColumn()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName currency = ColumnName.of("Currency");
        ColumnName rounded = ColumnName.of("RoundedAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("2.555"));
        amounts.add(new BigDecimal("123.45"));
        amounts.add(new BigDecimal("1.2344"));
        amounts.addNull();
        ColumnObject.Builder<Currency> currencies = ColumnObject.builder(currency, ColumnTypes.CURRENCY);
        currencies.add(Currency.getInstance("USD"));
        currencies.add(Currency.getInstance("JPY"));
        currencies.add(Currency.getInstance("KWD"));
        currencies.add(Currency.getInstance("USD"));
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build(), currencies.build());

        TableColumnar transformed = table.apply(TransformRound.using(amount, currency, rounded, null,
                TransformRound.roundScales(Currency.class, Currency::getDefaultFractionDigits)));

        assertEquals(0, new BigDecimal("2.56").compareTo(transformed.getDecimal(rounded).get(0)));
        assertEquals(0, new BigDecimal("123").compareTo(transformed.getDecimal(rounded).get(1)));
        assertEquals(0, new BigDecimal("1.234").compareTo(transformed.getDecimal(rounded).get(2)));
        assertFalse(transformed.getDecimal(rounded).isSet(3));
    }
}
