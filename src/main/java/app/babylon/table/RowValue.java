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

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

final class RowValue
{
    private enum ValueCategory
    {
        OBJECT, INT, LONG
    }

    private final ColumnName[] names;
    private final ValueCategory[] valueCategories;
    private final boolean[] isSet;
    private final Object[] objects;
    private final int[] ints;
    private final long[] longs;

    private RowValue(int size)
    {
        this.names = new ColumnName[size];
        this.valueCategories = new ValueCategory[size];
        this.isSet = new boolean[size];
        this.objects = new Object[size];
        this.ints = new int[size];
        this.longs = new long[size];
    }

    static RowValue of(Column[] columns, int rowIndex)
    {
        RowValue rowValue = new RowValue(columns.length);
        for (int j = 0; j < columns.length; ++j)
        {
            Column column = columns[j];
            rowValue.names[j] = column.getName();
            if (column instanceof ColumnObject<?> co)
            {
                rowValue.valueCategories[j] = ValueCategory.OBJECT;
                if (co.isSet(rowIndex))
                {
                    Object value = co.get(rowIndex);
                    if (value instanceof BigDecimal decimal)
                    {
                        value = decimal.stripTrailingZeros();
                    }
                    rowValue.objects[j] = value;
                    rowValue.isSet[j] = true;
                }
            } else if (column instanceof ColumnInt ints)
            {
                rowValue.valueCategories[j] = ValueCategory.INT;
                if (ints.isSet(rowIndex))
                {
                    rowValue.ints[j] = ints.get(rowIndex);
                    rowValue.isSet[j] = true;
                }
            } else if (column instanceof ColumnLong longs)
            {
                rowValue.valueCategories[j] = ValueCategory.LONG;
                if (longs.isSet(rowIndex))
                {
                    rowValue.longs[j] = longs.get(rowIndex);
                    rowValue.isSet[j] = true;
                }
            } else
            {
                throw new IllegalArgumentException("removeDuplicates does not support key column type "
                        + column.getClass().getSimpleName() + " for column " + column.getName());
            }
        }
        return rowValue;
    }

    boolean isSet(ColumnName columnName)
    {
        return this.isSet[indexOf(columnName)];
    }

    Object getObject(ColumnName columnName)
    {
        int index = indexOf(columnName);
        requireValueCategory(index, ValueCategory.OBJECT, columnName);
        return this.objects[index];
    }

    BigDecimal getDecimal(ColumnName columnName)
    {
        Object value = getObject(columnName);
        if (value == null)
        {
            return null;
        }
        if (value instanceof BigDecimal decimal)
        {
            return decimal;
        }
        throw new IllegalStateException("Column " + columnName + " is not a BigDecimal value.");
    }

    int getInt(ColumnName columnName)
    {
        int index = indexOf(columnName);
        requireValueCategory(index, ValueCategory.INT, columnName);
        return this.ints[index];
    }

    long getLong(ColumnName columnName)
    {
        int index = indexOf(columnName);
        requireValueCategory(index, ValueCategory.LONG, columnName);
        return this.longs[index];
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof RowValue)
        {
            RowValue that = (RowValue) obj;
            for (int i = 0; i < this.valueCategories.length; ++i)
            {
                if (!Objects.equals(this.names[i], that.names[i]) || this.valueCategories[i] != that.valueCategories[i]
                        || this.isSet[i] != that.isSet[i])
                {
                    return false;
                }
                if (!this.isSet[i])
                {
                    continue;
                }
                switch (this.valueCategories[i])
                {
                    case OBJECT :
                        if (!Objects.equals(this.objects[i], that.objects[i]))
                        {
                            return false;
                        }
                        break;
                    case INT :
                        if (this.ints[i] != that.ints[i])
                        {
                            return false;
                        }
                        break;
                    case LONG :
                        if (this.longs[i] != that.longs[i])
                        {
                            return false;
                        }
                        break;
                    default :
                        throw new IllegalStateException("Unsupported RowValue category " + this.valueCategories[i]);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int result = 1;
        for (int i = 0; i < this.valueCategories.length; ++i)
        {
            result = 31 * result + Objects.hashCode(this.valueCategories[i]);
            result = 31 * result + Boolean.hashCode(this.isSet[i]);
            if (!this.isSet[i])
            {
                continue;
            }
            switch (this.valueCategories[i])
            {
                case OBJECT :
                    result = 31 * result + Objects.hashCode(this.objects[i]);
                    break;
                case INT :
                    result = 31 * result + Integer.hashCode(this.ints[i]);
                    break;
                case LONG :
                    result = 31 * result + Long.hashCode(this.longs[i]);
                    break;
                default :
                    throw new IllegalStateException("Unsupported RowValue category " + this.valueCategories[i]);
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        return "RowValue(names=" + Arrays.toString(this.names) + ", valueCategories="
                + Arrays.toString(this.valueCategories) + ", isSet=" + Arrays.toString(this.isSet) + ", objects="
                + Arrays.toString(this.objects) + ", ints=" + Arrays.toString(this.ints) + ", longs="
                + Arrays.toString(this.longs) + ")";
    }

    private int indexOf(ColumnName columnName)
    {
        for (int i = 0; i < this.names.length; ++i)
        {
            if (Objects.equals(this.names[i], columnName))
            {
                return i;
            }
        }
        throw new IllegalArgumentException("No column " + columnName + " in RowValue.");
    }

    private void requireValueCategory(int index, ValueCategory expectedValueCategory, ColumnName columnName)
    {
        if (this.valueCategories[index] != expectedValueCategory)
        {
            throw new IllegalStateException("Column " + columnName + " is not of the expected type.");
        }
    }
}
