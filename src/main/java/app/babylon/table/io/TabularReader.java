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
import app.babylon.table.TableColumnar;
import app.babylon.table.column.ColumnName;

public interface TabularReader
{
    TabularReader withSelectedColumn(ColumnName columnName);

    TabularReader withSelectedColumns(ColumnName... columnNames);

    TabularReader withColumnRename(ColumnName original, ColumnName newName);

    TabularReader withColumnRenames(Map<ColumnName, ColumnName> renames);

    TabularReader withRowFilter(RowFilter rowFilter);

    TabularReader withRowConsumer(RowConsumer<TableColumnar> rowConsumer);

    Result read(DataSource dataSource);

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
        private final TableColumnar table;

        public Result(Status status, String message, Throwable cause, TableColumnar table)
        {
            this.status = ArgumentCheck.nonNull(status);
            this.message = message;
            this.cause = cause;
            this.table = table;
        }

        public static Result success(TableColumnar table)
        {
            return new Result(Status.SUCCESS, null, null, table);
        }

        public static Result success(TableColumnar table, String message)
        {
            return new Result(Status.SUCCESS, message, null, table);
        }

        public static Result warning(TableColumnar table, String message)
        {
            return new Result(Status.WARNING, message, null, table);
        }

        public static Result empty(String message)
        {
            return new Result(Status.EMPTY, message, null, null);
        }

        public static Result exception(String message, Throwable cause)
        {
            return new Result(Status.EXCEPTION, message, cause, null);
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

        public TableColumnar getTable()
        {
            return this.table;
        }

        public boolean hasTable()
        {
            return this.table != null;
        }
    }
}
