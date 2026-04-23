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
     * CSV example:
     *
     * <pre>{@code
     * StreamSource streamSource = ...;
     * TablePlanRead plan = new TablePlanRead();
     *
     * try (InputStream inputStream = streamSource.openStream();
     *         RowCursor rowCursor = RowCursorCsv.builder()
     *                 .withSeparator(';')
     *                 .build(inputStream))
     * {
     *     TableColumnar table = plan.execute(rowCursor);
     * }
     * }</pre>
     *
     * JDBC example:
     *
     * <pre>{@code
     * PreparedStatement preparedStatement = ...;
     * TablePlanRead plan = new TablePlanRead();
     *
     * try (ResultSet resultSet = preparedStatement.executeQuery();
     *         RowCursor rowCursor = new RowCursorResultSet(resultSet))
     * {
     *     TableColumnar table = plan.execute(rowCursor);
     * }
     * }</pre>
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
     * CSV example:
     *
     * <pre>{@code
     * StreamSource streamSource = ...;
     * TablePlanRead plan = new TablePlanRead();
     *
     * TableColumnar table = plan.execute(RowSourceCsv.builder()
     *         .withStreamSource(streamSource)
     *         .withSeparator(';')
     *         .build());
     * }</pre>
     *
     * JDBC example:
     *
     * <pre>{@code
     * PreparedStatement preparedStatement = ...;
     * TablePlanRead plan = new TablePlanRead();
     *
     * TableColumnar table = plan.execute(RowSourceResultSet.builder()
     *         .withPreparedStatement(preparedStatement)
     *         .build());
     * }</pre>
     *
     * @param rowSource
     *            configured source that opens rows for this execution
     * @return the resulting table
     */
    TableColumnar execute(RowSource rowSource);
}
