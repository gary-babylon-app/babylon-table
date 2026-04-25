/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ColumnsTest
{
    @Test
    void frequencyMapCountsSetValues()
    {
        final ColumnName CITY = ColumnName.of("city");
        ColumnObject.Builder<String> strings = ColumnObject.builder(CITY, ColumnTypes.STRING);
        strings.add("London");
        strings.add("Paris");
        strings.add("London");
        strings.addNull();
        strings.add("Paris");
        strings.add("London");

        Map<String, Integer> frequencies = Columns.frequencyMap(strings.build());

        assertEquals(2, frequencies.size());
        assertEquals(3, frequencies.get("London"));
        assertEquals(2, frequencies.get("Paris"));
    }

    @Test
    void isEmptyReturnsTrueForNullColumn()
    {
        assertTrue(Columns.isEmpty(null));
    }

    @Test
    void columnIsEmptyShouldTreatZeroRowsAndAllUnsetAsEmpty()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnObject.Builder<String> zeroRowsBuilder = ColumnObject.builder(VALUES, ColumnTypes.STRING);
        ColumnObject<String> zeroRows = zeroRowsBuilder.build();

        ColumnObject.Builder<String> allUnsetBuilder = ColumnObject.builder(VALUES, ColumnTypes.STRING);
        allUnsetBuilder.addNull();
        allUnsetBuilder.addNull();
        ColumnObject<String> allUnset = allUnsetBuilder.build();

        ColumnObject.Builder<String> someValuesBuilder = ColumnObject.builder(VALUES, ColumnTypes.STRING);
        someValuesBuilder.addNull();
        someValuesBuilder.add("x");
        ColumnObject<String> someValues = someValuesBuilder.build();

        assertTrue(zeroRows.isEmpty());
        assertTrue(allUnset.isEmpty());
        assertFalse(someValues.isEmpty());
        assertTrue(Columns.isEmpty(zeroRows));
        assertTrue(Columns.isEmpty(allUnset));
        assertFalse(Columns.isEmpty(someValues));
    }

    @Test
    void stringToTypeSupportsBigDecimal()
    {
        final ColumnName AMOUNT = ColumnName.of("amount");
        ColumnObject.Builder<String> strings = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        strings.add("1.25");
        strings.add("");
        strings.addNull();
        strings.add("-2.50");

        ColumnObject<BigDecimal> decimals = Columns.stringToType(strings.build(), BigDecimal::new, ColumnTypes.DECIMAL);

        assertEquals(4, decimals.size());
        assertEquals(0, new BigDecimal("1.25").compareTo(decimals.get(0)));
        assertFalse(decimals.isSet(1));
        assertFalse(decimals.isSet(2));
        assertEquals(0, new BigDecimal("-2.50").compareTo(decimals.get(3)));

        ColumnObject.Builder<BigDecimal> existingBuilder = ColumnObject.builderDecimal(AMOUNT);
        existingBuilder.add(new BigDecimal("7.25"));
        ColumnObject<BigDecimal> existing = existingBuilder.build();

        ColumnObject<BigDecimal> same = Columns.stringToType(existing, BigDecimal::new, ColumnTypes.DECIMAL);

        assertSame(existing, same);
    }

    @Test
    void stringToTypeReturnsNullForNonObjectColumns()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt.Builder ints = ColumnInt.builder(VALUES);
        ints.add(1);

        assertNull(Columns.stringToType(ints.build(), BigDecimal::new, ColumnTypes.DECIMAL));
    }

    @Test
    void stringToDecimalSupportsFormattedDecimalText()
    {
        final ColumnName AMOUNT = ColumnName.of("amount");
        ColumnObject.Builder<String> strings = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        strings.add("1,234.50");
        strings.add("");
        strings.addNull();
        strings.add("(2.50)");

        ColumnObject<BigDecimal> decimals = Columns.stringToDecimal(strings.build());

        assertEquals(4, decimals.size());
        assertEquals(0, new BigDecimal("1234.50").compareTo(decimals.get(0)));
        assertFalse(decimals.isSet(1));
        assertFalse(decimals.isSet(2));
        assertEquals(0, new BigDecimal("-2.50").compareTo(decimals.get(3)));
    }

    @Test
    void maxAndMinSupportComparableObjectColumns()
    {
        final ColumnName TRADE_DATE = ColumnName.of("tradeDate");
        ColumnObject.Builder<LocalDate> dates = ColumnObject.builder(TRADE_DATE, ColumnTypes.LOCALDATE);
        dates.add(LocalDate.of(2026, 4, 18));
        dates.addNull();
        dates.add(LocalDate.of(2026, 4, 16));
        dates.add(LocalDate.of(2026, 4, 17));

        ColumnObject<LocalDate> column = dates.build();

        assertEquals(LocalDate.of(2026, 4, 18), Columns.max(column));
        assertEquals(LocalDate.of(2026, 4, 16), Columns.min(column));
    }

    @Test
    void maxAndMinThrowOnEmptyComparableObjectColumns()
    {
        final ColumnName TRADE_DATE = ColumnName.of("tradeDate");
        ColumnObject.Builder<LocalDate> builder = ColumnObject.builder(TRADE_DATE, ColumnTypes.LOCALDATE);
        ColumnObject<LocalDate> dates = builder.build();

        assertThrows(RuntimeException.class, () -> Columns.max(dates));
        assertThrows(RuntimeException.class, () -> Columns.min(dates));
    }

    @Test
    void newColumnSupportsIntType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        Column.Builder builder = Columns.newBuilder(VALUES, ColumnTypes.INT);

        assertInstanceOf(ColumnInt.Builder.class, builder);
    }

    @Test
    void newColumnSupportsDoubleType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        Column.Builder builder = Columns.newBuilder(VALUES, ColumnTypes.DOUBLE);

        assertInstanceOf(ColumnDouble.Builder.class, builder);
    }

    @Test
    void newColumnSupportsLongType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        Column.Builder builder = Columns.newBuilder(VALUES, ColumnTypes.LONG);

        assertInstanceOf(ColumnLong.Builder.class, builder);
    }

    @Test
    void newColumnSupportsByteType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        Column.Builder builder = Columns.newBuilder(VALUES, ColumnTypes.BYTE);

        assertInstanceOf(ColumnByte.Builder.class, builder);
    }

    @Test
    void newCharSliceBuilderSupportsIntType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        Column.Builder builder = Columns.newBuilder(VALUES, ColumnTypes.INT);
        builder.add("12", 0, 2);
        builder.add(null, 0, 0);

        ColumnInt column = (ColumnInt) builder.build();
        assertEquals(12, column.get(0));
        assertFalse(column.isSet(1));
    }

    @Test
    void newCharSliceBuilderSupportsByteType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        Column.Builder builder = Columns.newBuilder(VALUES, ColumnTypes.BYTE);
        builder.add("7", 0, 1);
        builder.add(null, 0, 0);

        ColumnByte column = (ColumnByte) builder.build();
        assertEquals((byte) 7, column.get(0));
        assertEquals("", column.toString(1));
    }

    @Test
    void newCharSliceBuilderSupportsLongType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        Column.Builder builder = Columns.newBuilder(VALUES, ColumnTypes.LONG);
        builder.add("123456789", 0, 9);
        builder.add(null, 0, 0);

        ColumnLong column = (ColumnLong) builder.build();
        assertEquals(123456789L, column.get(0));
        assertFalse(column.isSet(1));
    }

    @Test
    void newCharSliceBuilderSupportsStringType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnObject.Builder<String> builder = ColumnObject.builder(VALUES);
        builder.add("Alpha", 0, 5);
        builder.add(null, 0, 0);

        ColumnObject<String> column = builder.build();
        assertEquals("Alpha", column.get(0));
        assertFalse(column.isSet(1));
    }

    @Test
    void newCharSliceBuilderSupportsDecimalType()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnObject.Builder<BigDecimal> builder = ColumnObject.builder(VALUES, ColumnTypes.DECIMAL);
        builder.add("1234.50", 0, 7);
        builder.add("bad", 0, 3);

        ColumnObject<BigDecimal> column = builder.build();
        assertEquals(0, new BigDecimal("1234.50").compareTo(column.get(0)));
        assertFalse(column.isSet(1));
    }

    @Test
    void newIntCreatesConstantIntColumn()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt ints = Columns.newInt(VALUES, 7, 3);

        assertEquals(3, ints.size());
        assertEquals(7, ints.get(0));
        assertEquals(7, ints.get(2));
        assertTrue(ints.isAllSet());
    }

    @Test
    void newDoubleCreatesConstantDoubleColumn()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnDouble doubles = Columns.newDouble(VALUES, 1.5d, 3);

        assertEquals(3, doubles.size());
        assertEquals(1.5d, doubles.get(0), 1e-12);
        assertEquals(1.5d, doubles.get(2), 1e-12);
        assertTrue(doubles.isAllSet());
    }

    @Test
    void newStringCreatesConstantStringColumn()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnObject<String> strings = Columns.newString(VALUES, "ABC", 3);

        assertEquals(3, strings.size());
        assertEquals("ABC", strings.get(0));
        assertEquals("ABC", strings.get(2));
        assertTrue(strings.isAllSet());
    }

    @Test
    void newCategoricalCreatesConstantCategoricalColumn()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnCategorical<String> categorical = Columns.newCategorical(VALUES, "ABC", 3, ColumnTypes.STRING);

        assertEquals(3, categorical.size());
        assertEquals("ABC", categorical.get(0));
        assertEquals("ABC", categorical.get(2));
        assertTrue(categorical.isAllSet());
    }

    @Test
    void sortUsesColumnNaturalOrderAndReturnsView()
    {
        final ColumnName CITY = ColumnName.of("city");
        ColumnObject.Builder<String> strings = ColumnObject.builder(CITY, ColumnTypes.STRING, ColumnObject.Mode.ARRAY);
        strings.add("Zurich");
        strings.addNull();
        strings.add("Amsterdam");
        strings.add("London");

        ColumnObject<String> sorted = (ColumnObject<String>) Columns.sort(strings.build());

        assertFalse(sorted.isSet(0));
        assertEquals("Amsterdam", sorted.get(1));
        assertEquals("London", sorted.get(2));
        assertEquals("Zurich", sorted.get(3));
    }

    @Test
    void sortUsesCustomComparator()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt.Builder ints = ColumnInt.builder(VALUES);
        ints.add(1);
        ints.add(3);
        ints.add(2);

        ColumnInt source = ints.build();
        ColumnInt sorted = (ColumnInt) Columns.sort(source, (a, b) -> Integer.compare(source.get(b), source.get(a)));

        assertEquals(3, sorted.get(0));
        assertEquals(2, sorted.get(1));
        assertEquals(1, sorted.get(2));
    }

    @Test
    void concatKeepsCategoricalColumnsCategorical()
    {
        final ColumnName CITY = ColumnName.of("city");
        ColumnCategorical.Builder<String> left = ColumnCategorical.builder(CITY, ColumnTypes.STRING);
        left.add("London");
        left.addNull();
        ColumnCategorical.Builder<String> right = ColumnCategorical.builder(CITY, ColumnTypes.STRING);
        right.add("Paris");
        right.add("Rome");

        Column result = Columns.concat(List.of(left.build(), right.build()));

        ColumnCategorical<?> categorical = assertInstanceOf(ColumnCategorical.class, result);
        assertArrayEquals(new String[]
        {"London", null, "Paris", "Rome"}, values((ColumnObject<String>) categorical));
        assertEquals(CITY, result.getName());
    }

    @Test
    void concatReturnsNullForEmptyList()
    {
        assertNull(Columns.concat(List.of()));
    }

    @Test
    void concatReturnsNullForNullList()
    {
        assertNull(Columns.concat(null));
    }

    @Test
    void concatSupportsMixedStringBackingsWhenValueClassMatches()
    {
        final ColumnName CITY = ColumnName.of("city");
        ColumnCategorical.Builder<String> categorical = ColumnCategorical.builder(CITY, ColumnTypes.STRING);
        categorical.add("London");

        ColumnObject.Builder<String> objects = ColumnObject.builder(CITY, ColumnTypes.STRING, ColumnObject.Mode.ARRAY);
        objects.add("Paris");
        objects.addNull();

        Column result = Columns.concat(List.of(categorical.build(), objects.build()));

        ColumnObject<?> objectColumn = assertInstanceOf(ColumnObject.class, result);
        assertArrayEquals(new String[]
        {"London", "Paris", null}, values((ColumnObject<String>) objectColumn));
    }

    @Test
    void concatRejectsDifferentValueClasses()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnObject.Builder<String> strings = ColumnObject.builder(VALUES, ColumnTypes.STRING);
        strings.add("A");

        ColumnInt.Builder ints = ColumnInt.builder(VALUES);
        ints.add(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Columns.concat(List.of(strings.build(), ints.build())));

        assertEquals("Cannot concat different column types: " + ColumnTypes.STRING + " and " + ColumnTypes.INT
                + " for column " + VALUES, ex.getMessage());
    }

    @Test
    void concatSupportsIntColumns()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnInt.Builder left = ColumnInt.builder(VALUES);
        left.add(10);
        left.addNull();
        ColumnInt.Builder right = ColumnInt.builder(VALUES);
        right.add(-3);

        Column result = Columns.concat(List.of(left.build(), right.build()));

        ColumnInt ints = assertInstanceOf(ColumnInt.class, result);
        assertEquals(VALUES, ints.getName());
        assertEquals(3, ints.size());
        assertEquals(10, ints.get(0));
        assertEquals(-3, ints.get(2));
        assertEquals("10", ints.toString(0));
        assertEquals("", ints.toString(1));
        assertEquals("-3", ints.toString(2));
    }

    @Test
    void concatSupportsLongColumns()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnLong.Builder left = ColumnLong.builder(VALUES);
        left.add(100L);
        ColumnLong.Builder right = ColumnLong.builder(VALUES);
        right.addNull();
        right.add(200L);

        Column result = Columns.concat(List.of(left.build(), right.build()));

        ColumnLong longs = assertInstanceOf(ColumnLong.class, result);
        assertEquals(VALUES, longs.getName());
        assertEquals(3, longs.size());
        assertEquals(100L, longs.get(0));
        assertEquals("", longs.toString(1));
        assertEquals("200", longs.toString(2));
    }

    @Test
    void concatSupportsDoubleColumns()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnDouble.Builder left = ColumnDouble.builder(VALUES);
        left.add(1.5d);
        ColumnDouble.Builder right = ColumnDouble.builder(VALUES);
        right.addNull();
        right.add(-2.25d);

        Column result = Columns.concat(List.of(left.build(), right.build()));

        ColumnDouble doubles = assertInstanceOf(ColumnDouble.class, result);
        assertEquals(VALUES, doubles.getName());
        assertEquals(3, doubles.size());
        assertEquals(1.5d, doubles.get(0), 1e-12);
        assertEquals("", doubles.toString(1));
        assertEquals("-2.25", doubles.toString(2));
    }

    @Test
    void concatSupportsByteColumns()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnByte.Builder left = ColumnByte.builder(VALUES);
        left.add((byte) 7);
        ColumnByte.Builder right = ColumnByte.builder(VALUES);
        right.addNull();
        right.add((byte) -1);

        Column result = Columns.concat(List.of(left.build(), right.build()));

        ColumnByte bytes = assertInstanceOf(ColumnByte.class, result);
        assertEquals(VALUES, bytes.getName());
        assertEquals(3, bytes.size());
        assertEquals((byte) 7, bytes.get(0));
        assertEquals("", bytes.toString(1));
        assertEquals("-1", bytes.toString(2));
    }

    private static String[] values(ColumnObject<String> column)
    {
        String[] values = new String[column.size()];
        for (int i = 0; i < column.size(); ++i)
        {
            values[i] = column.get(i);
        }
        return values;
    }
}
