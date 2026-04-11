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
import app.babylon.table.io.TabularReader;

public interface TablePlan
{
    TableColumnar execute(DataSource dataSource, TabularReader reader);
}
