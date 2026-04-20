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

import app.babylon.table.column.ColumnDefinition;

/**
 * Supplies tabular rows from a live underlying source.
 * <p>
 * A row supplier represents an open row cursor. Some implementations own the
 * underlying resource and release it on {@link #close()}, while others adapt
 * caller-owned resources and treat {@code close()} as a no-op.
 * <p>
 * The {@link ColumnDefinition}s returned by {@link #columns()} describe the
 * source-side schema seen by the row consumer. In particular, any
 * source-specified {@link app.babylon.table.column.Column.Type column types}
 * can influence which low-level builders are created before rows are read. That
 * matters when the caller wants to avoid an intermediate in-memory
 * {@code String} column and parse slices directly into the final builder, for
 * example:
 * <p>
 * - primitive numeric columns such as {@code int}, {@code long}, and
 * {@code double}
 * <p>
 * - fast enum-like object types with a direct {@code CharSequence} parser, when
 * that is materially better than first building a string dictionary
 * <p>
 * - high-cardinality columns where building a full string column and then
 * parsing it later would be unnecessary work
 * <p>
 * For ordinary categorical text, source-side typing is usually not necessary.
 * In that common case it is often better to let the row consumer build the
 * natural string dictionary first and only request a direct source type when
 * that parser has a real advantage over storing dictionary strings.
 */
public interface RowCursor extends AutoCloseable
{
    /**
     * Returns the source-side column definitions exposed by this cursor.
     * <p>
     * A non-null {@link ColumnDefinition#type() type} here is not just metadata. It
     * can be used by downstream row consumers to choose a more direct builder from
     * the start, bypassing intermediate {@code String} materialization when the
     * source type already has an efficient slice-based parser.
     * <p>
     * That is most useful for primitive numerics and for special object types whose
     * direct parser is genuinely better than first building a string dictionary.
     *
     * @return the source column definitions in source order
     */
    ColumnDefinition[] columns();

    boolean next();

    Row current();

    @Override
    void close() throws Exception;
}
