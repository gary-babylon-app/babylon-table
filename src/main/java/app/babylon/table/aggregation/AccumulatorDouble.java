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

import java.util.function.DoubleConsumer;

/**
 * Incrementally accumulates summary statistics for double values.
 */
public class AccumulatorDouble implements DoubleConsumer
{
    private long count;
    private double min;
    private double max;
    private double sum;
    private double mean;
    private double m2;

    /**
     * Creates an empty accumulator.
     */
    public AccumulatorDouble()
    {
        this.count = 0L;
        this.min = Double.NaN;
        this.max = Double.NaN;
        this.sum = 0.0d;
        this.mean = Double.NaN;
        this.m2 = 0.0d;
    }

    @Override
    /**
     * Adds a value to the running aggregate state.
     *
     * @param value
     *            value to accumulate
     */
    public void accept(double value)
    {
        if (this.count == 0L)
        {
            this.count = 1L;
            this.min = value;
            this.max = value;
            this.sum = value;
            this.mean = value;
            this.m2 = 0.0d;
            return;
        }

        ++this.count;
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
        this.sum += value;

        double delta = value - this.mean;
        this.mean += delta / this.count;
        double delta2 = value - this.mean;
        this.m2 += delta * delta2;
    }

    /**
     * Parses and accumulates a double from a character slice.
     *
     * @param chars
     *            source characters
     * @param start
     *            inclusive slice start
     * @param length
     *            slice length
     */
    public void accept(CharSequence chars, int start, int length)
    {
        if (chars == null)
        {
            throw new IllegalArgumentException("chars must not be null");
        }
        accept(Double.parseDouble(chars.subSequence(start, start + length).toString()));
    }

    /**
     * Returns the number of accumulated values.
     *
     * @return accumulated count
     */
    public long getCount()
    {
        return this.count;
    }

    /**
     * Returns the minimum accumulated value.
     *
     * @return minimum value
     */
    public double getMin()
    {
        return this.min;
    }

    /**
     * Returns the maximum accumulated value.
     *
     * @return maximum value
     */
    public double getMax()
    {
        return this.max;
    }

    /**
     * Returns the sum of accumulated values.
     *
     * @return sum of values
     */
    public double getSum()
    {
        return this.sum;
    }

    /**
     * Returns the arithmetic mean of accumulated values.
     *
     * @return mean value
     */
    public double getMean()
    {
        return this.mean;
    }

    /**
     * Returns the population variance of accumulated values.
     *
     * @return population variance
     */
    public double getVariance()
    {
        if (this.count == 0L)
        {
            return Double.NaN;
        }
        return this.m2 / this.count;
    }

    /**
     * Returns the sample variance of accumulated values.
     *
     * @return sample variance
     */
    public double getVarianceSample()
    {
        if (this.count <= 1L)
        {
            return Double.NaN;
        }
        return this.m2 / (this.count - 1L);
    }

    /**
     * Returns the value of the requested aggregate.
     *
     * @param aggregate
     *            aggregate to return
     * @return aggregate value
     */
    public double get(Aggregate aggregate)
    {
        return switch (aggregate)
        {
            case COUNT -> getCount();
            case MIN -> getMin();
            case MAX -> getMax();
            case SUM -> getSum();
            case MEAN -> getMean();
            case VARIANCE -> getVariance();
            case VARIANCE_SAMPLE -> getVarianceSample();
        };
    }
}
