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

/**
 * Grouping result keyed by group values and holding one table view per group.
 */
public class GroupBy
{
    private final Map<GroupKey, TableColumnar> groupedTables;

    /**
     * Creates a grouping result from grouped tables.
     *
     * @param groupedTables
     *            grouped table map
     */
    public GroupBy(Map<GroupKey, TableColumnar> groupedTables)
    {
        this.groupedTables = new HashMap<>(groupedTables);

    }

    /**
     * Copies the grouped tables into the supplied destination map.
     *
     * @param x
     *            destination map or {@code null}
     * @return destination map containing the grouped tables
     */
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
