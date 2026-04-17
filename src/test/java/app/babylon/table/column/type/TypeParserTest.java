package app.babylon.table.column.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        char[] chars = "xx-12|345|6789012345|9.75|Beta-yy".toCharArray();

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
        char[] chars = "xx1234.50yy".toCharArray();

        assertEquals(0, new BigDecimal("1234.50").compareTo(parser.parse("1234.50")));
        assertEquals(0, new BigDecimal("1234.50").compareTo(parser.parse(chars, 2, 7)));
        assertNull(parser.parse("not-a-decimal"));
        assertNull(parser.parse(chars, 0, 2));
    }

    @Test
    void objectParserShouldWorkWithLocalDateYmdFallback()
    {
        TypeParser<LocalDate> parser = TypeParsers.LOCAL_DATE_YMD;
        char[] chars = "xx2024-03-15yy".toCharArray();

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
        char[] usChars = "xx03/15/2024yy".toCharArray();
        char[] ukChars = "xx15/03/2024yy".toCharArray();

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
        char[] chars = "xxBUYyy".toCharArray();

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
        char[] chars = "xxBUYyy".toCharArray();

        assertEquals(Side.BUY, parser.parse("BUY"));
        assertEquals(Side.SELL, parser.parse(" sell "));
        assertEquals(Side.BUY, parser.parse(chars, 2, 3));
        assertNull(parser.parse((CharSequence) null));
        assertNull(parser.parse(""));
        assertNull(parser.parse("B"));
        assertNull(parser.parse("hold"));
    }
}
