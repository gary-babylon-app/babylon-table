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

import java.util.Optional;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ToStringSettings;
import app.babylon.table.ViewIndex;
import app.babylon.table.column.type.TypeParser;
import app.babylon.table.column.type.TypeWriter;

/**
 * Base contract for a named column of tabular data, including size, null-state,
 * comparison, projection, and row-level access operations.
 */
public interface Column
{
    /**
     * Builds an immutable column instance for a specific column name.
     */
    public static interface Builder
    {
        /**
         * Returns the name that will be assigned to the built column.
         *
         * @return the target column name
         */
        public ColumnName getName();

        /**
         * Appends a value directly from a character slice.
         *
         * @param chars
         *            the source text
         * @param start
         *            the start offset
         * @param length
         *            the slice length
         * @return this builder
         */
        default Builder add(CharSequence chars, int start, int length)
        {
            throw new UnsupportedOperationException("Character-slice add not supported by " + getClass().getName());
        }

        /**
         * Materialises the current builder contents as an immutable column.
         *
         * @return the built column
         */
        public Column build();

        // default public Column build(Column.Type type)
        // {
        // return build();
        // }
    }

    /**
     * Describes the runtime value type stored by a column.
     */
    public static interface Type
    {
        /**
         * Creates a column type descriptor from its runtime value class and parser.
         *
         * @param valueClass
         *            the Java class represented by the column type
         * @param parser
         *            the parser associated with this type
         * @return the created column type descriptor
         */
        public static Type of(Class<?> valueClass, TypeParser<?> parser)
        {
            return of(valueClass, parser, null);
        }

        /**
         * Creates a column type descriptor from its runtime value class, parser, and
         * optional default writer.
         *
         * @param valueClass
         *            the Java class represented by the column type
         * @param parser
         *            the parser associated with this type
         * @param writer
         *            the optional writer associated with this type
         * @return the created column type descriptor
         */
        public static Type of(Class<?> valueClass, TypeParser<?> parser, TypeWriter<?> writer)
        {
            final Class<?> resolvedValueClass = ArgumentCheck.nonNull(valueClass);
            final TypeParser<?> resolvedParser = ArgumentCheck.nonNull(parser);
            final TypeWriter<?> resolvedWriter = writer;
            return new Type()
            {
                @Override
                public Class<?> getValueClass()
                {
                    return resolvedValueClass;
                }

                @Override
                public TypeParser<?> getParser()
                {
                    return resolvedParser;
                }

                @Override
                public Optional<TypeWriter<?>> getWriter()
                {
                    return Optional.ofNullable(resolvedWriter);
                }

                @Override
                public int hashCode()
                {
                    return this.getValueClass().hashCode();
                }

                @Override
                public boolean equals(Object obj)
                {
                    if (this == obj)
                    {
                        return true;
                    }
                    if (!(obj instanceof Column.Type other))
                    {
                        return false;
                    }
                    return this.getValueClass().equals(other.getValueClass());
                }

                @Override
                public String toString()
                {
                    String simpleName = this.getValueClass().getSimpleName();
                    return simpleName.isEmpty() ? this.getValueClass().getName() : simpleName;
                }
            };
        }

        /**
         * Returns the Java class represented by this column type.
         *
         * @return the runtime value class
         */
        public Class<?> getValueClass();

        /**
         * Returns the parser associated with this logical type.
         *
         * @return the type parser
         */
        public TypeParser<?> getParser();

        /**
         * Returns the optional writer associated with this logical type.
         *
         * @return the optional type writer
         */
        public Optional<TypeWriter<?>> getWriter();

        /**
         * Indicates whether the represented value class is a primitive Java type.
         *
         * @return {@code true} when the underlying value class is primitive
         */
        public default boolean isPrimitive()
        {
            return getValueClass().isPrimitive();
        }
    }

    /**
     * Returns the declared type of values stored in this column.
     *
     * @return the column value type
     */
    public Type getType();

    /**
     * Returns the number of rows in the column.
     *
     * @return the row count
     */
    public int size();

    /**
     * Returns the logical column name.
     *
     * @return the column name
     */
    public ColumnName getName();

    /**
     * Indicates whether the value at the supplied row is present.
     *
     * @param i
     *            the zero-based row index
     * @return {@code true} when the row contains a value
     */
    public boolean isSet(int i);

    /**
     * Indicates whether every row in the column contains a value.
     *
     * @return {@code true} when all rows are set
     */
    public boolean isAllSet();

    /**
     * Indicates whether no row in the column contains a value.
     *
     * @return {@code true} when every row is unset
     */
    public boolean isNoneSet();

    /**
     * Indicates whether the column has no set values, either because it has no rows
     * or because every row is unset.
     *
     * @return {@code true} when the column is effectively empty
     */
    default public boolean isEmpty()
    {
        return size() == 0 || isNoneSet();
    }

    /**
     * Formats the value at the supplied row for display.
     *
     * @param i
     *            the zero-based row index
     * @return the formatted row value, or an empty string when unset
     */
    public String toString(int i);

    /**
     * Formats the value at the supplied row using the provided rendering settings.
     *
     * @param i
     *            the zero-based row index
     * @param settings
     *            formatting settings to apply
     * @return the formatted row value
     */
    default public String toString(int i, ToStringSettings settings)
    {
        return toString(i);
    }

    /**
     * Appends the value at the supplied row using the provided rendering settings.
     *
     * @param i
     *            the zero-based row index
     * @param out
     *            the destination buffer
     * @param settings
     *            formatting settings to apply
     */
    default public void appendTo(int i, StringBuilder out, ToStringSettings settings)
    {
        out.append(toString(i, settings));
    }

    /**
     * Compares two row values from this column using the column's ordering
     * semantics.
     *
     * @param i
     *            the first row index
     * @param j
     *            the second row index
     * @return a negative number, zero, or a positive number as the first row is
     *         less than, equal to, or greater than the second
     */
    public int compare(int i, int j);

    /**
     * Returns a projected view of this column using the supplied row mapping.
     *
     * @param rowIndex
     *            the row mapping to apply
     * @return a column view over the selected rows
     */
    public Column view(ViewIndex rowIndex);

    /**
     * Creates a copy of this column with the supplied column name.
     *
     * @param x
     *            the name to assign to the copy
     * @return a column containing the same values under the new name
     */
    public Column copy(ColumnName x);

    /**
     * Returns a single-row column containing the value from the supplied row.
     *
     * @param i
     *            the row to extract
     * @return a single-row column with the same column name
     */
    public Column getAsColumn(int i);

}
