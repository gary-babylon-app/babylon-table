package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.type.TypeParsers;
import app.babylon.table.column.type.TypeWriters;

class ColumnTypesTest
{
    @Test
    void builtInTypesShouldExposeExpectedClassesParsersAndNames()
    {
        assertEquals(byte.class, ColumnTypes.BYTE.getValueClass());
        assertEquals(Byte.class, ColumnTypes.BYTE_OBJECT.getValueClass());
        assertEquals(int.class, ColumnTypes.INT.getValueClass());
        assertEquals(Integer.class, ColumnTypes.INT_OBJECT.getValueClass());
        assertEquals(long.class, ColumnTypes.LONG.getValueClass());
        assertEquals(Long.class, ColumnTypes.LONG_OBJECT.getValueClass());
        assertEquals(double.class, ColumnTypes.DOUBLE.getValueClass());
        assertEquals(Double.class, ColumnTypes.DOUBLE_OBJECT.getValueClass());
        assertEquals(String.class, ColumnTypes.STRING.getValueClass());
        assertEquals(BigDecimal.class, ColumnTypes.DECIMAL.getValueClass());
        assertEquals(Instant.class, ColumnTypes.INSTANT.getValueClass());
        assertEquals(LocalDateTime.class, ColumnTypes.LOCAL_DATE_TIME.getValueClass());
        assertEquals(LocalTime.class, ColumnTypes.LOCAL_TIME.getValueClass());
        assertEquals(OffsetDateTime.class, ColumnTypes.OFFSET_DATE_TIME.getValueClass());
        assertEquals(Period.class, ColumnTypes.PERIOD.getValueClass());
        assertEquals(YearMonth.class, ColumnTypes.YEAR_MONTH.getValueClass());
        assertEquals(LocalDate.class, ColumnTypes.LOCALDATE.getValueClass());
        assertEquals(TypeParsers.BYTE, ColumnTypes.BYTE.getParser());
        assertEquals(TypeParsers.INT, ColumnTypes.INT.getParser());
        assertEquals(TypeParsers.LONG, ColumnTypes.LONG.getParser());
        assertEquals(TypeParsers.DOUBLE, ColumnTypes.DOUBLE.getParser());
        assertEquals(TypeParsers.STRING, ColumnTypes.STRING.getParser());
        assertEquals(TypeParsers.BIG_DECIMAL, ColumnTypes.DECIMAL.getParser());
        assertEquals(TypeParsers.INSTANT, ColumnTypes.INSTANT.getParser());
        assertEquals(TypeParsers.LOCAL_DATE_TIME, ColumnTypes.LOCAL_DATE_TIME.getParser());
        assertEquals(TypeParsers.LOCAL_TIME, ColumnTypes.LOCAL_TIME.getParser());
        assertEquals(TypeParsers.OFFSET_DATE_TIME, ColumnTypes.OFFSET_DATE_TIME.getParser());
        assertEquals(TypeParsers.PERIOD, ColumnTypes.PERIOD.getParser());
        assertEquals(TypeParsers.YEAR_MONTH, ColumnTypes.YEAR_MONTH.getParser());
        assertEquals(TypeParsers.LOCAL_DATE_YMD, ColumnTypes.LOCALDATE.getParser());
        assertSame(TypeWriters.BYTE, ColumnTypes.BYTE.getWriter().orElseThrow());
        assertSame(TypeWriters.INT, ColumnTypes.INT.getWriter().orElseThrow());
        assertSame(TypeWriters.LONG, ColumnTypes.LONG.getWriter().orElseThrow());
        assertSame(TypeWriters.DOUBLE, ColumnTypes.DOUBLE.getWriter().orElseThrow());
        assertSame(TypeWriters.STRING, ColumnTypes.STRING.getWriter().orElseThrow());
        assertSame(TypeWriters.BIG_DECIMAL, ColumnTypes.DECIMAL.getWriter().orElseThrow());
        assertSame(TypeWriters.INSTANT, ColumnTypes.INSTANT.getWriter().orElseThrow());
        assertSame(TypeWriters.LOCAL_DATE_TIME, ColumnTypes.LOCAL_DATE_TIME.getWriter().orElseThrow());
        assertSame(TypeWriters.LOCAL_TIME, ColumnTypes.LOCAL_TIME.getWriter().orElseThrow());
        assertSame(TypeWriters.OFFSET_DATE_TIME, ColumnTypes.OFFSET_DATE_TIME.getWriter().orElseThrow());
        assertSame(TypeWriters.PERIOD, ColumnTypes.PERIOD.getWriter().orElseThrow());
        assertSame(TypeWriters.YEAR_MONTH, ColumnTypes.YEAR_MONTH.getWriter().orElseThrow());
        assertSame(TypeWriters.LOCAL_DATE, ColumnTypes.LOCALDATE.getWriter().orElseThrow());
        assertSame(TypeWriters.CURRENCY, ColumnTypes.CURRENCY.getWriter().orElseThrow());
        assertEquals("double", ColumnTypes.DOUBLE.toString());
        assertEquals("Double", ColumnTypes.DOUBLE_OBJECT.toString());
    }

    @Test
    void ofShouldCreateEquivalentCustomTypesWithoutCaching()
    {
        Column.Type first = Column.Type.of(ColumnTypesTest.class, TypeParsers.NULL);
        Column.Type second = Column.Type.of(ColumnTypesTest.class, TypeParsers.NULL);

        assertEquals(ColumnTypesTest.class, first.getValueClass());
        assertEquals(ColumnTypesTest.class, second.getValueClass());
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("ColumnTypesTest", first.toString());
        assertSame(TypeParsers.NULL, first.getParser());
        assertSame(TypeParsers.NULL, second.getParser());
        assertEquals(Optional.empty(), first.getWriter());
        assertEquals(Optional.empty(), second.getWriter());
    }

    @Test
    void ofShouldReturnNullProducingParserForUnknownNonEnumCustomTypes()
    {
        Column.Type type = Column.Type.of(ColumnTypesTest.class, TypeParsers.NULL);

        assertEquals(TypeParsers.NULL, type.getParser());
        assertEquals(null, type.getParser().parse("anything"));
        assertEquals(Optional.empty(), type.getWriter());
    }
}
