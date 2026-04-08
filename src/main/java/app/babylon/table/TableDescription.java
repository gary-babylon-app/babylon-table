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

public class TableDescription
{
    private String value;

    public TableDescription()
    {
        this("");
    }

    public TableDescription(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }
}
