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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;

/**
 * Configured JDBC row source that opens rows by executing a prepared statement.
 */
public final class RowSourceResultSet implements RowSource
{
    private final PreparedStatement preparedStatement;
    private final String name;

    private RowSourceResultSet(Builder builder)
    {
        this.preparedStatement = ArgumentCheck.nonNull(builder.preparedStatement);
        this.name = builder.name == null ? "PreparedStatement" : builder.name;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public RowSupplier openRows()
    {
        try
        {
            return RowSupplierResultSet.open(this.preparedStatement);
        }
        catch (SQLException e)
        {
            throw new TableException("Failed to execute prepared statement for row source '" + this.name + "'.", e);
        }
    }

    public static final class Builder
    {
        private PreparedStatement preparedStatement;
        private String name;

        private Builder()
        {
            this.preparedStatement = null;
            this.name = null;
        }

        public Builder withPreparedStatement(PreparedStatement preparedStatement)
        {
            this.preparedStatement = ArgumentCheck.nonNull(preparedStatement);
            return this;
        }

        public Builder withName(String name)
        {
            this.name = name;
            return this;
        }

        public RowSourceResultSet build()
        {
            return new RowSourceResultSet(this);
        }
    }
}
