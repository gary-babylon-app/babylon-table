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

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;

public final class TabularReadResult<T>
{
    private final TabularReadStatus status;
    private final String message;
    private final Throwable cause;
    private final T value;

    public TabularReadResult(TabularReadStatus status, String message, Throwable cause, T value)
    {
        this.status = ArgumentCheck.nonNull(status);
        this.message = message;
        this.cause = cause;
        this.value = value;
    }

    public static <T> TabularReadResult<T> success(T value)
    {
        return new TabularReadResult<>(TabularReadStatus.SUCCESS, null, null, value);
    }

    public static <T> TabularReadResult<T> success(T value, String message)
    {
        return new TabularReadResult<>(TabularReadStatus.SUCCESS, message, null, value);
    }

    public static <T> TabularReadResult<T> warning(T value, String message)
    {
        return new TabularReadResult<>(TabularReadStatus.WARNING, message, null, value);
    }

    public static <T> TabularReadResult<T> empty(String message)
    {
        return new TabularReadResult<>(TabularReadStatus.EMPTY, message, null, null);
    }

    public static <T> TabularReadResult<T> exception(String message, Throwable cause)
    {
        return new TabularReadResult<>(TabularReadStatus.EXCEPTION, message, cause, null);
    }

    public TabularReadStatus getStatus()
    {
        return this.status;
    }

    public boolean isSuccessLike()
    {
        return this.status.isSuccessLike();
    }

    public String getMessage()
    {
        return this.message;
    }

    public Throwable getCause()
    {
        return this.cause;
    }

    public T getValue()
    {
        return this.value;
    }

    public boolean hasValue()
    {
        return this.value != null;
    }

    public TableColumnar getTable()
    {
        if (this.value instanceof TableColumnar)
        {
            return (TableColumnar) this.value;
        }
        return null;
    }
}
