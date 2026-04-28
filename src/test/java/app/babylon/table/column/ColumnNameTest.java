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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ColumnNameTest
{
    @Test
    public void testTypicalUseCase()
    {
        String s = "Gary Kennedy-20";
        String expected = "garykennedy20";

        String actual = ColumnName.clean(s);

        assertEquals(expected, actual, s);

        final ColumnName COL_1 = ColumnName.of(s);
        final ColumnName COL_2 = ColumnName.of(expected);

        assertEquals(COL_1, COL_2);
        assertEquals(0, COL_1.compareTo(COL_2));

    }

    @Test
    public void testSnake()
    {
        String s = "GaryKennedy20";
        String expected = "gary_kennedy_20";

        final ColumnName CN = ColumnName.of(s);
        assertEquals(expected, CN.toSnake());

    }

    @Test
    public void testCamelUpper()
    {
        String s = "garyKennedy20";
        String expected = "GaryKennedy20";

        final ColumnName CN = ColumnName.of(s);
        assertEquals(expected, CN.getValue());

    }

    @Test
    public void testSplitToWords()
    {
        String s = "garyKennedy20";

        final ColumnName CN = ColumnName.of(s);
        Collection<String> words = CN.toWords(null);

        assertTrue(words.contains("Gary"));
        assertTrue(words.contains("Kennedy20"));

    }

    @Test
    public void testCleanupForSeparatorAndCaseVariants()
    {
        final ColumnName TRADE_DATE = ColumnName.of("trade-date");
        final ColumnName TRADE_DATE_2 = ColumnName.of("TRADE_DATE");
        final ColumnName TRADE_DATE_3 = ColumnName.of("trade date");
        final ColumnName TRADE_DATE_4 = ColumnName.of("trade.date");

        assertEquals("TradeDate", TRADE_DATE.getValue());
        assertEquals("TradeDate", TRADE_DATE_2.getValue());
        assertEquals("TradeDate", TRADE_DATE_3.getValue());
        assertEquals("TradeDate", TRADE_DATE_4.getValue());

        assertEquals(TRADE_DATE, TRADE_DATE_2);
        assertEquals(TRADE_DATE, TRADE_DATE_3);
        assertEquals(TRADE_DATE, TRADE_DATE_4);
        assertEquals("tradedate", TRADE_DATE.getCanonical());
    }

    @Test
    public void testLeadingAndTrailingSpacesAreIgnored()
    {
        final ColumnName TRADE_DATE = ColumnName.of("  Trade Date  ");
        final ColumnName TRADE_DATE_2 = ColumnName.of("Trade Date");

        assertEquals("TradeDate", TRADE_DATE.getValue());
        assertEquals(TRADE_DATE, TRADE_DATE_2);
        assertEquals("tradedate", TRADE_DATE.getCanonical());
    }

    @Test
    public void testNonLatinLettersAreRetainedInCanonical()
    {
        final ColumnName COLUMN_2026 = ColumnName.of("Журнал 2026");
        assertEquals("журнал2026", COLUMN_2026.getCanonical());

        final ColumnName COLUMN_7 = ColumnName.of("Δοκιμή 7");
        assertEquals("δοκιμη7", COLUMN_7.getCanonical());
    }

    @Test
    public void testLatinDiacriticsAreRemoved()
    {
        final ColumnName CAF = ColumnName.of("Café");
        assertEquals("cafe", CAF.getCanonical());
        assertEquals("Cafe", CAF.getValue());

        final ColumnName NGSTR_M = ColumnName.of("Ångström");
        assertEquals("angstrom", NGSTR_M.getCanonical());
        assertEquals("Angstrom", NGSTR_M.getValue());
    }

    @Test
    public void testSeparatorVariantsNormalizeToSameName()
    {
        String[] variants = new String[]
        {"trade date", "trade   date", "trade\tdate", "trade-date", "trade.date", "trade,date", "trade/date",
                "trade\\date", "trade:date", "trade;date", "trade@date", "trade#date", "trade%date", "trade&date",
                "trade*date", "trade(date)", "trade[date]", "trade{date}", "trade\"date\"", "trade'date'",
                "  trade date  "};

        for (String variant : variants)
        {
            final ColumnName CN = ColumnName.of(variant);
            assertEquals("TradeDate", CN.getValue(), variant);
            assertEquals("tradedate", CN.getCanonical(), variant);
        }
    }

    @Test
    public void testParseFactoriesAndArrayFactories()
    {
        assertNull(ColumnName.parse(null));
        assertNull(ColumnName.parse(""));

        ColumnName parsed = ColumnName.parse("trade date");
        assertEquals("TradeDate", parsed.getValue());
        assertNull(ColumnName.parse("xx   yy", 2, 3));
        assertEquals("SettleDate", ColumnName.parse("xx settle date yy", 2, 13).getValue());
        assertEquals("Cafe", ColumnName.of("xx Caf\u00E9 yy", 2, 5).getValue());

        ColumnName[] fromCollection = ColumnName.of(List.of("Trade Date", "Settle Date"));
        assertEquals(2, fromCollection.length);
        assertEquals("TradeDate", fromCollection[0].getValue());
        assertEquals("SettleDate", fromCollection[1].getValue());

        ColumnName[] fromArray = ColumnName.of(new String[]
        {"Skip", "Trade Date", "Settle Date"}, 1);
        assertEquals(2, fromArray.length);
        assertEquals("TradeDate", fromArray[0].getValue());
        assertEquals("SettleDate", fromArray[1].getValue());

        assertEquals(0, ColumnName.of(new String[0]).length);
        assertEquals(0, ColumnName.of(new String[]
        {"Only"}, 1).length);
    }

    @Test
    public void testStringAndHashRepresentCanonicalIdentity()
    {
        final ColumnName left = ColumnName.of("trade-date");
        final ColumnName right = ColumnName.of("Trade Date");

        assertEquals("TradeDate", left.toString());
        assertEquals(left.hashCode(), right.hashCode());
        assertFalse(left.equals("TradeDate"));
        assertSame(left, left);
    }

    @Test
    public void testSqlAndCamelCaseConversions()
    {
        final ColumnName cn = ColumnName.of("trade date 20");

        assertEquals("TradeDate20", cn.toCamelCaseUpper());
        assertEquals("tradeDate20", cn.toCamelCase());
        assertEquals("trade_date_20", cn.toSnake());
        assertEquals("`trade_date_20`", cn.toSqlIdentifier());
    }

    @Test
    public void testToWordsReuseProvidedCollection()
    {
        final ColumnName cn = ColumnName.of("tradeDate20");
        Collection<String> words = new ArrayList<>();

        Collection<String> returned = cn.toWords(words);

        assertSame(words, returned);
        assertEquals(List.of("Trade", "Date20"), new ArrayList<>(returned));
    }
}
