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

import app.babylon.table.TableDescription;
import app.babylon.table.TableName;

public abstract class TablePlanCommon<T extends TablePlanCommon<T>> implements TablePlan
{
    private TableName tableName;
    private TableDescription tableDescription;

    protected TablePlanCommon()
    {
        this.tableName = null;
        this.tableDescription = null;
    }

    @Override
    public T withTableName(TableName tableName)
    {
        this.tableName = tableName;
        return self();
    }

    @Override
    public TableName getTableName()
    {
        return this.tableName;
    }

    @Override
    public T withTableDescription(TableDescription tableDescription)
    {
        this.tableDescription = tableDescription;
        return self();
    }

    @Override
    public TableDescription getTableDescription()
    {
        return this.tableDescription;
    }

    protected abstract T self();
}
