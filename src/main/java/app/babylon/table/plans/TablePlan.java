/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.plans;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.io.RowSource;
import app.babylon.table.io.RowCursor;

public interface TablePlan
{
    TablePlan withTableName(TableName tableName);

    TableName getTableName();

    TablePlan withTableDescription(TableDescription tableDescription);

    TableDescription getTableDescription();

    /**
     * Executes this plan against an already-open row supplier.
     * <p>
     * Use this lower-level entry point when the caller already owns the live source
     * resource and wants to control its lifetime explicitly.
     * <p>
     * For CSV, open the input stream, create a {@code RowCursor}, then pass that
     * cursor to this method inside the caller's resource scope. For JDBC, execute
     * the prepared statement, wrap the live {@code ResultSet} in a row cursor, and
     * pass it to this method before closing the JDBC resources.
     *
     * @param rowCursor
     *            open row supplier to consume
     * @return the resulting table
     */
    TableColumnar execute(RowCursor rowCursor);

    /**
     * Executes this plan against a configured row source.
     * <p>
     * This is the simplest high-level entry point when the source should handle
     * opening and closing its own row supplier internally.
     * <p>
     * For CSV or JDBC, build an appropriate {@code RowSource} and pass it to this
     * method. The row source owns the open/close lifecycle for each execution.
     *
     * @param rowSource
     *            configured source that opens rows for this execution
     * @return the resulting table
     */
    TableColumnar execute(RowSource rowSource);
}
