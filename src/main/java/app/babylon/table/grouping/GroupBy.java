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

import java.util.HashMap;
import java.util.Map;

import app.babylon.table.TableColumnar;

public class GroupBy
{
    private final Map<GroupKey, TableColumnar> groupedTables;

    public GroupBy(Map<GroupKey, TableColumnar> groupedTables)
    {
        this.groupedTables = new HashMap<>(groupedTables);

    }

    public Map<GroupKey, TableColumnar> getGroupedTables(Map<GroupKey, TableColumnar> x)
    {
        if (x == null)
        {
            x = new HashMap<>();
        }
        x.putAll(this.groupedTables);
        return x;
    }
}
