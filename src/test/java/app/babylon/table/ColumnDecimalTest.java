/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class ColumnDecimalTest
{
    @Test
    public void testEquals()
    {
        ColumnName cn = ColumnName.of("test");
        ColumnObject.Builder<BigDecimal> cd1Builder = ColumnObject.builderDecimal(cn);
        cd1Builder.add(BigDecimals.parse("0.01"));
        cd1Builder.add(BigDecimals.parse("0.02"));
        ColumnObject<BigDecimal> cd1 = cd1Builder.build();

        ColumnObject.Builder<BigDecimal> cd2Builder = ColumnObject.builderDecimal(cn);
        cd2Builder.add(BigDecimals.parse("0.010"));
        cd2Builder.add(BigDecimals.parse("0.020"));
        ColumnObject<BigDecimal> cd2 = cd2Builder.build();

        assertEquals(cd1, cd1);
        assertEquals(cd1, cd2);

        ColumnObject<BigDecimal> constant = Columns.newDecimal(cn, BigDecimal.TEN, 3);
        ColumnObject<BigDecimal> constant2 = Columns.newDecimal(cn, BigDecimal.TEN, 3);

        assertEquals(constant, constant);
        assertEquals(constant, constant2);

    }
    @Test
    public void testRemoveCommas()
    {
        String s = "32,000.00";
        String expected = "32000.00";
        String actual = BigDecimals.removeCommas(s);
        assertEquals(expected, actual);

        s = "-32,000.00";
        expected = "-32000.00";
        actual = BigDecimals.removeCommas(s);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseRejectsInternalSpacesAndRepeatedPeriods()
    {
        assertNull(BigDecimals.parse("1.2 3"));
        assertNull(BigDecimals.parse("1  23"));
        assertNull(BigDecimals.parse("1..23"));
        assertNull(BigDecimals.parse("12.3.4"));
        assertNull(BigDecimals.parse(")(123"));
        assertNull(BigDecimals.parse("123%123"));
        assertNull(BigDecimals.parse("123$123"));
    }

    @Test
    public void testPrepareAndParseDoubleShareDecimalCleanup()
    {
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
    }

    @Test
    public void testExtractDoubleRequiresOnlyOneDecimalWord()
    {
        assertEquals(12.5d, BigDecimals.extractDouble("USD 12.50").doubleValue(), 1.0e-12);
        assertNull(BigDecimals.extractDouble("12.5 and 7.5"));
        assertNull(BigDecimals.prepare("1  23"));
    }
    @Test
    public void testSmallMutable()
    {
        ColumnObject.Builder<BigDecimal> cdBuilder = ColumnObject.builderDecimal(ColumnName.of("Test"));
        cdBuilder.add(BigDecimal.ZERO);
        cdBuilder.add(BigDecimal.ONE);

        ColumnObject<BigDecimal> cd = cdBuilder.build();
        String actual = cd.toString();
        String expected = "Test[0, 1, ... ,1]";
        assertEquals(expected, actual);
    }

    @Test
    public void testAggregate()
    {
        ColumnObject.Builder<BigDecimal> cdBuilder = ColumnObject.builderDecimal(ColumnName.of("Test"));
        cdBuilder.add(BigDecimal.ZERO);
        cdBuilder.add(BigDecimal.ONE);
        cdBuilder.add(BigDecimal.TEN);
        ColumnObject<BigDecimal> cd = cdBuilder.build();

        BigDecimal ELEVEN = new BigDecimal("11");
        assertTrue(ELEVEN.compareTo(Columns.aggregate(cd, Aggregate.SUM))==0);

        assertTrue(BigDecimal.ZERO.compareTo(Columns.aggregate(cd, Aggregate.MIN))==0);
        assertTrue(BigDecimal.TEN.compareTo(Columns.aggregate(cd, Aggregate.MAX))==0);
    }
}
