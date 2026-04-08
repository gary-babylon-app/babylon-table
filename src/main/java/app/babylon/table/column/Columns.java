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

import app.babylon.table.aggregation.Aggregate;
import app.babylon.table.column.Transformer;
import app.babylon.table.ViewIndex;
import app.babylon.text.BigDecimals;
import app.babylon.text.Strings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    public static <S> ColumnObject<S> stringToType(Column column, Function<String, S> parser, Class<S> targetClass)
    {
        if (!(column instanceof ColumnObject<?> co))
        {
            return null;
        }

        Class<?> valueClass = column.getType().getValueClass();

        if (targetClass.equals(valueClass))
        {
            @SuppressWarnings("unchecked")
            ColumnObject<S> typed = (ColumnObject<S>) co;
            return typed;
        }

        if (String.class.equals(valueClass))
        {
            @SuppressWarnings("unchecked")
            ColumnObject<String> strings = (ColumnObject<String>) co;

            Transformer<String, S> transformer = Transformer.of(s -> Strings.isEmpty(s) ? null : parser.apply(s),
                    targetClass);

            return strings.transform(transformer);
        }

        return null;
    }

    public static ColumnObject<BigDecimal> stringToDecimal(Column column)
    {
        return stringToType(column, BigDecimals::parse, BigDecimal.class);
    }

    public static boolean isEmpty(Column column)
    {
        if (column == null || column.size() == 0)
        {
            return true;
        }
        return column.isNoneSet();
    }

    public static ColumnBuilder newColumn(ColumnName colName, Column.Type type)
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
                return ColumnObject.builder(colName, valueClass);
            } else if (int.class.equals(valueClass))
            {
                return ColumnInt.builder(colName);
            } else if (double.class.equals(valueClass))
            {
                return ColumnDouble.builder(colName);
            } else if (long.class.equals(valueClass))
            {
                return ColumnLong.builder(colName);
            }
        }
        throw new IllegalArgumentException("Unsupported value class " + valueClass);
    }
    public static ColumnObject<BigDecimal> newDecimal(ColumnName colName, BigDecimal value, int size)
    {
        return ColumnCategorical.constant(colName, value, size, BigDecimal.class);
    }

    public static ColumnInt newInt(ColumnName colName, int value, int size)
    {
        return new ColumnIntConstant(colName, value, size);
    }

    public static ColumnObject<String> newString(ColumnName colName, String value, int size)
    {
        return ColumnCategorical.constant(colName, value, size, String.class);
    }

    public static <T> ColumnCategorical<T> newCategorical(ColumnName colName, T value, int size, Class<T> valueClass)
    {
        return new ColumnCategoricalConstant<T>(colName, value, size, valueClass);
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

    public static double aggregate(ColumnDouble cd, Aggregate aggregate)
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
                throw new IllegalArgumentException("Unsupported aggregate " + aggregate + " for double column.");
        }
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

    private static double sum(ColumnDouble cd)
    {
        double sum = 0.0d;
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                sum += cd.get(i);
            }
        }
        return sum;
    }

    private static double mean(ColumnDouble cd)
    {
        double sum = 0.0d;
        int n = 0;
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                sum += cd.get(i);
                ++n;
            }
        }
        return sum / n;
    }

    // private static BigDecimal max(ColumnObject<BigDecimal> cd)
    // {
    // if (cd.size()==0)
    // {
    // throw new RuntimeException("Can not compute max on column with no values. " +
    // cd.getName());
    // }
    //
    // BigDecimal max = null;
    // for(int i=0;i<cd.size();++i)
    // {
    // if (cd.isSet(i))
    // {
    // BigDecimal v = cd.get(i);
    // if (max==null)
    // {
    // max = v;
    // }
    // else if (max.compareTo(v)<0)
    // {
    // max = v;
    // }
    // }
    // }
    // return max;
    // }

    public static <T extends Comparable<? super T>> T max(ColumnObject<T> co)
    {
        if (co.size() == 0)
        {
            throw new RuntimeException("Can not compute max on column with no values. " + co.getName());
        }

        T max = null;
        for (int i = 0; i < co.size(); ++i)
        {
            if (co.isSet(i))
            {
                T v = co.get(i);
                if (max == null)
                {
                    max = v;
                } else if (max.compareTo(v) < 0)
                {
                    max = v;
                }
            }
        }
        return max;
    }

    private static double max(ColumnDouble cd)
    {
        if (cd.size() == 0)
        {
            throw new RuntimeException("Can not compute max on column with no values. " + cd.getName());
        }

        Double max = null;
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                double v = cd.get(i);
                if (max == null || Double.compare(max, v) < 0)
                {
                    max = v;
                }
            }
        }
        return max;
    }

    private static BigDecimal min(ColumnObject<BigDecimal> cd)
    {
        if (cd.size() == 0)
        {
            throw new RuntimeException("Can not compute min on column with no values. " + cd.getName());
        }

        BigDecimal min = null;
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                BigDecimal v = cd.get(i);
                if (min == null)
                {
                    min = v;
                } else if (min.compareTo(v) > 0)
                {
                    min = v;
                }
            }
        }
        return min;
    }

    private static double min(ColumnDouble cd)
    {
        if (cd.size() == 0)
        {
            throw new RuntimeException("Can not compute min on column with no values. " + cd.getName());
        }

        Double min = null;
        for (int i = 0; i < cd.size(); ++i)
        {
            if (cd.isSet(i))
            {
                double v = cd.get(i);
                if (min == null || Double.compare(min, v) > 0)
                {
                    min = v;
                }
            }
        }
        return min;
    }

    public static ColumnDouble newDouble(ColumnName colName, double value, int size)
    {
        return new ColumnDoubleConstant(colName, value, size);
    }

    public static <T extends Enum<T>> Column newView(Column c, ViewIndex rowIndex)
    {
        return c.view(rowIndex);
    }

    public static ColumnObject<String> newStringView(ColumnObject<String> c, ViewIndex rowIndex)
    {
        return asStringColumn(c.view(rowIndex));
    }

    @SuppressWarnings("unchecked")
    public static Column concat(List<Column> columns)
    {
        if (columns.size() == 0)
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
            Class<Object> typedValueClass = (Class<Object>) valueClass;
            ColumnCategorical.Builder<Object> newColumn = ColumnCategorical.builder(outputColumnName, typedValueClass);
            for (Column c : columns)
            {
                ColumnCategorical<Object> cc = (ColumnCategorical<Object>) c;
                for (int j = 0; j < cc.size(); ++j)
                {
                    if (cc.isSet(j))
                    {
                        newColumn.add(cc.get(j));
                    } else
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
            Class<Object> typedValueClass = (Class<Object>) valueClass;
            ColumnObject.Builder<Object> newColumn = ColumnObject.builder(outputColumnName, typedValueClass);

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
                    } else
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
                    } else
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
                    } else
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
                    } else
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
                    } else
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
            } else
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
