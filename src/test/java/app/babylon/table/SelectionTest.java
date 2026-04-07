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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SelectionTest
{
    @Test
    public void not_flipsBitsWithinSelectionSize()
    {
        Selection selection = new Selection("s");
        selection.add(true);
        selection.add(false);
        selection.add(true);

        Selection actual = selection.not();

        assertEquals(3, actual.size());
        assertFalse(actual.get(0));
        assertTrue(actual.get(1));
        assertFalse(actual.get(2));
        assertEquals(1, actual.selected());
    }

    @Test
    public void and_returnsIntersection()
    {
        Selection left = new Selection("left");
        left.add(true);
        left.add(false);
        left.add(true);

        Selection right = new Selection("right");
        right.add(true);
        right.add(true);
        right.add(false);

        Selection actual = left.and(right);

        assertEquals(3, actual.size());
        assertEquals("(left AND right)", actual.getName());
        assertTrue(actual.get(0));
        assertFalse(actual.get(1));
        assertFalse(actual.get(2));
        assertEquals(1, actual.selected());
    }

    @Test
    public void or_returnsUnion()
    {
        Selection left = new Selection("left");
        left.add(true);
        left.add(false);
        left.add(true);

        Selection right = new Selection("right");
        right.add(false);
        right.add(true);
        right.add(false);

        Selection actual = left.or(right);

        assertEquals(3, actual.size());
        assertEquals("(left OR right)", actual.getName());
        assertTrue(actual.get(0));
        assertTrue(actual.get(1));
        assertTrue(actual.get(2));
        assertEquals(3, actual.selected());
    }

    @Test
    public void and_throwsWhenSizesDiffer()
    {
        Selection left = new Selection("left");
        left.add(true);
        left.add(false);

        Selection right = new Selection("right");
        right.add(true);

        assertThrows(IllegalArgumentException.class, () -> left.and(right));
    }

    @Test
    public void or_throwsWhenArgumentIsNull()
    {
        Selection left = new Selection("left");
        left.add(true);

        assertThrows(RuntimeException.class, () -> left.or(null));
    }
}
