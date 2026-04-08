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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SentenceTest
{
    @Test
    public void onlyOneInShouldReturnOnlyMatch()
    {
        Integer actual = Sentence.onlyOneIn(this::parseInteger, "abc 123 xyz");
        assertEquals(123, actual);
    }

    @Test
    public void onlyOneInShouldReturnNullWhenNoMatch()
    {
        Integer actual = Sentence.onlyOneIn(this::parseInteger, "abc xyz");
        assertNull(actual);
    }

    @Test
    public void onlyOneInShouldReturnNullWhenMultipleMatches()
    {
        Integer actual = Sentence.onlyOneIn(this::parseInteger, "123 abc 456");
        assertNull(actual);
    }

    @Test
    public void onlyInShouldDelegateToOnlyOneIn()
    {
        Integer actual = Sentence.onlyIn(this::parseInteger, "abc 123 xyz");
        assertEquals(123, actual);
    }

    private Integer parseInteger(CharSequence s)
    {
        try
        {
            return Integer.valueOf(s.toString());
        } catch (NumberFormatException e)
        {
            return null;
        }
    }
}
