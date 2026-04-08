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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ColumnObjectGetAllAndUniquesTest
{
    @Test
    void objectColumnGetAllAndUniquesShouldSkipNullsWithoutBecomingCategorical()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("s"), String.class);
        for (int i = 0; i < 260; ++i)
        {
            if (i == 120)
            {
                builder.addNull();
            }
            builder.add("value-" + i);
        }

        ColumnObject<String> column = builder.build();

        assertFalse(column instanceof ColumnCategorical<?>);

        List<String> all = (List<String>) column.getAll(new ArrayList<>());
        Set<String> uniques = column.getUniques(null);

        assertEquals(260, all.size());
        assertFalse(all.contains(null));
        assertEquals("value-0", all.get(0));
        assertEquals("value-119", all.get(119));
        assertEquals("value-120", all.get(120));
        assertEquals("value-259", all.get(259));

        assertEquals(260, uniques.size());
        assertFalse(uniques.contains(null));
    }

    @Test
    void objectViewGetAllAndUniquesShouldRespectViewOrderAndSkipNulls()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("s"), String.class);
        for (int i = 0; i < 260; ++i)
        {
            if (i == 50)
            {
                builder.addNull();
            }
            builder.add("value-" + i);
        }
        ColumnObject<String> column = builder.build();

        ViewIndex rowIndex = ViewIndex.builder().add(51).add(50).add(10).add(51).build();
        ColumnObject<String> view = column.view(rowIndex);

        List<String> all = (List<String>) view.getAll(new ArrayList<>());
        Set<String> uniques = view.getUniques(null);

        assertIterableEquals(List.of("value-50", "value-10", "value-50"), all);
        assertEquals(new LinkedHashSet<>(List.of("value-50", "value-10")), uniques);
    }

    @Test
    void categoricalGetAllShouldKeepDuplicatesButUniquesShouldUseCategories()
    {
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("s"), String.class);
        builder.add("A");
        builder.addNull();
        builder.add("B");
        builder.add("A");
        ColumnCategorical<String> column = builder.build();

        List<String> all = (List<String>) column.getAll(new ArrayList<>());
        Set<String> uniques = column.getUniques(null);

        assertIterableEquals(List.of("A", "B", "A"), all);
        assertEquals(Set.of("A", "B"), uniques);
        assertFalse(uniques.contains(null));
    }

    @Test
    void categoricalConstantShouldReturnRepeatedValuesForGetAllAndSingleValueForUniques()
    {
        ColumnCategorical<String> constant = ColumnCategorical.constant(ColumnName.of("s"), "X", 4, String.class);

        List<String> all = (List<String>) constant.getAll(new ArrayList<>());
        Set<String> uniques = constant.getUniques(null);

        assertIterableEquals(List.of("X", "X", "X", "X"), all);
        assertEquals(Set.of("X"), uniques);
    }

    @Test
    void categoricalViewUniquesShouldOnlyContainCategoriesUsedByTheView()
    {
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("s"), String.class);
        builder.add("A");
        builder.add("B");
        builder.add("C");
        builder.addNull();
        builder.add("A");
        ColumnCategorical<String> original = builder.build();

        ViewIndex rowIndex = ViewIndex.builder().add(2).add(3).add(4).build();
        ColumnCategorical<String> view = original.view(rowIndex);

        List<String> all = (List<String>) view.getAll(new ArrayList<>());
        Set<String> uniques = view.getUniques(null);

        assertIterableEquals(List.of("C", "A"), all);
        assertEquals(Set.of("A", "C"), uniques);
        assertTrue(uniques.contains("A"));
        assertTrue(uniques.contains("C"));
        assertFalse(uniques.contains("B"));
        assertFalse(uniques.contains(null));
    }
}
