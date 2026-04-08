/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.grouping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class GroupKeyTest
{
    @Test
    public void test1()
    {
        String account = "HLGIA";
        LocalDate tradeDate = LocalDate.of(2024, 8, 20);

        GroupKey key1 = GroupKey.of(account, tradeDate);
        GroupKey key2 = GroupKey.of(account, tradeDate);
        GroupKey key3 = GroupKey.of(account, tradeDate, account);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1.hashCode(), key3.hashCode());
        assertNotEquals(key1, key3);
    }
}
