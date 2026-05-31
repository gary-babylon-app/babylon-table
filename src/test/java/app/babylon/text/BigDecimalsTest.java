package app.babylon.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class BigDecimalsTest
{
    private static void assertDecimalEquals(String expected, BigDecimal actual)
    {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void prepareShouldNormalizePercentAndBracketNegativeValues()
    {
        BigDecimals.PreparedDecimal prepared = BigDecimals.prepare("$1,234.50%");

        assertNotNull(prepared);
        assertEquals("1234.50", prepared.normalizedNumberText());
        assertTrue(prepared.isPercent());
        assertFalse(prepared.isNegativeBracket());

        BigDecimals.PreparedDecimal negativeBracket = BigDecimals.prepare("(1,234.50)");
        assertNotNull(negativeBracket);
        assertEquals("1234.50", negativeBracket.normalizedNumberText());
        assertFalse(negativeBracket.isPercent());
        assertTrue(negativeBracket.isNegativeBracket());
    }

    @Test
    void prepareShouldRejectInvalidShapes()
    {
        assertNull(BigDecimals.prepare("1  23"));
        assertNull(BigDecimals.prepare(null));
        assertNull(BigDecimals.prepare("abc"));
    }

    @Test
    void isExtractableDecimalWordShouldRecognizeSimpleExtractableWords()
    {
        assertTrue(BigDecimals.isDecimal("$1,234.50"));
        assertTrue(BigDecimals.isDecimal("(1,234.50)"));
        assertTrue(BigDecimals.isDecimal(" 12.5% "));
        assertFalse(BigDecimals.isDecimal("12.5 and 7.5"));
        assertFalse(BigDecimals.isDecimal("abc"));
        assertFalse(BigDecimals.isDecimal(null));
        assertFalse(BigDecimals.isDecimal("   "));
    }

    @Test
    void parseAndExtractShouldStillHandleNormalizedInputs()
    {
        assertEquals(0, new BigDecimal("12.345").compareTo(BigDecimals.parse("12.345")));
        assertEquals(12.5d, BigDecimals.extractDouble("USD 12.50").doubleValue(), 1e-12);
        assertNull(BigDecimals.extractDouble("12.5 and 7.5"));
    }

    @Test
    void parse2ShouldHandleCurrentParseCasesExceptScientificNotation()
    {
        assertDecimalEquals("12.345", BigDecimals.parse2("12.345", 0, 6));
        assertDecimalEquals("12.5", BigDecimals.parse2(" 12.5 ", 0, 6));
        assertDecimalEquals("-12.5", BigDecimals.parse2("-12.5", 0, 5));
        assertDecimalEquals("1234.50", BigDecimals.parse2("$1,234.50", 0, 9));
        assertDecimalEquals("-1234.50", BigDecimals.parse2("(1,234.50)", 0, 10));
        assertDecimalEquals("12.345", BigDecimals.parse2("xx12.345yy", 2, 8));
        assertDecimalEquals("0.125", BigDecimals.parse2("12.5%", 0, 5));
        assertNull(BigDecimals.parse2("abc", 0, 3));
        assertNull(BigDecimals.parse2(null, 0, 0));
    }

    @Test
    void parseSliceShouldUseFastPathForPlainDecimals()
    {
        assertDecimalEquals("12.345", BigDecimals.parse("12.345", 0, 6));
        assertDecimalEquals("-12.345", BigDecimals.parse("-12.345", 0, 7));
        assertDecimalEquals("-12.345", BigDecimals.parse("(12.345)", 0, 8));
        assertDecimalEquals("12.345", BigDecimals.parse("R12.345", 0, 7));
        assertDecimalEquals("12.345", BigDecimals.parse("12.345R", 0, 7));
        assertDecimalEquals("-12.345", BigDecimals.parse("R(12.345)", 0, 9));
        assertDecimalEquals("-12.345", BigDecimals.parse("(R12.345)", 0, 9));
        assertDecimalEquals("-12.345", BigDecimals.parse("(12.345)R", 0, 9));
        assertDecimalEquals("0.12345", BigDecimals.parse("12.345%", 0, 7));
        assertDecimalEquals("0.12345", BigDecimals.parse("R12.345%", 0, 8));
        assertDecimalEquals("12", BigDecimals.parse("12.", 0, 3));
        assertDecimalEquals("0.12", BigDecimals.parse(".12", 0, 3));
        assertDecimalEquals("-0.12", BigDecimals.parse("-.12", 0, 4));
    }

    @Test
    void parseSliceShouldRejectInvalidFastPathCharacters()
    {
        assertNull(BigDecimals.parse("-1x2", 0, 4));
        assertNull(BigDecimals.parse("1x2", 0, 3));
        assertNull(BigDecimals.parse("1.2x0", 0, 5));
    }

    @Test
    void parseByteSequenceShouldAcceptKnownNbspGroupingSeparators()
    {
        assertDecimalEquals("1234.56", BigDecimals.parse(byteSequence('1', 0xA0, '2', '3', '4', '.', '5', '6'), 0, 8));
        assertDecimalEquals("1234.56",
                BigDecimals.parse(byteSequence('1', 0xC2, 0xA0, '2', '3', '4', '.', '5', '6'), 0, 9));
        assertDecimalEquals("1234.56",
                BigDecimals.parse(byteSequence('1', 0xE2, 0x80, 0xAF, '2', '3', '4', '.', '5', '6'), 0, 10));
    }

    @Test
    void parseByteSequenceShouldRejectMalformedHighBitGroupingSeparators()
    {
        assertNull(BigDecimals.parse(byteSequence('1', 0xC2, '2', '3', '4'), 0, 5));
        assertNull(BigDecimals.parse(byteSequence('1', 0xE2, 0xAF, 0x80, '2', '3', '4'), 0, 7));
        assertNull(BigDecimals.parse(byteSequence('1', '.', '2', 0xC2, 0xA0, '3'), 0, 6));
    }

    @Test
    void parseByteSequenceShouldResolveCommaDecimalAndGroupingSeparators()
    {
        assertDecimalEquals("100379.20", BigDecimals.parse(byteSequence("100,379.20"), 0, 10));
        assertDecimalEquals("100379.20", BigDecimals.parse(byteSequence("100.379,20"), 0, 10));
        assertDecimalEquals("100379.20", BigDecimals.parse(byteSequence("100 379,20"), 0, 10));
        assertDecimalEquals("1234567.89", BigDecimals.parse(byteSequence("1 234 567,89"), 0, 12));
        assertDecimalEquals("1234567", BigDecimals.parse(byteSequence("1.234.567"), 0, 9));
        assertDecimalEquals("123456", BigDecimals.parse(byteSequence("123,456"), 0, 7));
        assertDecimalEquals("123.45", BigDecimals.parse(byteSequence("123,45"), 0, 6));
    }

    @Test
    void parseByteSequenceShouldRejectGroupingAfterDecimalOrInvalidGrouping()
    {
        assertNull(BigDecimals.parse(byteSequence("123,456.00 01"), 0, 13));
        assertNull(BigDecimals.parse(byteSequence("123,45.67,00"), 0, 12));
        assertNull(BigDecimals.parse(byteSequence("1234,567"), 0, 8));
        assertNull(BigDecimals.parse(byteSequence("1 23,45"), 0, 7));
        assertNull(BigDecimals.parse(byteSequence("1  234,56"), 0, 9));
    }

    @Test
    void parseSliceShouldRejectRichDecimalsOutsideFastPath()
    {
        assertDecimalEquals("1234.50", BigDecimals.parse("$1,234.50", 0, 9));
        assertNull(BigDecimals.parse("R ( 100,379.00 )", 0, 16));
        assertDecimalEquals("12.345", BigDecimals.parse("$1,234.50%", 0, 10));
        assertNull(BigDecimals.parse("abc", 0, 3));
        assertNull(BigDecimals.parse("(123", 0, 4));
        assertNull(BigDecimals.parse("123)", 0, 4));
        assertNull(BigDecimals.parse("((123))", 0, 7));
    }

    @Test
    void parseLazyShouldUseCompactRepresentationWhenPossible()
    {
        BigDecimals.ParsedDecimal decimal = BigDecimals.parseLazy("123.4500", 0, 8);

        assertNotNull(decimal);
        assertTrue(decimal.isCompact());
        assertEquals(2, decimal.scale());
        assertDecimalEquals("123.45", decimal.toBigDecimal());
        assertTrue(decimal.toBigDecimal() == decimal.toBigDecimal());
    }

    @Test
    void parseLazyShouldParseValuesLargerThanLong()
    {
        BigDecimals.ParsedDecimal decimal = BigDecimals.parseLazy("92233720368547758070.0000", 0, 25);

        assertNotNull(decimal);
        assertFalse(decimal.isCompact());
        assertEquals(0, decimal.scale());
        assertDecimalEquals("92233720368547758070", decimal.toBigDecimal());
    }

    @Test
    void parseLazyShouldParseUnsigned128Maximum()
    {
        String value = "340282366920938463463374607431768211455";
        BigDecimals.ParsedDecimal decimal = BigDecimals.parseLazy(value, 0, value.length());

        assertNotNull(decimal);
        assertFalse(decimal.isCompact());
        assertDecimalEquals(value, decimal.toBigDecimal());
    }

    @Test
    void parseLazyShouldRejectValuesLargerThanUnsigned128()
    {
        String value = "340282366920938463463374607431768211456";

        assertNull(BigDecimals.parseLazy(value, 0, value.length()));
    }

    @Test
    void parseLazyShouldMatchParseSliceSeparatorRules()
    {
        assertDecimalEquals("1234.56", BigDecimals.parseLazy("1,234.5600", 0, 10).toBigDecimal());
        assertNull(BigDecimals.parseLazy("1234.5,6", 0, 8));
        assertNull(BigDecimals.parseLazy("12.3.4", 0, 6));
    }

    @Test
    void parse2ShouldHandleCurrencyPercentAndBracketNegatives()
    {
        assertDecimalEquals("1234.50", BigDecimals.parse2("R1234.50", 0, 8));
        assertDecimalEquals("1234.50", BigDecimals.parse2("1234.50R", 0, 8));
        assertDecimalEquals("-1234.50", BigDecimals.parse2("R(1234.50)", 0, 10));
        assertDecimalEquals("-1234.50", BigDecimals.parse2("(R1234.50)", 0, 10));
        assertDecimalEquals("-1234.50", BigDecimals.parse2("(1234.50)R", 0, 10));
        assertDecimalEquals("-100379.00", BigDecimals.parse2("R(100,379.00)", 0, 13));
        assertDecimalEquals("-100379.00", BigDecimals.parse2("R (100,379.00)", 0, 14));
        assertDecimalEquals("-100379.00", BigDecimals.parse2("R( 100,379.00)", 0, 14));
        assertDecimalEquals("-100379.00", BigDecimals.parse2("R ( 100,379.00 )", 0, 16));
        assertDecimalEquals("12.345", BigDecimals.parse2("$1,234.50%", 0, 10));
    }

    @Test
    void parse2ShouldHandleGroupedAndLocaleFormattedDecimals()
    {
        assertDecimalEquals("100379.20", BigDecimals.parse2("100,379.20", 0, 10));
        assertDecimalEquals("100379.20", BigDecimals.parse2("100.379,20", 0, 10));
        assertDecimalEquals("100379.20", BigDecimals.parse2("100 379,20", 0, 10));
        assertDecimalEquals("100379.20", BigDecimals.parse2("100 379.20", 0, 10));
        assertDecimalEquals("100379.20", BigDecimals.parse2("100\u00A0379,20", 0, 10));
        assertDecimalEquals("100379.20", BigDecimals.parse2("100\u202F379,20", 0, 10));
        assertDecimalEquals("1014.3017", BigDecimals.parse2("1 014,3017", 0, 10));
        assertDecimalEquals("1014.3017", BigDecimals.parse2("1\u00A0014,3017", 0, 10));
        assertDecimalEquals("1014.3017", BigDecimals.parse2("1\u202F014,3017", 0, 10));
        assertDecimalEquals("100000000", BigDecimals.parse2("100 000 000", 0, 11));
    }

    @Test
    void parse2ShouldUseAutoPolicyForAmbiguousCommaValues()
    {
        assertDecimalEquals("100379", BigDecimals.parse2("100,379", 0, 7));
        assertDecimalEquals("100.37", BigDecimals.parse2("100,37", 0, 6));
        assertDecimalEquals("100.3792", BigDecimals.parse2("100,3792", 0, 8));
        assertDecimalEquals("100.379", BigDecimals.parse2("100.379", 0, 7));
    }

    @Test
    void parse2ShouldAllowExplicitDecimalPolicy()
    {
        assertDecimalEquals("100.379", BigDecimals.parse2("100,379", 0, 7, BigDecimals.DecimalPolicy.COMMA_DECIMAL));
        assertDecimalEquals("100379", BigDecimals.parse2("100,379", 0, 7, BigDecimals.DecimalPolicy.PERIOD_DECIMAL));
        assertDecimalEquals("100379", BigDecimals.parse2("100.379", 0, 7, BigDecimals.DecimalPolicy.COMMA_DECIMAL));
        assertDecimalEquals("100.379", BigDecimals.parse2("100.379", 0, 7, BigDecimals.DecimalPolicy.PERIOD_DECIMAL));
    }

    @Test
    void decimalPolicyShouldBeDerivedFromLocale()
    {
        assertEquals(BigDecimals.DecimalPolicy.AUTO, BigDecimals.DecimalPolicy.fromLocale(null));
        assertEquals(BigDecimals.DecimalPolicy.PERIOD_DECIMAL, BigDecimals.DecimalPolicy.fromLocale(Locale.ROOT));
        assertEquals(BigDecimals.DecimalPolicy.PERIOD_DECIMAL, BigDecimals.DecimalPolicy.fromLocale(Locale.UK));
        assertEquals(BigDecimals.DecimalPolicy.COMMA_DECIMAL,
                BigDecimals.DecimalPolicy.fromLocale(Locale.forLanguageTag("af-ZA")));
        assertEquals(BigDecimals.DecimalPolicy.COMMA_DECIMAL,
                BigDecimals.DecimalPolicy.fromLocale(Locale.forLanguageTag("en-ZA")));
        assertEquals(BigDecimals.DecimalPolicy.PERIOD_DECIMAL,
                BigDecimals.DecimalPolicy.fromLocale(Locale.forLanguageTag("xh-ZA")));
    }

    @Test
    void parse2ShouldRejectInvalidSeparatorShapes()
    {
        assertNull(BigDecimals.parse2("100 000  000", 0, 12));
        assertNull(BigDecimals.parse2("100  000", 0, 8));
        assertNull(BigDecimals.parse2("100 ,000", 0, 8));
        assertNull(BigDecimals.parse2("100, 000", 0, 8));
        assertNull(BigDecimals.parse2("100,,000", 0, 8));
        assertNull(BigDecimals.parse2("100..000", 0, 8));
        assertNull(BigDecimals.parse2("100,000,00", 0, 10, BigDecimals.DecimalPolicy.COMMA_DECIMAL));
        assertNull(BigDecimals.parse2("100.000.00", 0, 10, BigDecimals.DecimalPolicy.PERIOD_DECIMAL));
        assertNull(BigDecimals.parse2("100 000,00.1", 0, 12));
        assertNull(BigDecimals.parse2("34%56", 0, 5));
        assertNull(BigDecimals.parse2("23(345)", 0, 7));
        assertNull(BigDecimals.parse2("123)", 0, 4));
        assertNull(BigDecimals.parse2(")123(", 0, 5));
        assertNull(BigDecimals.parse2("()", 0, 2));
    }

    private static byte[] bytes(int... values)
    {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; ++i)
        {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    private static byte[] bytes(String value)
    {
        byte[] bytes = new byte[value.length()];
        for (int i = 0; i < value.length(); ++i)
        {
            bytes[i] = (byte) value.charAt(i);
        }
        return bytes;
    }

    private static ByteSequence byteSequence(int... values)
    {
        return new TestByteSequence(bytes(values));
    }

    private static ByteSequence byteSequence(String value)
    {
        return new TestByteSequence(bytes(value));
    }

    private record TestByteSequence(byte[] bytes) implements ByteSequence
    {
        @Override
        public int length()
        {
            return this.bytes.length;
        }

        @Override
        public byte byteAt(int index)
        {
            return this.bytes[index];
        }
    }
}
