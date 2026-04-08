/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccumulatorDoubleTest
{
    @Test
    void shouldUseNanForEmptyMinMaxAndMean()
    {
        AccumulatorDouble accumulator = new AccumulatorDouble();

        assertEquals(0L, accumulator.getCount());
        assertEquals(0.0d, accumulator.getSum());
        assertTrue(Double.isNaN(accumulator.getMin()));
        assertTrue(Double.isNaN(accumulator.getMax()));
        assertTrue(Double.isNaN(accumulator.getMean()));
    }

    @Test
    void shouldAccumulateFromDoubles()
    {
        AccumulatorDouble accumulator = new AccumulatorDouble();

        accumulator.accept(10.0d);
        accumulator.accept(20.0d);
        accumulator.accept(30.0d);

        assertEquals(3L, accumulator.getCount());
        assertEquals(10.0d, accumulator.getMin());
        assertEquals(30.0d, accumulator.getMax());
        assertEquals(60.0d, accumulator.getSum());
        assertEquals(20.0d, accumulator.getMean());
    }

    @Test
    void shouldAccumulateFromCharArray()
    {
        AccumulatorDouble accumulator = new AccumulatorDouble();
        char[] chars = "xx12.5yy".toCharArray();

        accumulator.accept(chars, 2, 4);

        assertEquals(1L, accumulator.getCount());
        assertEquals(12.5d, accumulator.getMin());
        assertEquals(12.5d, accumulator.getMax());
        assertEquals(12.5d, accumulator.getSum());
        assertEquals(12.5d, accumulator.getMean());
    }
}
