package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.type.TypeParsers;

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
        assertEquals(LocalDate.class, ColumnTypes.LOCALDATE.getValueClass());
        assertEquals(TypeParsers.STRING, ColumnTypes.BYTE.getParser());
        assertEquals(TypeParsers.STRING, ColumnTypes.INT.getParser());
        assertEquals(TypeParsers.STRING, ColumnTypes.LONG.getParser());
        assertEquals(TypeParsers.STRING, ColumnTypes.DOUBLE.getParser());
        assertEquals(TypeParsers.STRING, ColumnTypes.STRING.getParser());
        assertEquals(TypeParsers.BIG_DECIMAL, ColumnTypes.DECIMAL.getParser());
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
