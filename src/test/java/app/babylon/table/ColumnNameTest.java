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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

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
        
        ColumnName col1 = ColumnName.of(s);
        ColumnName col2 = ColumnName.of(expected);
        
        assertEquals(col1, col2);
        assertEquals(0, col1.compareTo(col2));        
        
    }

    @Test
    public void testSnake()
    {
        String s = "GaryKennedy20";
        String expected = "gary_kennedy_20";

        ColumnName cn = ColumnName.of(s);
        assertEquals(expected, cn.toSnake());
        
    }


    @Test
    public void testCamelUpper()
    {
        String s = "garyKennedy20";
        String expected = "GaryKennedy20";

        ColumnName cn = ColumnName.of(s);
        assertEquals(expected, cn.getValue());
        
    }
    
    @Test
    public void testSplitToWords()
    {
        String s = "garyKennedy20";
       
        ColumnName cn = ColumnName.of(s);
        Collection<String> words = cn.toWords(null);

        assertTrue(words.contains("Gary"));
        assertTrue(words.contains("Kennedy20"));
        
    }

    @Test
    public void testCleanupForSeparatorAndCaseVariants()
    {
        ColumnName a = ColumnName.of("trade-date");
        ColumnName b = ColumnName.of("TRADE_DATE");
        ColumnName c = ColumnName.of("trade date");
        ColumnName d = ColumnName.of("trade.date");

        assertEquals("TradeDate", a.getValue());
        assertEquals("TradeDate", b.getValue());
        assertEquals("TradeDate", c.getValue());
        assertEquals("TradeDate", d.getValue());

        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(a, d);
        assertEquals("tradedate", a.getCanonical());
    }

    @Test
    public void testLeadingAndTrailingSpacesAreIgnored()
    {
        ColumnName a = ColumnName.of("  Trade Date  ");
        ColumnName b = ColumnName.of("Trade Date");

        assertEquals("TradeDate", a.getValue());
        assertEquals(a, b);
        assertEquals("tradedate", a.getCanonical());
    }

    @Test
    public void testNonLatinLettersAreRetainedInCanonical()
    {
        ColumnName cyrillic = ColumnName.of("Журнал 2026");
        assertEquals("журнал2026", cyrillic.getCanonical());

        ColumnName greek = ColumnName.of("Δοκιμή 7");
        assertEquals("δοκιμη7", greek.getCanonical());
    }

    @Test
    public void testLatinDiacriticsAreRemoved()
    {
        ColumnName cafe = ColumnName.of("Café");
        assertEquals("cafe", cafe.getCanonical());
        assertEquals("Cafe", cafe.getValue());

        ColumnName angstrom = ColumnName.of("Ångström");
        assertEquals("angstrom", angstrom.getCanonical());
        assertEquals("Angstrom", angstrom.getValue());
    }

    @Test
    public void testSeparatorVariantsNormalizeToSameName()
    {
        String[] variants = new String[]
        {
            "trade date",
            "trade   date",
            "trade\tdate",
            "trade-date",
            "trade.date",
            "trade,date",
            "trade/date",
            "trade\\date",
            "trade:date",
            "trade;date",
            "trade@date",
            "trade#date",
            "trade%date",
            "trade&date",
            "trade*date",
            "trade(date)",
            "trade[date]",
            "trade{date}",
            "trade\"date\"",
            "trade'date'",
            "  trade date  "
        };

        for (String variant : variants)
        {
            ColumnName cn = ColumnName.of(variant);
            assertEquals("TradeDate", cn.getValue(), variant);
            assertEquals("tradedate", cn.getCanonical(), variant);
        }
    }
}
