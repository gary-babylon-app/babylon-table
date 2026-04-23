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

import app.babylon.table.selection.Selection;

/**
 * A column of byte values used for compact storage of byte-oriented data and
 * file-signature style checks.
 */
public interface ColumnByte extends Column
{
    /**
     * Column type descriptor for primitive byte columns.
     */
    public static final Type TYPE = ColumnTypes.BYTE;

    /**
     * Builder for nullable byte columns.
     */
    public static interface Builder extends Column.Builder
    {
        /**
         * Appends a byte value.
         *
         * @param x
         *            the value to append
         * @return this builder
         */
        public Builder add(byte x);

        default Builder add(CharSequence chars, int start, int length)
        {
            if (chars == null || length == 0)
            {
                return addNull();
            }
            try
            {
                return add(TYPE.getParser().parseByte(chars, start, length));
            }
            catch (RuntimeException e)
            {
                return addNull();
            }
        }

        /**
         * Appends an unset row.
         *
         * @return this builder
         */
        public Builder addNull();

        @Override
        public ColumnByte build();
    }

    /**
     * Creates a byte column builder for the supplied column name.
     *
     * @param name
     *            the column name
     * @return a new byte column builder
     */
    public static Builder builder(ColumnName name)
    {
        return new ColumnByteBuilderArray(name);
    }

    @Override
    default public Type getType()
    {
        return TYPE;
    }

    /**
     * Returns the byte value at the supplied row.
     *
     * @param i
     *            the zero-based row index
     * @return the byte value
     */
    public byte get(int i);

    /**
     * Copies the values into the provided array, allocating a new array when
     * necessary.
     *
     * @param x
     *            the destination array, or {@code null}
     * @return an array containing the column values
     */
    public byte[] toArray(byte[] x);

    /**
     * Returns the maximum set value in the column.
     *
     * @return maximum set value
     */
    default byte max()
    {
        if (isEmpty())
        {
            throw new RuntimeException("Can not compute max on column with no values. " + getName());
        }
        if (isConstant())
        {
            return get(0);
        }

        boolean found = false;
        byte max = 0;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                byte value = get(i);
                if (!found || value > max)
                {
                    max = value;
                    found = true;
                }
            }
        }
        if (!found)
        {
            throw new RuntimeException("Can not compute max on column with no values. " + getName());
        }
        return max;
    }

    /**
     * Returns the minimum set value in the column.
     *
     * @return minimum set value
     */
    default byte min()
    {
        if (isEmpty())
        {
            throw new RuntimeException("Can not compute min on column with no values. " + getName());
        }
        if (isConstant())
        {
            return get(0);
        }

        boolean found = false;
        byte min = 0;
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                byte value = get(i);
                if (!found || value < min)
                {
                    min = value;
                    found = true;
                }
            }
        }
        if (!found)
        {
            throw new RuntimeException("Can not compute min on column with no values. " + getName());
        }
        return min;
    }

    @Override
    default int compare(int i, int j)
    {
        if (i == j)
        {
            return 0;
        }
        boolean aSet = isSet(i);
        boolean bSet = isSet(j);
        if (aSet && bSet)
        {
            return Byte.compare(get(i), get(j));
        }
        if (!aSet && !bSet)
        {
            return 0;
        }
        return aSet ? 1 : -1;
    }

    @Override
    default public String toString(int i)
    {
        return isSet(i) ? Byte.toString(get(i)) : "";
    }

    @Override
    default public Column getAsColumn(int i)
    {
        return isSet(i)
                ? new ColumnByteConstant(getName(), get(i), 1, true)
                : ColumnByteConstant.createNull(getName(), 1);
    }

    @Override
    default public ColumnByte copy(ColumnName x)
    {
        Builder newBuilder = ColumnByte.builder(x);
        for (int i = 0; i < size(); ++i)
        {
            if (isSet(i))
            {
                newBuilder.add(get(i));
            }
            else
            {
                newBuilder.addNull();
            }
        }
        return newBuilder.build();
    }

    /**
     * Selects rows whose values satisfy the supplied predicate.
     *
     * @param predicate
     *            the predicate to test against each set value
     * @return a selection containing the predicate result for each row
     */
    default Selection select(BytePredicate predicate)
    {
        BytePredicate p = predicate;
        Selection selection = new Selection(this.getName() + " filtered.");
        for (int i = 0; i < this.size(); ++i)
        {
            if (isSet(i))
            {
                selection.add(p.test(get(i)));
            }
            else
            {
                selection.add(false);
            }
        }
        return selection;
    }

    /**
     * Indicates whether the column represents the same value for every row.
     *
     * @return {@code true} when the column is constant
     */
    public boolean isConstant();

    /**
     * Indicates whether the byte values match the legacy XLS file signature.
     *
     * @return {@code true} when the column contents look like XLS bytes
     */
    public boolean isXls();

    /**
     * Indicates whether the byte values match the XLSX file signature.
     *
     * @return {@code true} when the column contents look like XLSX bytes
     */
    public boolean isXlsx();
}
