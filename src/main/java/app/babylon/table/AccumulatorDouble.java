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

import java.util.function.DoubleConsumer;

public class AccumulatorDouble implements DoubleConsumer
{
    private long count;
    private double min;
    private double max;
    private double sum;
    private double mean;
    private double m2;

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

    public void accept(char[] chars, int start, int length)
    {
        if (chars == null)
        {
            throw new IllegalArgumentException("chars must not be null");
        }
        accept(Double.parseDouble(new String(chars, start, length)));
    }

    public long getCount()
    {
        return this.count;
    }

    public double getMin()
    {
        return this.min;
    }

    public double getMax()
    {
        return this.max;
    }

    public double getSum()
    {
        return this.sum;
    }

    public double getMean()
    {
        return this.mean;
    }

    public double getM2()
    {
        return this.m2;
    }
}
