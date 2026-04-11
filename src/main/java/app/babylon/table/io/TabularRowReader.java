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

import java.util.Map;

import app.babylon.io.DataSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.ColumnName;

public interface TabularRowReader
{
    TabularRowReader withSelectedColumn(ColumnName columnName);

    TabularRowReader withSelectedColumns(ColumnName... columnNames);

    TabularRowReader withColumnRename(ColumnName original, ColumnName newName);

    TabularRowReader withColumnRenames(Map<ColumnName, ColumnName> renames);

    TabularRowReader withRowFilter(RowFilter rowFilter);

    ColumnName getColumnReName(ColumnName original);

    Result read(DataSource dataSource, RowConsumer rowConsumer);

    enum Status
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

    final class Result
    {
        private final Status status;
        private final String message;
        private final Throwable cause;

        public Result(Status status, String message, Throwable cause)
        {
            this.status = ArgumentCheck.nonNull(status);
            this.message = message;
            this.cause = cause;
        }

        public static Result success()
        {
            return new Result(Status.SUCCESS, null, null);
        }

        public static Result success(String message)
        {
            return new Result(Status.SUCCESS, message, null);
        }

        public static Result warning(String message)
        {
            return new Result(Status.WARNING, message, null);
        }

        public static Result empty(String message)
        {
            return new Result(Status.EMPTY, message, null);
        }

        public static Result exception(String message, Throwable cause)
        {
            return new Result(Status.EXCEPTION, message, cause);
        }

        public Status getStatus()
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
    }
}
