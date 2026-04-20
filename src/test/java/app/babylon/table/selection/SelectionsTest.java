/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class SelectionsTest
{
    @Test
    public void selectShouldKeepRowsMatchingPattern()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");
        names.add("Bob");
        names.add("");
        names.addNull();
        names.add("Amy");

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), names.build());

        TableColumnar selected = Selections.select(table, NAME, Pattern.compile("^A"));

        assertEquals(2, selected.getRowCount());
        assertEquals("Alice", selected.getString(NAME).get(0));
        assertEquals("Amy", selected.getString(NAME).get(1));
    }

    @Test
    public void eqShouldMatchOnlyEqualNonNullValues()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");
        names.addNull();
        names.add("Bob");
        names.add("Alice");

        Selection selection = Selections.eq(names.build(), "Alice");

        assertEquals(4, selection.size());
        assertTrue(selection.get(0));
        assertFalse(selection.get(1));
        assertFalse(selection.get(2));
        assertTrue(selection.get(3));
        assertEquals(2, selection.selected());
    }

    @Test
    public void neShouldMatchOnlyDifferentNonNullValues()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");
        names.addNull();
        names.add("Bob");
        names.add("Alice");

        Selection selection = Selections.ne(names.build(), "Alice");

        assertEquals(4, selection.size());
        assertFalse(selection.get(0));
        assertFalse(selection.get(1));
        assertTrue(selection.get(2));
        assertFalse(selection.get(3));
        assertEquals(1, selection.selected());
    }

    @Test
    public void inShouldMatchAnyOfTheProvidedValues()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");
        names.addNull();
        names.add("Bob");
        names.add("Cara");

        Selection selection = Selections.in(names.build(), new String[]
        {"Alice", "Cara"});

        assertEquals(4, selection.size());
        assertTrue(selection.get(0));
        assertFalse(selection.get(1));
        assertFalse(selection.get(2));
        assertTrue(selection.get(3));
        assertEquals(2, selection.selected());
    }

    @Test
    public void ninShouldRejectProvidedValuesAndTreatNullAsSelected()
    {
        final ColumnName NAME = ColumnName.of("Name");
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");
        names.addNull();
        names.add("Bob");
        names.add("Cara");

        Selection selection = Selections.nin(names.build(), new String[]
        {"Alice", "Cara"});

        assertEquals(4, selection.size());
        assertFalse(selection.get(0));
        assertTrue(selection.get(1));
        assertTrue(selection.get(2));
        assertFalse(selection.get(3));
        assertEquals(2, selection.selected());
    }

    @Test
    public void methodsShouldRejectNullArguments()
    {
        final ColumnName NAME = ColumnName.of("Name");
        assertThrows(RuntimeException.class, () -> Selections.eq(null, "x"));
        assertThrows(RuntimeException.class, () -> Selections.ne(null, "x"));
        assertThrows(RuntimeException.class, () -> Selections.in(null, new String[]
        {"x"}));
        assertThrows(RuntimeException.class, () -> Selections.nin(null, new String[]
        {"x"}));

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("Alice");

        assertThrows(RuntimeException.class, () -> Selections.in(names.build(), null));
        assertThrows(RuntimeException.class, () -> Selections.nin(names.build(), null));
    }
}
