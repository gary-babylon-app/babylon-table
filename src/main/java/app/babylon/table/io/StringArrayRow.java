/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import java.util.Arrays;
import java.util.Objects;

import app.babylon.table.column.Column;
import app.babylon.text.Strings;

public final class StringArrayRow implements RowValues
{
    private final String[] values;

    public StringArrayRow(String[] values)
    {
        this.values = Arrays.copyOf(Objects.requireNonNull(values, "values"), values.length);
    }

    private StringArrayRow(StringArrayRow source, int[] selectedIndexes, boolean strip)
    {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(selectedIndexes, "selectedIndexes");
        this.values = new String[selectedIndexes.length];
        for (int i = 0; i < selectedIndexes.length; ++i)
        {
            String value = source.getString(selectedIndexes[i]);
            this.values[i] = strip ? stripped(value) : value;
        }
    }

    @Override
    public int size()
    {
        return this.values.length;
    }

    @Override
    public boolean isEmpty()
    {
        for (String value : this.values)
        {
            if (!Strings.isEmpty(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSet(int fieldIndex)
    {
        return !Strings.isEmpty(getString(fieldIndex));
    }

    @Override
    public String getString(int fieldIndex)
    {
        return this.values[fieldIndex];
    }

    @Override
    public StringArrayRow select(int[] selectedIndexes, boolean strip)
    {
        return new StringArrayRow(this, selectedIndexes, strip);
    }

    @Override
    public void addTo(Column.Builder builder, int fieldIndex)
    {
        String value = getString(fieldIndex);
        if (Strings.isEmpty(value))
        {
            builder.addNull();
            return;
        }
        builder.add(value, 0, value.length());
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(this.values);
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj || obj instanceof StringArrayRow other && Arrays.equals(this.values, other.values);
    }

    private static String stripped(String value)
    {
        if (Strings.isEmpty(value))
        {
            return null;
        }
        CharSequence stripped = Strings.stripx(value);
        return Strings.isEmpty(stripped) ? null : stripped.toString();
    }
}
