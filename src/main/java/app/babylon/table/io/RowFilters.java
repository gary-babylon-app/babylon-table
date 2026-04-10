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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.ColumnName;

public final class RowFilters
{
    private RowFilters()
    {
    }

    public static RowFilter excludeEmpty(ColumnName... columnNames)
    {
        if (columnNames == null || columnNames.length == 0)
        {
            throw new IllegalArgumentException("excludeEmpty requires at least one column name.");
        }
        return excludeEmpty(new LinkedHashSet<>(Arrays.asList(columnNames)));
    }

    public static RowFilter excludeEmpty(Set<ColumnName> columnNames)
    {
        Set<ColumnName> requiredColumns = new LinkedHashSet<>(ArgumentCheck.nonNull(columnNames));
        if (requiredColumns.isEmpty())
        {
            throw new IllegalArgumentException("excludeEmpty requires at least one column name.");
        }
        return availableColumns -> {
            int[] positions = new int[requiredColumns.size()];
            int writeIndex = 0;
            for (ColumnName requiredColumn : requiredColumns)
            {
                positions[writeIndex++] = positionOf(availableColumns, requiredColumn);
            }
            return row -> {
                for (int position : positions)
                {
                    if (row.length(position) <= 0)
                    {
                        return false;
                    }
                }
                return true;
            };
        };
    }

    public static RowFilter include(Map<ColumnName, Predicate<CharSequence>> predicatesByColumn)
    {
        Map<ColumnName, Predicate<CharSequence>> predicates = Map.copyOf(ArgumentCheck.nonNull(predicatesByColumn));
        if (predicates.isEmpty())
        {
            throw new IllegalArgumentException("include requires at least one column predicate.");
        }
        return availableColumns -> {
            BoundPredicate[] boundPredicates = bindPredicates(availableColumns, predicates);
            FieldCharSequence fieldValue = new FieldCharSequence();
            return row -> {
                for (BoundPredicate boundPredicate : boundPredicates)
                {
                    fieldValue.with(row, boundPredicate.position());
                    if (!boundPredicate.predicate().test(fieldValue))
                    {
                        return false;
                    }
                }
                return true;
            };
        };
    }

    public static RowFilter exclude(Map<ColumnName, Predicate<CharSequence>> predicatesByColumn)
    {
        Map<ColumnName, Predicate<CharSequence>> predicates = Map.copyOf(ArgumentCheck.nonNull(predicatesByColumn));
        if (predicates.isEmpty())
        {
            throw new IllegalArgumentException("exclude requires at least one column predicate.");
        }
        return availableColumns -> {
            BoundPredicate[] boundPredicates = bindPredicates(availableColumns, predicates);
            FieldCharSequence fieldValue = new FieldCharSequence();
            return row -> {
                for (BoundPredicate boundPredicate : boundPredicates)
                {
                    fieldValue.with(row, boundPredicate.position());
                    if (boundPredicate.predicate().test(fieldValue))
                    {
                        return false;
                    }
                }
                return true;
            };
        };
    }

    private static BoundPredicate[] bindPredicates(ColumnName[] availableColumns,
            Map<ColumnName, Predicate<CharSequence>> predicates)
    {
        BoundPredicate[] boundPredicates = new BoundPredicate[predicates.size()];
        int writeIndex = 0;
        for (Map.Entry<ColumnName, Predicate<CharSequence>> entry : predicates.entrySet())
        {
            boundPredicates[writeIndex++] = new BoundPredicate(positionOf(availableColumns, entry.getKey()),
                    ArgumentCheck.nonNull(entry.getValue()));
        }
        return boundPredicates;
    }

    private static record BoundPredicate(int position, Predicate<CharSequence> predicate)
    {
    }

    private static final class FieldCharSequence implements CharSequence
    {
        private char[] chars;
        private int start;
        private int length;

        private FieldCharSequence with(Row row, int fieldIndex)
        {
            this.chars = row.chars();
            this.start = row.start(fieldIndex);
            this.length = row.length(fieldIndex);
            return this;
        }

        @Override
        public int length()
        {
            return this.length;
        }

        @Override
        public char charAt(int index)
        {
            if (index < 0 || index >= this.length)
            {
                throw new IndexOutOfBoundsException();
            }
            return this.chars[this.start + index];
        }

        @Override
        public CharSequence subSequence(int start, int end)
        {
            if (start < 0 || end < start || end > this.length)
            {
                throw new IndexOutOfBoundsException();
            }
            return new String(this.chars, this.start + start, end - start);
        }

        @Override
        public String toString()
        {
            return new String(this.chars, this.start, this.length);
        }
    }

    private static int positionOf(ColumnName[] availableColumns, ColumnName requiredColumn)
    {
        for (int i = 0; i < availableColumns.length; ++i)
        {
            if (requiredColumn.equals(availableColumns[i]))
            {
                return i;
            }
        }
        throw new IllegalArgumentException("Column not present for row filter: " + requiredColumn);
    }
}
