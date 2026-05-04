/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.dsl;

public class TransformDslException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    private final int position;

    public TransformDslException(String message, int position)
    {
        super(message + " at column " + (position + 1));
        this.position = position;
    }

    public int position()
    {
        return this.position;
    }

    public int column()
    {
        return this.position + 1;
    }
}
