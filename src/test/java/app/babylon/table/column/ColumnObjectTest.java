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

import app.babylon.table.ViewIndex;
import app.babylon.table.aggregation.Aggregate;
import app.babylon.table.selection.Selection;
import app.babylon.table.transform.ColumnLocalDates;
import app.babylon.table.transform.DateFormat;
import app.babylon.text.BigDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ColumnObjectTest
{
    @Test
    void stringColumnsShouldFormatCompactToString()
    {
        final ColumnName TEST = ColumnName.of("Test");
        ColumnObject.Builder<String> strings = ColumnObject.builder(TEST, ColumnTypes.STRING);
        strings.add(BigDecimal.ZERO.toPlainString());
        strings.add(BigDecimal.ONE.toPlainString());
        strings.add(BigDecimal.TEN.toPlainString());

        assertEquals("Test[0, 1, ... ,10]", strings.build().toString());
    }

    @Test
    void stringViewsShouldRespectViewOrder()
    {
        final ColumnName TEST = ColumnName.of("TEST");
        ColumnObject.Builder<String> original = ColumnObject.builder(TEST, ColumnTypes.STRING);
        original.add("abc1");
        original.add("abc2");
        original.add("abc3");
        original.add("abc4");

        ViewIndex rowIndex = ViewIndex.builder().addAll(new int[]
        {1, 3}).build();
        ColumnObject<String> view = Columns.newStringView(original.build(), rowIndex);

        assertEquals(2, view.size());
        assertEquals("abc2", view.get(0));
        assertEquals("abc4", view.get(1));
    }

    @Test
    void localDateColumnsShouldSupportParsingConstantChecksAndViews()
    {
        LocalDate d = ColumnLocalDates.stringToDate("45436.2980092593", DateFormat.ExcelLocalDateTime);
        assertNotNull(d);

        final ColumnName TEST = ColumnName.of("TEST");
        ColumnObject.Builder<LocalDate> dates = ColumnObject.builder(TEST, ColumnTypes.LOCALDATE);
        dates.add(LocalDate.of(2026, 8, 20));
        dates.addNull();
        dates.addNull();
        assertFalse(dates.build().isConstant());

        ColumnObject.Builder<LocalDate> dates2 = ColumnObject.builder(TEST, ColumnTypes.LOCALDATE);
        dates2.addNull();
        dates2.addNull();
        dates2.add(LocalDate.of(2026, 8, 20));
        dates2.addNull();
        assertFalse(dates2.build().isConstant());

        ColumnObject.Builder<LocalDate> original = ColumnObject.builder(TEST, ColumnTypes.LOCALDATE);
        original.add(ColumnLocalDates.stringToDate("2024-08-20", DateFormat.YMD));
        original.add(ColumnLocalDates.stringToDate("2023/08/20", DateFormat.YMD));
        original.add(ColumnLocalDates.stringToDate("20230220", DateFormat.YMD));
        original.addNull();

        ColumnObject<LocalDate> view = original.build().view(ViewIndex.builder().addAll(new int[]
        {1, 3}).build());
        assertEquals(2, view.size());
        assertEquals(LocalDate.of(2023, 8, 20), view.get(0));
        assertNull(view.get(1));
    }

    @Test
    void decimalColumnsShouldCompareFormatParseAndAggregate()
    {
        final ColumnName TEST = ColumnName.of("test");
        ColumnObject.Builder<BigDecimal> cd1Builder = ColumnObject.builderDecimal(TEST);
        cd1Builder.add(BigDecimals.parse("0.01"));
        cd1Builder.add(BigDecimals.parse("0.02"));
        ColumnObject<BigDecimal> cd1 = cd1Builder.build();

        ColumnObject.Builder<BigDecimal> cd2Builder = ColumnObject.builderDecimal(TEST);
        cd2Builder.add(BigDecimals.parse("0.010"));
        cd2Builder.add(BigDecimals.parse("0.020"));
        ColumnObject<BigDecimal> cd2 = cd2Builder.build();

        assertEquals(cd1, cd1);
        assertEquals(cd1, cd2);

        ColumnObject<BigDecimal> constant = Columns.newDecimal(TEST, BigDecimal.TEN, 3);
        ColumnObject<BigDecimal> constant2 = Columns.newDecimal(TEST, BigDecimal.TEN, 3);
        assertEquals(constant, constant);
        assertEquals(constant, constant2);

        assertEquals("32000.00", BigDecimals.removeCommas("32,000.00"));
        assertEquals("-32000.00", BigDecimals.removeCommas("-32,000.00"));

        assertNull(BigDecimals.parse("1.2 3"));
        assertNull(BigDecimals.parse("1  23"));
        assertNull(BigDecimals.parse("1..23"));
        assertNull(BigDecimals.parse("12.3.4"));
        assertNull(BigDecimals.parse(")(123"));
        assertNull(BigDecimals.parse("123%123"));
        assertNull(BigDecimals.parse("123$123"));

        BigDecimals.PreparedDecimal prepared = BigDecimals.prepare("$1,234.50%");
        assertEquals("1234.50", prepared.normalizedNumberText());
        assertTrue(prepared.isPercent());
        assertFalse(prepared.isNegativeBracket());

        BigDecimal decimal = BigDecimals.parse("$1,234.50%");
        Double dbl = BigDecimals.parseDouble("$1,234.50%");
        assertTrue(new BigDecimal("12.345").compareTo(decimal) == 0);
        assertEquals(12.345d, dbl.doubleValue(), 1.0e-12);

        BigDecimal negativeBracketDecimal = BigDecimals.parse("(1,234.50)");
        Double negativeBracketDouble = BigDecimals.parseDouble("(1,234.50)");
        assertTrue(new BigDecimal("-1234.50").compareTo(negativeBracketDecimal) == 0);
        assertEquals(-1234.5d, negativeBracketDouble.doubleValue(), 1.0e-12);

        assertTrue(new BigDecimal("1e6").compareTo(BigDecimals.parse("1e6")) == 0);
        assertEquals(1.0e6d, BigDecimals.parseDouble("1e6").doubleValue(), 1.0e-12);
        assertTrue(new BigDecimal("1E-6").compareTo(BigDecimals.parse("1E-6")) == 0);
        assertEquals(1.0e-6d, BigDecimals.parseDouble("1E-6").doubleValue(), 1.0e-18);

        assertEquals(12.5d, BigDecimals.extractDouble("USD 12.50").doubleValue(), 1.0e-12);
        assertNull(BigDecimals.extractDouble("12.5 and 7.5"));
        assertNull(BigDecimals.prepare("1  23"));
    }

    @Test
    void decimalColumnsShouldFormatAggregateAndView()
    {
        final ColumnName TEST = ColumnName.of("Test");
        ColumnObject.Builder<BigDecimal> cdBuilder = ColumnObject.builderDecimal(TEST);
        cdBuilder.add(BigDecimal.ZERO);
        cdBuilder.add(BigDecimal.ONE);
        assertEquals("Test[0, 1, ... ,1]", cdBuilder.build().toString());

        ColumnObject.Builder<BigDecimal> aggregateBuilder = ColumnObject.builderDecimal(TEST);
        aggregateBuilder.add(BigDecimal.ONE);
        aggregateBuilder.addNull();
        aggregateBuilder.add(BigDecimal.ONE);
        aggregateBuilder.add(BigDecimal.TEN);
        aggregateBuilder.add(new BigDecimal("-2"));
        ColumnObject<BigDecimal> cd = aggregateBuilder.build();

        assertTrue(new BigDecimal("10").compareTo(Columns.aggregate(cd, Aggregate.SUM)) == 0);
        assertTrue(new BigDecimal("-2").compareTo(Columns.aggregate(cd, Aggregate.MIN)) == 0);
        assertTrue(BigDecimal.TEN.compareTo(Columns.aggregate(cd, Aggregate.MAX)) == 0);
        assertTrue(new BigDecimal("2.5").compareTo(Columns.aggregate(cd, Aggregate.MEAN)) == 0);
        assertNull(Columns.aggregate(ColumnObject.builderDecimal(TEST).add(BigDecimal.ONE).build(), Aggregate.COUNT));

        ColumnObject.Builder<BigDecimal> originalBuilder = ColumnObject.builderDecimal(ColumnName.of("TEST"));
        originalBuilder.add(BigDecimals.parse("1.42"));
        originalBuilder.add(BigDecimals.parse("100,100.32"));
        originalBuilder.add(BigDecimals.parse(""));
        originalBuilder.add(BigDecimals.parse("99.99"));
        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> view = (ColumnObject<BigDecimal>) Columns.newView(originalBuilder.build(),
                ViewIndex.builder().addAll(new int[]
                {1, 3}).build());

        assertEquals(2, view.size());
        assertEquals(new BigDecimal("100100.32"), view.get(0));
        assertEquals(new BigDecimal("99.99"), view.get(1));
    }

    @Test
    void objectViewsShouldHandleNullJoinRows()
    {
        final ColumnName TEST = ColumnName.of("TEST");
        ColumnObject.Builder<String> original = ColumnObject.builder(TEST, ColumnTypes.STRING);
        original.add("abc1");
        original.add("abc2");
        original.add("abc3");

        ColumnObject<String> join = original.build().view(ViewIndex.builder().add(1).addNull().add(1).build());

        assertEquals(3, join.size());
        assertEquals("abc2", join.get(0));
        assertTrue(join.isSet(0));
        assertNull(join.get(1));
        assertFalse(join.isSet(1));
        assertEquals("abc2", join.get(2));
        assertFalse(join.isConstant());
    }

    @Test
    void selectShouldUsePredicateAndTreatNullAsFalse()
    {
        final ColumnName OBJ = ColumnName.of("OBJ");
        ColumnObject.Builder<String> builder = ColumnObject.builder(OBJ, ColumnTypes.STRING);
        builder.add("a");
        builder.addNull();
        builder.add("bb");

        ColumnObject<String> column = builder.build();
        Selection selection = column.select(s -> s.length() == 1);

        assertEquals(3, selection.size());
        assertTrue(selection.get(0));
        assertFalse(selection.get(1));
        assertFalse(selection.get(2));
        assertEquals(1, selection.selected());
    }

    @Test
    void objectTransformShouldUseTransformerColumnNameAndPreserveNullnessOnView()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName AMOUNT_TEXT = ColumnName.of("AMOUNT_TEXT");
        ColumnObject.Builder<BigDecimal> builder = ColumnObject.builderDecimal(AMOUNT);
        builder.add(new BigDecimal("10.5"));
        builder.addNull();
        builder.add(new BigDecimal("3.0"));
        ColumnObject<BigDecimal> column = builder.build();

        ColumnObject<BigDecimal> view = column.view(ViewIndex.builder().add(2).add(1).build());
        ColumnObject<String> transformed = view
                .transform(Transformer.of(BigDecimal::toPlainString, String.class, AMOUNT_TEXT));

        assertEquals(AMOUNT_TEXT, transformed.getName());
        assertEquals("3.0", transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertEquals(2, transformed.size());
    }

    @Test
    void objectColumnsShouldGetAllAndUniquesWithoutBecomingCategorical()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnObject.Builder<String> builder = ColumnObject.builder(S, ColumnTypes.STRING);
        for (int i = 0; i < 260; ++i)
        {
            if (i == 120)
            {
                builder.addNull();
            }
            builder.add("value-" + i);
        }

        ColumnObject<String> column = builder.build();
        assertFalse(column instanceof ColumnCategorical<?>);

        List<String> all = (List<String>) column.getAll(new ArrayList<>());
        Set<String> uniques = column.getUniques(null);

        assertEquals(260, all.size());
        assertFalse(all.contains(null));
        assertEquals("value-0", all.get(0));
        assertEquals("value-119", all.get(119));
        assertEquals("value-120", all.get(120));
        assertEquals("value-259", all.get(259));

        assertEquals(260, uniques.size());
        assertFalse(uniques.contains(null));
    }

    @Test
    void objectViewsShouldGetAllAndUniquesInViewOrder()
    {
        final ColumnName S = ColumnName.of("S");
        ColumnObject.Builder<String> builder = ColumnObject.builder(S, ColumnTypes.STRING);
        for (int i = 0; i < 260; ++i)
        {
            if (i == 50)
            {
                builder.addNull();
            }
            builder.add("value-" + i);
        }
        ColumnObject<String> column = builder.build();

        ColumnObject<String> view = column.view(ViewIndex.builder().add(51).add(50).add(10).add(51).build());

        List<String> all = (List<String>) view.getAll(new ArrayList<>());
        Set<String> uniques = view.getUniques(null);

        assertIterableEquals(List.of("value-50", "value-10", "value-50"), all);
        assertEquals(new LinkedHashSet<>(List.of("value-50", "value-10")), uniques);
    }
}
