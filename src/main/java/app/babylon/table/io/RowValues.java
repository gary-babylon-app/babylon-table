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

import app.babylon.table.column.Column;

/**
 * Immutable values for one physical source row.
 * <p>
 * Implementations keep their natural backing representation and decide how best
 * to append a field to a column builder.
 */
public interface RowValues
{
    int size();

    boolean isEmpty();

    boolean isSet(int fieldIndex);

    String getString(int fieldIndex);

    default RowValues select(int[] selectedIndexes)
    {
        return select(selectedIndexes, false);
    }

    RowValues select(int[] selectedIndexes, boolean strip);

    void addTo(Column.Builder builder, int fieldIndex);
}
