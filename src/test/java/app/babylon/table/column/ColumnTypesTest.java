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
    void ofShouldReuseBuiltInTypes()
    {
        assertSame(ColumnTypes.BYTE, Column.Type.get(byte.class));
        assertSame(ColumnTypes.BYTE_OBJECT, Column.Type.get(Byte.class));
        assertSame(ColumnTypes.INT, Column.Type.get(int.class));
        assertSame(ColumnTypes.INT_OBJECT, Column.Type.get(Integer.class));
        assertSame(ColumnTypes.LONG, Column.Type.get(long.class));
        assertSame(ColumnTypes.LONG_OBJECT, Column.Type.get(Long.class));
        assertSame(ColumnTypes.DOUBLE, Column.Type.get(double.class));
        assertSame(ColumnTypes.DOUBLE_OBJECT, Column.Type.get(Double.class));
        assertSame(ColumnTypes.STRING, Column.Type.get(String.class));
        assertSame(ColumnTypes.DECIMAL, Column.Type.get(BigDecimal.class));
        assertSame(ColumnTypes.LOCALDATE, Column.Type.get(LocalDate.class));
        assertSame(TypeParsers.STRING, ColumnTypes.BYTE.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.INT.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.LONG.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.DOUBLE.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.STRING.getParser());
        assertSame(TypeParsers.BIG_DECIMAL, ColumnTypes.DECIMAL.getParser());
        assertSame(TypeParsers.LOCAL_DATE_YMD, ColumnTypes.LOCALDATE.getParser());
        assertEquals("double", ColumnTypes.DOUBLE.toString());
        assertEquals("Double", ColumnTypes.DOUBLE_OBJECT.toString());
    }

    @Test
    void ofShouldCreateEquivalentCustomTypesWithoutCaching()
    {
        Column.Type first = Column.Type.register(ColumnTypesTest.class, TypeParsers.NULL);
        Column.Type second = Column.Type.register(ColumnTypesTest.class, TypeParsers.NULL);

        assertSame(second, Column.Type.get(ColumnTypesTest.class));
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
        Column.Type type = Column.Type.register(ColumnTypesTest.class, TypeParsers.NULL);

        assertSame(TypeParsers.NULL, type.getParser());
        assertEquals(null, type.getParser().parse("anything"));
    }
}
