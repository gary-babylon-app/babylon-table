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
        assertTrue(Double.isNaN(accumulator.getVariance()));
        assertTrue(Double.isNaN(accumulator.getVarianceSample()));
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
        assertEquals(200.0d / 3.0d, accumulator.getVariance(), 1e-12);
        assertEquals(100.0d, accumulator.getVarianceSample(), 1e-12);
    }

    @Test
    void shouldAccumulateFromCharArray()
    {
        AccumulatorDouble accumulator = new AccumulatorDouble();
        String chars = "xx12.5yy";

        accumulator.accept(chars, 2, 4);

        assertEquals(1L, accumulator.getCount());
        assertEquals(12.5d, accumulator.getMin());
        assertEquals(12.5d, accumulator.getMax());
        assertEquals(12.5d, accumulator.getSum());
        assertEquals(12.5d, accumulator.getMean());
    }

    @Test
    void shouldExposeAggregateValues()
    {
        AccumulatorDouble accumulator = new AccumulatorDouble();

        accumulator.accept(10.0d);
        accumulator.accept(20.0d);

        assertEquals(10.0d, accumulator.get(Aggregate.MIN));
        assertEquals(20.0d, accumulator.get(Aggregate.MAX));
        assertEquals(30.0d, accumulator.get(Aggregate.SUM));
        assertEquals(15.0d, accumulator.get(Aggregate.MEAN));
        assertEquals(2.0d, accumulator.get(Aggregate.COUNT));
        assertEquals(25.0d, accumulator.get(Aggregate.VARIANCE));
        assertEquals(50.0d, accumulator.get(Aggregate.VARIANCE_SAMPLE));
    }
}
