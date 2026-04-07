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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class StringsTest
{
    @Test
    public void leftPadShouldMatchExpectedBehavior()
    {
        assertNull(Strings.leftPad(null, 5, 'x'));
        assertEquals("bat", Strings.leftPad("bat", 1, 'x'));
        assertEquals("bat", Strings.leftPad("bat", 3, 'x'));
        assertEquals("xxbat", Strings.leftPad("bat", 5, 'x'));
        assertEquals("xxx", Strings.leftPad("", 3, 'x'));
    }

    @Test
    public void rightPadShouldMatchExpectedBehavior()
    {
        assertNull(Strings.rightPad(null, 5, 'x'));
        assertEquals("bat", Strings.rightPad("bat", 1, 'x'));
        assertEquals("bat", Strings.rightPad("bat", 3, 'x'));
        assertEquals("batxx", Strings.rightPad("bat", 5, 'x'));
        assertEquals("xxx", Strings.rightPad("", 3, 'x'));
    }

    @Test
    public void toCamelUpperShouldNormalizeCommonSeparatorsAndCase()
    {
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade-date"));
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("TRADE_DATE"));
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade date"));
        assertEquals("TradeDate", Strings.toCamelUpperPreserve("trade.date"));
    }
}
