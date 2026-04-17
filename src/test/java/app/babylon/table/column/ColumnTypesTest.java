package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.type.TypeParsers;

class ColumnTypesTest
{
    private enum Status
    {
        ACTIVE, INACTIVE
    }

    @Test
    void ofShouldReuseBuiltInTypes()
    {
        assertSame(ColumnTypes.BYTE, Column.Type.of(byte.class));
        assertSame(ColumnTypes.BYTE_OBJECT, Column.Type.of(Byte.class));
        assertSame(ColumnTypes.INT, Column.Type.of(int.class));
        assertSame(ColumnTypes.INT_OBJECT, Column.Type.of(Integer.class));
        assertSame(ColumnTypes.LONG, Column.Type.of(long.class));
        assertSame(ColumnTypes.LONG_OBJECT, Column.Type.of(Long.class));
        assertSame(ColumnTypes.DOUBLE, Column.Type.of(double.class));
        assertSame(ColumnTypes.DOUBLE_OBJECT, Column.Type.of(Double.class));
        assertSame(ColumnTypes.STRING, Column.Type.of(String.class));
        assertSame(ColumnTypes.DECIMAL, Column.Type.of(BigDecimal.class));
        assertSame(ColumnTypes.LOCALDATE, Column.Type.of(LocalDate.class));
        assertSame(TypeParsers.STRING, ColumnTypes.BYTE.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.INT.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.LONG.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.DOUBLE.getParser());
        assertSame(TypeParsers.STRING, ColumnTypes.STRING.getParser());
        assertSame(TypeParsers.BIG_DECIMAL, ColumnTypes.DECIMAL.getParser());
        assertSame(TypeParsers.LOCAL_DATE_YMD, ColumnTypes.LOCALDATE.getParser());
    }

    @Test
    void ofShouldCreateEquivalentCustomTypesWithoutCaching()
    {
        Column.Type first = Column.Type.of(Status.class);
        Column.Type second = Column.Type.of(Status.class);

        assertEquals(Status.class, first.getValueClass());
        assertEquals(Status.class, second.getValueClass());
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("status", first.id());
        assertEquals(Status.ACTIVE, first.getParser().parse("ACTIVE"));
        assertEquals(Status.INACTIVE, first.getParser().parse(" inactive "));
        assertSame(first.getParser(), first.getParser());
    }

    @Test
    void ofShouldReturnNullParserForUnknownNonEnumCustomTypes()
    {
        Column.Type type = Column.Type.of(ColumnTypesTest.class);

        assertNull(type.getParser());
    }
}
