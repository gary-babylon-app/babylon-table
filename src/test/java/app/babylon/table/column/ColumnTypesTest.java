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

import org.junit.jupiter.api.Test;

import app.babylon.table.column.type.TypeParsers;
import app.babylon.table.column.ColumnTypes;

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
    }

    @Test
    void ofShouldReturnNullProducingParserForUnknownNonEnumCustomTypes()
    {
        Column.Type type = Column.Type.of(ColumnTypesTest.class, TypeParsers.NULL);

        assertEquals(TypeParsers.NULL, type.getParser());
        assertEquals(null, type.getParser().parse("anything"));
    }
}
