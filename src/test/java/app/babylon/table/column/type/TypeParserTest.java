package app.babylon.table.column.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class TypeParserTest
{
    private enum Side
    {
        BUY, SELL;

        private static Side parse(CharSequence s)
        {
            if (s == null)
            {
                return null;
            }
            String text = s.toString().strip().toUpperCase();
            return switch (text)
            {
                case "B", "BUY" -> BUY;
                case "S", "SELL" -> SELL;
                default -> null;
            };
        }
    }

    @Test
    void defaultPrimitiveParsersShouldWorkForCharSequence()
    {
        TypeParser<String> parser = s -> s == null ? null : s.toString().trim();

        assertEquals("Alpha", parser.parse(" Alpha "));
        assertEquals((byte) 12, parser.parseByte("12"));
        assertEquals(123, parser.parseInt("123"));
        assertEquals(1234567890123L, parser.parseLong("1234567890123"));
        assertEquals(12.5d, parser.parseDouble("12.5"));
    }

    @Test
    void defaultPrimitiveParsersShouldWorkForCharSlice()
    {
        TypeParser<String> parser = CharSequence::toString;
        String chars = "xx-12|345|6789012345|9.75|Beta-yy";

        assertEquals((byte) -12, parser.parseByte(chars, 2, 3));
        assertEquals(345, parser.parseInt(chars, 6, 3));
        assertEquals(6789012345L, parser.parseLong(chars, 10, 10));
        assertEquals(9.75d, parser.parseDouble(chars, 21, 4));
        assertEquals("Beta", parser.parse(chars, 26, 4));
    }

    @Test
    void objectParserShouldWorkWithBigDecimals()
    {
        TypeParser<BigDecimal> parser = TypeParsers.BIG_DECIMAL;
        String chars = "xx1234.50yy";

        assertEquals(0, new BigDecimal("1234.50").compareTo(parser.parse("1234.50")));
        assertEquals(0, new BigDecimal("1234.50").compareTo(parser.parse(chars, 2, 7)));
        assertNull(parser.parse("not-a-decimal"));
        assertNull(parser.parse(chars, 0, 2));
    }

    @Test
    void objectParserShouldWorkWithLocalDateYmdFallback()
    {
        TypeParser<LocalDate> parser = TypeParsers.LOCAL_DATE_YMD;
        String chars = "xx2024-03-15yy";

        assertEquals(LocalDate.of(2024, 3, 15), parser.parse("2024-03-15"));
        assertEquals(LocalDate.of(2024, 3, 15), parser.parse(chars, 2, 10));
        assertNull(parser.parse("not-a-date"));
        assertNull(parser.parse(chars, 0, 2));
    }

    @Test
    void objectParserShouldWorkWithLocalDateMdyAndDmy()
    {
        TypeParser<LocalDate> usParser = TypeParsers.localDate(app.babylon.table.transform.DateFormat.MDY);
        TypeParser<LocalDate> ukParser = TypeParsers.localDate(app.babylon.table.transform.DateFormat.DMY);
        String usChars = "xx03/15/2024yy";
        String ukChars = "xx15/03/2024yy";

        assertEquals(LocalDate.of(2024, 3, 15), usParser.parse("03/15/2024"));
        assertEquals(LocalDate.of(2024, 3, 15), usParser.parse(usChars, 2, 10));
        assertEquals(LocalDate.of(2024, 3, 15), ukParser.parse("15/03/2024"));
        assertEquals(LocalDate.of(2024, 3, 15), ukParser.parse(ukChars, 2, 10));
        assertNull(usParser.parse("15/03/2024"));
        assertNull(ukParser.parse("03/15/2024"));
    }

    @Test
    void enumParserShouldWrapSuppliedFunction()
    {
        TypeParser<Side> parser = TypeParsers.enumParser(Side::parse);
        String chars = "xxBUYyy";

        assertEquals(Side.BUY, parser.parse("B"));
        assertEquals(Side.SELL, parser.parse("sell"));
        assertEquals(Side.BUY, parser.parse(chars, 2, 3));
        assertNull(parser.parse((CharSequence) null));
        assertNull(parser.parse(""));
        assertNull(parser.parse("hold"));
    }

    @Test
    void enumParserShouldProvideDefaultEnumValueOfFallbacks()
    {
        TypeParser<Side> parser = TypeParsers.enumParser(Side.class);
        String chars = "xxBUYyy";

        assertEquals(Side.BUY, parser.parse("BUY"));
        assertEquals(Side.SELL, parser.parse(" sell "));
        assertEquals(Side.BUY, parser.parse(chars, 2, 3));
        assertNull(parser.parse((CharSequence) null));
        assertNull(parser.parse(""));
        assertNull(parser.parse("B"));
        assertNull(parser.parse("hold"));
    }

    @Test
    void currencyParserShouldUseFastPathForCommonCurrencies()
    {
        TypeParser<Currency> parser = TypeParsers.CURRENCY;
        String chars = "xx usd yy";

        assertSame(Currency.getInstance("USD"), parser.parse("USD"));
        assertSame(Currency.getInstance("EUR"), parser.parse(" eur "));
        assertSame(Currency.getInstance("USD"), parser.parse(chars, 2, 5));
        assertSame(Currency.getInstance("ZAR"), parser.parse("zar"));
        assertSame(Currency.getInstance("SEK"), parser.parse("SEK"));
        assertSame(Currency.getInstance("NOK"), parser.parse("nok"));
        assertSame(Currency.getInstance("DKK"), parser.parse("DKK"));
        assertSame(Currency.getInstance("SGD"), parser.parse("SGD"));
        assertSame(Currency.getInstance("HKD"), parser.parse("hkd"));
    }

    @Test
    void currencyParserShouldFallbackForOtherSupportedCurrenciesAndReturnNullForInvalid()
    {
        TypeParser<Currency> parser = TypeParsers.CURRENCY;

        assertSame(Currency.getInstance("MXN"), parser.parse(" mxN "));
        assertNull(parser.parse("CNH"));
        assertNull(parser.parse("not-a-currency"));
        assertNull(parser.parse(""));
        assertNull(parser.parse((CharSequence) null));
    }
}
