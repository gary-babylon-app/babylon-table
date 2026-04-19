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

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import app.babylon.lang.Is;
import app.babylon.table.ViewIndex;
import app.babylon.table.aggregation.AccumulatorDouble;
import app.babylon.table.aggregation.Aggregate;
import app.babylon.table.column.type.TypeParser;
import app.babylon.table.sorting.ComparatorInt;
import app.babylon.table.sorting.SortInt;
import app.babylon.text.BigDecimals;
import app.babylon.text.Strings;

//import app.babylon.BigDecimals;
//import app.babylon.BigDecimals;

public class Columns
{
    public static boolean isStringColumn(Column column)
    {
        return column instanceof ColumnObject<?> && String.class.equals(column.getType().getValueClass());
    }

    @SuppressWarnings("unchecked")
    public static ColumnObject<String> asStringColumn(Column column)
    {
        if (!isStringColumn(column))
        {
            throw new IllegalArgumentException("Not a string column: " + column);
        }
        return (ColumnObject<String>) column;
    }

    public static <T> Map<T, Integer> frequencyMap(ColumnObject<T> c)
    {
        Map<T, Integer> m = new HashMap<>();
        for (int i = 0; i < c.size(); ++i)
        {
            if (c.isSet(i))
            {
                T t = c.get(i);
                m.put(t, m.getOrDefault(t, 0) + 1);
            }
        }
        return m;
    }

    public static <S> ColumnObject<S> stringToType(Column column, Function<String, S> parser, Column.Type targetType)
    {
        if (!(column instanceof ColumnObject<?> co))
        {
            return null;
        }

        Column.Type type = app.babylon.lang.ArgumentCheck.nonNull(targetType);

        Class<?> valueClass = column.getType().getValueClass();

        if (type.getValueClass().equals(valueClass))
        {
            @SuppressWarnings("unchecked")
            ColumnObject<S> typed = (ColumnObject<S>) co;
            return typed;
        }

        if (String.class.equals(valueClass))
        {
            @SuppressWarnings("unchecked")
            ColumnObject<String> strings = (ColumnObject<String>) co;

            Transformer<String, S> transformer = Transformer.of(s -> Strings.isEmpty(s) ? null : parser.apply(s), type);

            return strings.transform(transformer);
        }

        return null;
    }

    public static ColumnObject<BigDecimal> stringToDecimal(Column column)
    {
        return stringToType(column, BigDecimals::parse, ColumnTypes.DECIMAL);
    }

    public static boolean isEmpty(Column column)
    {
        if (column == null || column.size() == 0)
        {
            return true;
        }
        return column.isEmpty();
    }

    public static Column.Builder newColumn(ColumnName colName, Column.Type type)
    {
        if (type == null)
        {
            throw new RuntimeException("Unsupported type null");
        }

        Class<?> valueClass = type.getValueClass();
        if (valueClass != null)
        {
            if (!valueClass.isPrimitive())
            {
                return ColumnObject.builder(colName, type);
            }
            else if (int.class.equals(valueClass))
            {
                return ColumnInt.builder(colName);
            }
            else if (double.class.equals(valueClass))
            {
                return ColumnDouble.builder(colName);
            }
            else if (long.class.equals(valueClass))
            {
                return ColumnLong.builder(colName);
            }
            else if (byte.class.equals(valueClass))
            {
                return ColumnByte.builder(colName);
            }
        }
        throw new IllegalArgumentException("Unsupported value class " + valueClass);
    }

    public static Column.Builder newCharSliceBuilder(ColumnName colName, Column.Type type)
    {
        if (type == null)
        {
            throw new RuntimeException("Unsupported type null");
        }

        if (ColumnTypes.STRING.equals(type))
        {
            return ColumnObject.builder(colName, app.babylon.table.column.ColumnTypes.STRING);
        }
        if (ColumnTypes.DECIMAL.equals(type))
        {
            return ColumnObject.builderDecimal(colName);
        }
        if (ColumnDouble.TYPE.equals(type))
        {
            return ColumnDouble.builder(colName);
        }
        if (ColumnInt.TYPE.equals(type))
        {
            return ColumnInt.builder(colName);
        }
        if (ColumnLong.TYPE.equals(type))
        {
            return ColumnLong.builder(colName);
        }
        if (ColumnByte.TYPE.equals(type))
        {
            return ColumnByte.builder(colName);
        }
        Class<?> valueClass = type.getValueClass();
        throw new IllegalArgumentException("Unsupported char-slice builder type " + valueClass);
    }

    public static ColumnObject<BigDecimal> newDecimal(ColumnName colName, BigDecimal value, int size)
    {
        return ColumnCategorical.constant(colName, value, size, app.babylon.table.column.ColumnTypes.DECIMAL);
    }

    public static ColumnInt newInt(ColumnName colName, int value, int size)
    {
        return new ColumnIntConstant(colName, value, size);
    }

    public static ColumnByte newByte(ColumnName colName, byte value, int size)
    {
        return new ColumnByteConstant(colName, value, size);
    }

    public static ColumnObject<String> newString(ColumnName colName, String value, int size)
    {
        return ColumnCategorical.constant(colName, value, size, app.babylon.table.column.ColumnTypes.STRING);
    }

    public static <T> ColumnCategorical<T> newCategorical(ColumnName colName, T value, int size, Column.Type type)
    {
        return new ColumnCategoricalConstant<T>(colName, value, size, type);
    }

    public static BigDecimal aggregate(ColumnObject<BigDecimal> cd, Aggregate aggregate)
    {
        switch (aggregate)
        {
            case SUM :
                return sum(cd);
            case MIN :
                return min(cd);
            case MAX :
                return max(cd);
            case MEAN :
                return mean(cd);

            default :
                return null;
        }
    }

    public static <T> T max(ColumnObject<T> co)
    {
        return co.max();
    }

    public static <T> T min(ColumnObject<T> co)
    {
        return co.min();
    }

    public static double aggregate(ColumnDouble cd, Aggregate aggregate)
    {
        AccumulatorDouble accumulator = new AccumulatorDouble();
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                accumulator.accept(cd.get(i));
            }
        }
        return accumulator.get(aggregate);
    }

    private static BigDecimal sum(ColumnObject<BigDecimal> cd)
    {
        BigDecimal sum = BigDecimal.ZERO;
        MathContext mc = MathContext.DECIMAL64;
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                sum = sum.add(cd.get(i), mc);
            }
        }
        return sum;
    }

    private static BigDecimal mean(ColumnObject<BigDecimal> cd)
    {
        BigDecimal sum = BigDecimal.ZERO;
        MathContext mc = MathContext.DECIMAL64;
        int n = 0;
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                sum = sum.add(cd.get(i), mc);
                ++n;
            }
        }
        return sum.divide(new BigDecimal(n), mc);
    }

    public static ColumnDouble newDouble(ColumnName colName, double value, int size)
    {
        return new ColumnDoubleConstant(colName, value, size);
    }

    public static <T extends Enum<T>> Column newView(Column c, ViewIndex rowIndex)
    {
        return c.view(rowIndex);
    }

    public static Column sort(Column column)
    {
        return sort(column, column::compare);
    }

    public static Column sort(Column column, ComparatorInt comparator)
    {
        if (column == null)
        {
            return null;
        }
        if (column.size() == 0)
        {
            return column.view(ViewIndex.builder().build());
        }

        int[] sortArray = new int[column.size()];
        for (int i = 0; i < sortArray.length; ++i)
        {
            sortArray[i] = i;
        }
        SortInt.stableSort(sortArray, comparator);

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.addAll(sortArray);
        return column.view(rowIndexBuilder.build());
    }

    public static ColumnObject<String> newStringView(ColumnObject<String> c, ViewIndex rowIndex)
    {
        return asStringColumn(c.view(rowIndex));
    }

    @SuppressWarnings("unchecked")
    public static Column concat(List<Column> columns)
    {
        if (Is.empty(columns))
        {
            return null;
        }
        Column firstColumn = columns.get(0);
        Column.Type type = firstColumn.getType();
        ColumnName outputColumnName = firstColumn.getName();

        for (int i = 1; i < columns.size(); ++i)
        {
            Column c = columns.get(i);
            if (!sameValueClass(type, c.getType()))
            {
                throw new IllegalArgumentException("Cannot concat different column types: " + type + " and "
                        + c.getType() + " for column " + outputColumnName);
            }
        }

        if (firstColumn instanceof ColumnCategorical<?> && allCategorical(columns))
        {
            Class<?> valueClass = type.getValueClass();
            if (valueClass == null || valueClass.isPrimitive())
            {
                throw new IllegalStateException("Unsupported categorical column type for concat: " + type);
            }
            ColumnCategorical.Builder<Object> newColumn = ColumnCategorical.builder(outputColumnName, type);
            for (Column c : columns)
            {
                ColumnCategorical<Object> cc = (ColumnCategorical<Object>) c;
                for (int j = 0; j < cc.size(); ++j)
                {
                    if (cc.isSet(j))
                    {
                        newColumn.add(cc.get(j));
                    }
                    else
                    {
                        newColumn.addNull();
                    }
                }
            }
            return newColumn.build();
        }

        if (firstColumn instanceof ColumnObject<?>)
        {
            Class<?> valueClass = type.getValueClass();
            if (valueClass == null || valueClass.isPrimitive())
            {
                throw new IllegalStateException("Unsupported object column type for concat: " + type);
            }
            ColumnObject.Builder<Object> newColumn = ColumnObject.builder(outputColumnName, type);

            for (Column c : columns)
            {
                if (!(c instanceof ColumnObject<?>))
                {
                    throw new IllegalArgumentException(
                            "Cannot concat object with non-object for column " + outputColumnName);
                }
                ColumnObject<Object> co = (ColumnObject<Object>) c;
                for (int j = 0; j < co.size(); ++j)
                {
                    if (co.isSet(j))
                    {
                        newColumn.add(co.get(j));
                    }
                    else
                    {
                        newColumn.addNull();
                    }
                }
            }
            return newColumn.build();
        }

        if (ColumnInt.TYPE.equals(type))
        {
            ColumnInt.Builder newColumn = ColumnInt.builder(outputColumnName);
            for (Column c : columns)
            {
                ColumnInt ci = (ColumnInt) c;
                for (int j = 0; j < ci.size(); ++j)
                {
                    if (ci.isSet(j))
                    {
                        newColumn.add(ci.get(j));
                    }
                    else
                    {
                        newColumn.addNull();
                    }
                }
            }
            return newColumn.build();
        }
        if (ColumnLong.TYPE.equals(type))
        {
            ColumnLong.Builder newColumn = ColumnLong.builder(outputColumnName);
            for (Column c : columns)
            {
                ColumnLong cl = (ColumnLong) c;
                for (int j = 0; j < cl.size(); ++j)
                {
                    if (cl.isSet(j))
                    {
                        newColumn.add(cl.get(j));
                    }
                    else
                    {
                        newColumn.addNull();
                    }
                }
            }
            return newColumn.build();
        }
        if (ColumnDouble.TYPE.equals(type))
        {
            ColumnDouble.Builder newColumn = ColumnDouble.builder(outputColumnName);
            for (Column c : columns)
            {
                ColumnDouble cd = (ColumnDouble) c;
                for (int j = 0; j < cd.size(); ++j)
                {
                    if (cd.isSet(j))
                    {
                        newColumn.add(cd.get(j));
                    }
                    else
                    {
                        newColumn.addNull();
                    }
                }
            }
            return newColumn.build();
        }
        if (ColumnByte.TYPE.equals(type))
        {
            ColumnByte.Builder newColumn = ColumnByte.builder(outputColumnName);
            for (Column c : columns)
            {
                ColumnByte cb = (ColumnByte) c;
                for (int j = 0; j < cb.size(); ++j)
                {
                    if (cb.isSet(j))
                    {
                        newColumn.add(cb.get(j));
                    }
                    else
                    {
                        newColumn.addNull();
                    }
                }
            }
            return newColumn.build();
        }
        throw new IllegalStateException("Unsupported column type for concat: " + type);
    }

    private static boolean sameValueClass(Column.Type a, Column.Type b)
    {
        Class<?> aClass = a.getValueClass();
        Class<?> bClass = b.getValueClass();
        return aClass != null && aClass.equals(bClass);
    }

    private static boolean allCategorical(List<Column> columns)
    {
        for (Column c : columns)
        {
            if (!(c instanceof ColumnCategorical<?>))
            {
                return false;
            }
        }
        return true;
    }

    public static String toString(Column column)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(column.getName().getValue());
        for (int i = 0; i < Math.max(Math.min(2, column.size()), 0); ++i)
        {
            if (i != 0)
            {
                builder.append(", ");
            }
            else
            {
                builder.append("[");
            }
            builder.append(column.toString(i));
        }
        if (column.size() > 1)
        {
            builder.append(", ... ,");
            builder.append(column.toString(column.size() - 1));
        }
        builder.append("]");

        return builder.toString();
    }

}
