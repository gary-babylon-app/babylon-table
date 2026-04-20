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

/**
 * Supported aggregate calculations.
 */
public enum Aggregate
{
    /** Number of set values. */
    COUNT,
    /** Sum of set values. */
    SUM,
    /** Maximum set value. */
    MAX,
    /** Minimum set value. */
    MIN,
    /** Arithmetic mean of set values. */
    MEAN,
    /** Population variance of set values. */
    VARIANCE,
    /** Sample variance of set values. */
    VARIANCE_SAMPLE;
}
