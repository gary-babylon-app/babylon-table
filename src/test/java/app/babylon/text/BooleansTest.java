/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BooleansTest
{
    @Test
    void shouldRecogniseWholeBooleanValues()
    {
        assertTrue(Booleans.isBooleanTrue("true"));
        assertTrue(Booleans.isBooleanTrue("TRUE"));
        assertTrue(Booleans.isBooleanTrue("T"));
        assertTrue(Booleans.isBooleanTrue("t"));
        assertTrue(Booleans.isBooleanTrue("1"));

        assertTrue(Booleans.isBooleanFalse("false"));
        assertTrue(Booleans.isBooleanFalse("FALSE"));
        assertTrue(Booleans.isBooleanFalse("F"));
        assertTrue(Booleans.isBooleanFalse("f"));
        assertTrue(Booleans.isBooleanFalse("0"));
    }

    @Test
    void shouldRecogniseBooleanSlices()
    {
        assertTrue(Booleans.isBooleanTrue("xxtrueyy", 2, 4));
        assertTrue(Booleans.isBooleanTrue("xxTyy", 2, 1));
        assertTrue(Booleans.isBooleanFalse("xxfalseyy", 2, 5));
        assertTrue(Booleans.isBooleanFalse("xx0yy", 2, 1));
    }

    @Test
    void shouldRejectUnmatchedValues()
    {
        assertFalse(Booleans.isBooleanTrue(null));
        assertFalse(Booleans.isBooleanFalse(null));
        assertFalse(Booleans.isBooleanTrue("truth"));
        assertFalse(Booleans.isBooleanFalse("no"));
        assertEquals(Booleans.UNPARSED, Booleans.parseBooleanValue("xxbadyy", 2, 3));
    }
}
