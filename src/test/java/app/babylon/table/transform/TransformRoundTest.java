package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnBoolean;
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

        TableColumnar transformed = table.apply(TransformRound.builder(amount).withScale(0).build());

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

        TableColumnar transformed = table.apply(TransformRound.builder(amount).withNewColumnName(rounded).withScale(2)
                .withRoundingMode(RoundingMode.HALF_UP).build());

        assertEquals(0, new BigDecimal("2.35").compareTo(transformed.getDecimal(rounded).get(0)));
    }

    @Test
    void shouldRoundWithExplicitScaleOnlyWhenBooleanColumnIsTrue()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName noCents = ColumnName.of("NoCents");
        ColumnName rounded = ColumnName.of("RoundedAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("12.50"));
        amounts.add(new BigDecimal("12.50"));
        amounts.add(new BigDecimal("12.50"));
        amounts.addNull();
        ColumnBoolean.Builder flags = ColumnBoolean.builder(noCents);
        flags.add(true);
        flags.add(false);
        flags.addNull();
        flags.add(true);
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build(), flags.build());

        TableColumnar transformed = table
                .apply(TransformRound.builder(amount).withScale(0).when(noCents).withNewColumnName(rounded).build());

        assertEquals(0, new BigDecimal("12").compareTo(transformed.getDecimal(rounded).get(0)));
        assertEquals(0, new BigDecimal("12.50").compareTo(transformed.getDecimal(rounded).get(1)));
        assertEquals(0, new BigDecimal("12.50").compareTo(transformed.getDecimal(rounded).get(2)));
        assertFalse(transformed.getDecimal(rounded).isSet(3));
    }

    @Test
    void shouldRejectNonBooleanConditionColumn()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName condition = ColumnName.of("Condition");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("12.50"));
        ColumnObject.Builder<String> conditions = ColumnObject.builder(condition, ColumnTypes.STRING);
        conditions.add("true");
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build(), conditions.build());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> table.apply(TransformRound.builder(amount).withScale(0).when(condition).build()));

        assertTrue(exception.getMessage().contains("when requires Boolean column"));
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
    void shouldRoundUsingCurrencyScaleColumnOnlyWhenBooleanColumnIsTrue()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName currency = ColumnName.of("Currency");
        ColumnName noCents = ColumnName.of("NoCents");
        ColumnName rounded = ColumnName.of("RoundedAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("2.555"));
        amounts.add(new BigDecimal("123.45"));
        amounts.add(new BigDecimal("1.2344"));
        ColumnObject.Builder<Currency> currencies = ColumnObject.builder(currency, ColumnTypes.CURRENCY);
        currencies.add(Currency.getInstance("USD"));
        currencies.add(Currency.getInstance("JPY"));
        currencies.add(Currency.getInstance("KWD"));
        ColumnBoolean.Builder flags = ColumnBoolean.builder(noCents);
        flags.add(false);
        flags.add(true);
        flags.add(true);
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build(), currencies.build(), flags.build());

        TableColumnar transformed = table.apply(
                TransformRound.builder(amount).withScaleColumnName(currency).when(noCents).withNewColumnName(rounded)
                        .withRoundScale(Currency.class, Currency::getDefaultFractionDigits).build());

        assertEquals(0, new BigDecimal("2.555").compareTo(transformed.getDecimal(rounded).get(0)));
        assertEquals(0, new BigDecimal("123").compareTo(transformed.getDecimal(rounded).get(1)));
        assertEquals(0, new BigDecimal("1.234").compareTo(transformed.getDecimal(rounded).get(2)));
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

        TableColumnar transformed = table.apply(TransformRound.builder(amount).withScaleColumnName(currency)
                .withNewColumnName(rounded).withRoundScale(Currency.class, Currency::getDefaultFractionDigits).build());

        assertEquals(0, new BigDecimal("2.56").compareTo(transformed.getDecimal(rounded).get(0)));
        assertEquals(0, new BigDecimal("123").compareTo(transformed.getDecimal(rounded).get(1)));
        assertEquals(0, new BigDecimal("1.234").compareTo(transformed.getDecimal(rounded).get(2)));
        assertFalse(transformed.getDecimal(rounded).isSet(3));
    }
}
