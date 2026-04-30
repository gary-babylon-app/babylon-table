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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class SplitTest
{
    @Test
    public void characterShouldSplitOnConfiguredSeparator()
    {
        List<String> actual = Split.character("alpha,beta,gamma", ',');

        assertEquals(List.of("alpha", "beta", "gamma"), actual);
    }

    @Test
    public void characterShouldRemoveEmptyValues()
    {
        List<String> actual = Split.character("alpha beta  gamma", ' ');

        assertEquals(List.of("alpha", "beta", "gamma"), actual);
    }

    @Test
    public void literalShouldPreserveTrailingEmptyValuesWhenRequested()
    {
        String[] actual = Split.literal("a,,b,", ",", true);

        assertArrayEquals(new String[]
        {"a", "", "b", ""}, actual);
    }

    @Test
    public void literalShouldDropTrailingEmptyValuesWhenNotRequested()
    {
        String[] actual = Split.literal("a,,b,", ",", false);

        assertArrayEquals(new String[]
        {"a", "", "b"}, actual);
    }

    @Test
    public void whitespaceShouldSplitOnRunsOfWhitespace()
    {
        String[] actual = Split.whitespace("  alpha\tbeta \n gamma  ");

        assertArrayEquals(new String[]
        {"alpha", "beta", "gamma"}, actual);
    }
}
