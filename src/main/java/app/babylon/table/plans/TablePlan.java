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

import app.babylon.io.DataSource;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.io.RowSource;
import app.babylon.table.io.RowSupplier;
import app.babylon.table.io.TabularRowReader;

public interface TablePlan
{
    TablePlan withTableName(TableName tableName);

    TableName getTableName();

    TablePlan withTableDescription(TableDescription tableDescription);

    TableDescription getTableDescription();

    TableColumnar execute(DataSource dataSource, TabularRowReader reader);

    /**
     * Executes this plan against an already-open row supplier.
     * <p>
     * Use this lower-level entry point when the caller already owns the live source
     * resource and wants to control its lifetime explicitly.
     * <p>
     * CSV example:
     *
     * <pre>{@code
     * DataSource dataSource = ...;
     * TablePlanRead plan = new TablePlanRead();
     *
     * try (InputStream inputStream = dataSource.openStream();
     *         RowSupplier rowSupplier = RowSupplierCsv.builder()
     *                 .withSeparator(';')
     *                 .build(inputStream))
     * {
     *     TableColumnar table = plan.execute(rowSupplier);
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
     *         RowSupplier rowSupplier = new RowSupplierResultSet(resultSet))
     * {
     *     TableColumnar table = plan.execute(rowSupplier);
     * }
     * }</pre>
     *
     * @param rowSupplier
     *            open row supplier to consume
     * @return the resulting table
     */
    TableColumnar execute(RowSupplier rowSupplier);

    /**
     * Executes this plan against a configured row source.
     * <p>
     * This is the simplest high-level entry point when the source should handle
     * opening and closing its own row supplier internally.
     * <p>
     * CSV example:
     *
     * <pre>{@code
     * DataSource dataSource = ...;
     * TablePlanRead plan = new TablePlanRead();
     *
     * TableColumnar table = plan.execute(RowSourceCsv.builder()
     *         .withDataSource(dataSource)
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
