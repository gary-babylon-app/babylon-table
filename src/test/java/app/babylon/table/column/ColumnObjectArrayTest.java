/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ColumnObjectArrayTest
{
    @Test
    void shouldSortAndCreateView()
    {
        final ColumnName CITY = ColumnName.of("CITY");
        final ColumnName SORTED_CITY = ColumnName.of("SORTED_CITY");
        ColumnObject.Builder<String> builder = ColumnObject.builder(CITY, ColumnTypes.STRING, ColumnObject.Mode.ARRAY);
        builder.add("Zurich");
        builder.addNull();
        builder.add("Amsterdam");
        builder.add("London");
        ColumnObject<String> view = (ColumnObject<String>) Columns.sort(builder.build());
        ColumnObject<String> copy = (ColumnObject<String>) view.copy(SORTED_CITY);

        assertFalse(view.isSet(0));
        assertEquals("Amsterdam", view.get(1));
        assertEquals("London", view.get(2));
        assertEquals("Zurich", view.get(3));
        assertEquals(-1, view.compare(0, 1));
        assertEquals(0, view.compare(2, 2));
        assertEquals("CITY[, Amsterdam, ... ,Zurich]", view.toString());

        assertEquals(SORTED_CITY, copy.getName());
        assertFalse(copy.isSet(0));
        assertEquals("Amsterdam", copy.get(1));
        assertEquals("London", copy.get(2));
        assertEquals("Zurich", copy.get(3));
    }

    @Test
    void shouldCopyArrayAndExtractSingleValueColumn()
    {
        final ColumnName CITY = ColumnName.of("CITY");
        final ColumnName CITY_COPY = ColumnName.of("CITY_COPY");
        ColumnObject.Builder<String> builder = ColumnObject.builder(CITY, ColumnTypes.STRING, ColumnObject.Mode.ARRAY);
        builder.add("Zurich");
        builder.addNull();
        builder.add("Amsterdam");
        ColumnObject<String> column = builder.build();

        ColumnObject<String> copy = (ColumnObject<String>) column.copy(CITY_COPY);
        ColumnObject<String> single = (ColumnObject<String>) column.getAsColumn(2);

        assertEquals(CITY_COPY, copy.getName());
        assertEquals("Zurich", copy.get(0));
        assertFalse(copy.isSet(1));
        assertEquals("Amsterdam", copy.get(2));

        assertEquals(CITY, single.getName());
        assertEquals(1, single.size());
        assertEquals("Amsterdam", single.get(0));
    }
}
