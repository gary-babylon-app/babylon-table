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

public enum TabularReadStatus
{
    SUCCESS, WARNING, EMPTY, EXCEPTION;

    public boolean isSuccessLike()
    {
        return this == SUCCESS || this == WARNING || this == EMPTY;
    }

    public boolean isFailure()
    {
        return this == EXCEPTION;
    }
}
