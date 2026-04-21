package app.babylon.table.column.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class TypeParserTest
{
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
    void boxedPrimitiveParsersShouldReturnTypedValues()
    {
        assertEquals(Byte.valueOf((byte) 12), TypeParsers.BYTE.parse("12"));
        assertEquals(Integer.valueOf(345), TypeParsers.INT.parse("345"));
        assertEquals(Long.valueOf(6789012345L), TypeParsers.LONG.parse("6789012345"));
        assertEquals(Double.valueOf(9.75d), TypeParsers.DOUBLE.parse("9.75"));
        assertEquals(Integer.valueOf(345), TypeParsers.INT.parse("xx345yy", 2, 3));
        assertNull(TypeParsers.INT.parse(""));
        assertNull(TypeParsers.LONG.parse((CharSequence) null));
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
    void objectParserShouldWorkWithInstant()
    {
        TypeParser<Instant> parser = TypeParsers.INSTANT;
        Instant expected = Instant.parse("2026-04-21T10:15:30Z");
        String chars = "xx2026-04-21T10:15:30Zyy";

        assertEquals(expected, parser.parse("2026-04-21T10:15:30Z"));
        assertEquals(expected, parser.parse(chars, 2, 20));
        assertNull(parser.parse("not-an-instant"));
    }

    @Test
    void instantParserShouldTrimWhitespaceAtTheEdges()
    {
        TypeParser<Instant> parser = TypeParsers.INSTANT;
        Instant expected = Instant.parse("2026-04-21T10:15:30Z");
        String chars = "xx 2026-04-21T10:15:30Z yy";

        assertEquals(expected, parser.parse(" 2026-04-21T10:15:30Z "));
        assertEquals(expected, parser.parse(chars, 2, 22));
    }

    @Test
    void objectParserShouldWorkWithLocalDateTime()
    {
        TypeParser<LocalDateTime> parser = TypeParsers.LOCAL_DATE_TIME;
        LocalDateTime expected = LocalDateTime.of(2026, 4, 21, 10, 15, 30);
        String chars = "xx2026-04-21T10:15:30yy";

        assertEquals(expected, parser.parse("2026-04-21T10:15:30"));
        assertEquals(expected, parser.parse(chars, 2, 19));
        assertNull(parser.parse("not-a-local-date-time"));
    }

    @Test
    void localDateTimeParserShouldTrimWhitespaceAtTheEdges()
    {
        TypeParser<LocalDateTime> parser = TypeParsers.LOCAL_DATE_TIME;
        LocalDateTime expected = LocalDateTime.of(2026, 4, 21, 10, 15, 30);
        String chars = "xx 2026-04-21T10:15:30 yy";

        assertEquals(expected, parser.parse(" 2026-04-21T10:15:30 "));
        assertEquals(expected, parser.parse(chars, 2, 21));
    }

    @Test
    void objectParserShouldWorkWithLocalTime()
    {
        TypeParser<LocalTime> parser = TypeParsers.LOCAL_TIME;
        LocalTime expected = LocalTime.of(10, 15, 30);
        String chars = "xx10:15:30yy";

        assertEquals(expected, parser.parse("10:15:30"));
        assertEquals(expected, parser.parse(chars, 2, 8));
        assertNull(parser.parse("not-a-local-time"));
    }

    @Test
    void localTimeParserShouldTrimWhitespaceAtTheEdges()
    {
        TypeParser<LocalTime> parser = TypeParsers.LOCAL_TIME;
        LocalTime expected = LocalTime.of(10, 15, 30);
        String chars = "xx 10:15:30 yy";

        assertEquals(expected, parser.parse(" 10:15:30 "));
        assertEquals(expected, parser.parse(chars, 2, 10));
    }

    @Test
    void objectParserShouldWorkWithOffsetDateTime()
    {
        TypeParser<OffsetDateTime> parser = TypeParsers.OFFSET_DATE_TIME;
        OffsetDateTime expected = OffsetDateTime.parse("2026-04-21T10:15:30Z");
        String chars = "xx2026-04-21T10:15:30Zyy";

        assertEquals(expected, parser.parse("2026-04-21T10:15:30Z"));
        assertEquals(expected, parser.parse(chars, 2, 20));
        assertNull(parser.parse("not-an-offset-date-time"));
    }

    @Test
    void offsetDateTimeParserShouldTrimWhitespaceAtTheEdges()
    {
        TypeParser<OffsetDateTime> parser = TypeParsers.OFFSET_DATE_TIME;
        OffsetDateTime expected = OffsetDateTime.parse("2026-04-21T10:15:30Z");
        String chars = "xx 2026-04-21T10:15:30Z yy";

        assertEquals(expected, parser.parse(" 2026-04-21T10:15:30Z "));
        assertEquals(expected, parser.parse(chars, 2, 22));
    }

    @Test
    void objectParserShouldWorkWithYearMonth()
    {
        TypeParser<YearMonth> parser = TypeParsers.YEAR_MONTH;
        YearMonth expected = YearMonth.of(2026, 4);
        String chars = "xx2026-04yy";

        assertEquals(expected, parser.parse("2026-04"));
        assertEquals(expected, parser.parse(chars, 2, 7));
        assertNull(parser.parse("not-a-year-month"));
    }

    @Test
    void yearMonthParserShouldTrimWhitespaceAtTheEdges()
    {
        TypeParser<YearMonth> parser = TypeParsers.YEAR_MONTH;
        YearMonth expected = YearMonth.of(2026, 4);
        String chars = "xx 2026-04 yy";

        assertEquals(expected, parser.parse(" 2026-04 "));
        assertEquals(expected, parser.parse(chars, 2, 9));
    }

    @Test
    void objectParserShouldWorkWithPeriod()
    {
        TypeParser<Period> parser = TypeParsers.PERIOD;

        assertEquals(Period.ofMonths(1), parser.parse("1M"));
        assertEquals(Period.ofMonths(3), parser.parse("P3M"));
        assertEquals(Period.ofMonths(3), parser.parse("3M"));
        assertEquals(Period.ofMonths(6), parser.parse("6m"));
        assertEquals(Period.ofMonths(12), parser.parse("12M"));
        assertEquals(Period.ofYears(1), parser.parse("1Y"));
        assertEquals(Period.ofYears(1), parser.parse("1y"));
        assertEquals(Period.ofMonths(-3), parser.parse("-P3M"));
        assertEquals(Period.ofMonths(-3), parser.parse("-3M"));
        assertEquals(Period.ofDays(10), parser.parse("xx10Dyy", 2, 3));
        assertNull(parser.parse("not-a-period"));
    }

    @Test
    void periodParserShouldTrimWhitespaceAtTheEdges()
    {
        TypeParser<Period> parser = TypeParsers.PERIOD;
        String chars = "xx 3M yy";

        assertEquals(Period.ofMonths(3), parser.parse(" 3M "));
        assertEquals(Period.ofMonths(3), parser.parse(" P3M "));
        assertEquals(Period.ofMonths(-3), parser.parse(" -P3M "));
        assertEquals(Period.ofMonths(3), parser.parse(chars, 2, 4));
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
