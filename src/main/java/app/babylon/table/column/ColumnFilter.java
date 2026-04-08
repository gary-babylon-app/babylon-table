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

import app.babylon.table.TableColumnar;
import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Defines a reusable row filter that binds to a table and evaluates rows by
 * index.
 */
@FunctionalInterface
public interface ColumnFilter
{
    IntPredicate bind(TableColumnar table);

    default ColumnFilter and(ColumnFilter other)
    {
        ColumnFilter right = Objects.requireNonNull(other);
        return table -> {
            IntPredicate leftPredicate = this.bind(table);
            IntPredicate rightPredicate = right.bind(table);
            return i -> leftPredicate.test(i) && rightPredicate.test(i);
        };
    }

    default ColumnFilter or(ColumnFilter other)
    {
        ColumnFilter right = Objects.requireNonNull(other);
        return table -> {
            IntPredicate leftPredicate = this.bind(table);
            IntPredicate rightPredicate = right.bind(table);
            return i -> leftPredicate.test(i) || rightPredicate.test(i);
        };
    }

    default ColumnFilter not()
    {
        return table -> {
            IntPredicate predicate = this.bind(table);
            return i -> !predicate.test(i);
        };
    }

    static ColumnFilter of(ColumnName columnName, Predicate<Object> predicate)
    {
        ColumnName name = Objects.requireNonNull(columnName);
        Predicate<Object> p = Objects.requireNonNull(predicate);
        return table -> {
            Column column = table.get(name);
            if (!(column instanceof ColumnObject<?> co))
            {
                throw new IllegalArgumentException("filter requires an object-backed column: " + name);
            }
            IntPredicate predicateByRow = i -> co.isSet(i) && p.test(co.get(i));
            return predicateByRow;
        };
    }

    static ColumnFilter of(ColumnName columnName, IntPredicate predicate)
    {
        ColumnName name = Objects.requireNonNull(columnName);
        IntPredicate p = Objects.requireNonNull(predicate);
        return table -> {
            ColumnInt column = table.getInt(name);
            if (column == null)
            {
                throw new IllegalArgumentException("No int column " + name + " found");
            }
            IntPredicate predicateByRow = i -> column.isSet(i) && p.test(column.get(i));
            return predicateByRow;
        };
    }

    static ColumnFilter of(ColumnName columnName, LongPredicate predicate)
    {
        ColumnName name = Objects.requireNonNull(columnName);
        LongPredicate p = Objects.requireNonNull(predicate);
        return table -> {
            ColumnLong column = table.getLong(name);
            if (column == null)
            {
                throw new IllegalArgumentException("No long column " + name + " found");
            }
            IntPredicate predicateByRow = i -> column.isSet(i) && p.test(column.get(i));
            return predicateByRow;
        };
    }

    static ColumnFilter of(ColumnName columnName, DoublePredicate predicate)
    {
        ColumnName name = Objects.requireNonNull(columnName);
        DoublePredicate p = Objects.requireNonNull(predicate);
        return table -> {
            ColumnDouble column = table.getDouble(name);
            if (column == null)
            {
                throw new IllegalArgumentException("No double column " + name + " found");
            }
            IntPredicate predicateByRow = i -> column.isSet(i) && p.test(column.get(i));
            return predicateByRow;
        };
    }
}
