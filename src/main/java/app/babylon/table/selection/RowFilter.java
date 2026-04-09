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

import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

/**
 * Defines a reusable row filter that binds to a table and evaluates rows by
 * index.
 */
@FunctionalInterface
public interface RowFilter
{
    IntPredicate bind(TableColumnar table);

    default RowFilter and(RowFilter other)
    {
        RowFilter right = ArgumentCheck.nonNull(other);
        return table -> {
            IntPredicate leftPredicate = this.bind(table);
            IntPredicate rightPredicate = right.bind(table);
            return i -> leftPredicate.test(i) && rightPredicate.test(i);
        };
    }

    default RowFilter or(RowFilter other)
    {
        RowFilter right = ArgumentCheck.nonNull(other);
        return table -> {
            IntPredicate leftPredicate = this.bind(table);
            IntPredicate rightPredicate = right.bind(table);
            return i -> leftPredicate.test(i) || rightPredicate.test(i);
        };
    }

    default RowFilter not()
    {
        return table -> {
            IntPredicate predicate = this.bind(table);
            return i -> !predicate.test(i);
        };
    }

    static RowFilter of(ColumnName columnName, Predicate<Object> predicate)
    {
        ColumnName name = ArgumentCheck.nonNull(columnName);
        Predicate<Object> p = ArgumentCheck.nonNull(predicate);
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

    static RowFilter of(ColumnName columnName, IntPredicate predicate)
    {
        ColumnName name = ArgumentCheck.nonNull(columnName);
        IntPredicate p = ArgumentCheck.nonNull(predicate);
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

    static RowFilter of(ColumnName columnName, LongPredicate predicate)
    {
        ColumnName name = ArgumentCheck.nonNull(columnName);
        LongPredicate p = ArgumentCheck.nonNull(predicate);
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

    static RowFilter of(ColumnName columnName, DoublePredicate predicate)
    {
        ColumnName name = ArgumentCheck.nonNull(columnName);
        DoublePredicate p = ArgumentCheck.nonNull(predicate);
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
