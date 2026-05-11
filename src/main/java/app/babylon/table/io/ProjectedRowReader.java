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

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;

public final class ProjectedRowReader
{
    private static final int EMPTY_ROW_LIMIT = 1;

    private final RowStreamMarkable rows;
    private final RowProjected projectedRow;
    private final Predicate<Row> rowFilter;
    private final ColumnDefinition[] columns;
    private Row current;
    private int emptyRowCount;

    private ProjectedRowReader(Builder builder)
    {
        HeaderDetection headerDetection = ArgumentCheck.nonNull(builder.headerDetection);
        this.rows = ArgumentCheck.nonNull(builder.rows);
        ColumnName[] projectedColumnNames = projectedColumnNames(headerDetection, builder.columnRenames);
        this.columns = columnDefinitions(headerDetection, projectedColumnNames, builder.nativeColumnTypes,
                builder.columnTypes);
        this.projectedRow = builder.stripping
                ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                : new RowProjectedDefault(headerDetection.getSelectedPositions());
        this.rowFilter = builder.rowFilter == null ? null : builder.rowFilter.bind(projectedColumnNames);
        this.current = null;
        this.emptyRowCount = 0;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public ColumnDefinition[] columns()
    {
        return Arrays.copyOf(this.columns, this.columns.length);
    }

    public boolean next() throws IOException
    {
        if (this.rowFilter == null)
        {
            while (this.rows.next())
            {
                Row row = this.projectedRow.with(this.rows.current());
                if (isEndOfTable(row))
                {
                    this.current = null;
                    return false;
                }
                if (!row.isEmpty())
                {
                    this.current = row;
                    return true;
                }
            }
            this.current = null;
            return false;
        }
        while (this.rows.next())
        {
            Row row = this.projectedRow.with(this.rows.current());
            if (isEndOfTable(row))
            {
                this.current = null;
                return false;
            }
            if (row.isEmpty())
            {
                continue;
            }
            if (this.rowFilter.test(row))
            {
                this.current = row;
                return true;
            }
        }
        this.current = null;
        return false;
    }

    private boolean isEndOfTable(Row row)
    {
        if (!row.isEmpty())
        {
            this.emptyRowCount = 0;
            return false;
        }
        ++this.emptyRowCount;
        return this.emptyRowCount >= EMPTY_ROW_LIMIT;
    }

    public Row current()
    {
        return ArgumentCheck.nonNull(this.current, "current row is not available until next() succeeds");
    }

    private static ColumnName[] projectedColumnNames(HeaderDetection headerDetection,
            Map<ColumnName, ColumnName> columnRenames)
    {
        ColumnName[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnName[] columnNames = new ColumnName[selectedHeaders.length];
        for (int i = 0; i < selectedHeaders.length; ++i)
        {
            ColumnName originalColumnName = selectedHeaders[i];
            columnNames[i] = columnRenames.getOrDefault(originalColumnName, originalColumnName);
        }
        return columnNames;
    }

    private static ColumnDefinition[] columnDefinitions(HeaderDetection headerDetection,
            ColumnName[] projectedColumnNames, Map<ColumnName, Column.Type> nativeColumnTypes,
            Map<ColumnName, Column.Type> columnTypes)
    {
        ColumnName[] selectedHeaders = headerDetection.getSelectedHeaders();
        ColumnDefinition[] definitions = new ColumnDefinition[selectedHeaders.length];
        for (int i = 0; i < selectedHeaders.length; ++i)
        {
            Column.Type type = nativeColumnTypes.get(selectedHeaders[i]);
            if (type == null)
            {
                type = columnTypes.get(selectedHeaders[i]);
            }
            definitions[i] = new ColumnDefinition(projectedColumnNames[i], type);
        }
        return definitions;
    }

    public static HeaderDetection headerDetection(ColumnDefinition[] columnDefinitions, Set<ColumnName> selectedColumns)
    {
        ColumnDefinition[] definitions = ArgumentCheck.nonNull(columnDefinitions);
        if (definitions.length == 0)
        {
            return new HeaderDetection(new ColumnName[0]);
        }
        List<ColumnName> selectedHeaders = new ArrayList<>();
        List<Integer> selectedPositions = new ArrayList<>();
        ColumnName[] headers = new ColumnName[definitions.length];
        for (int i = 0; i < definitions.length; ++i)
        {
            ColumnName columnName = definitions[i].name();
            headers[i] = columnName;
            if (selectedColumns == null || selectedColumns.isEmpty() || selectedColumns.contains(columnName))
            {
                selectedHeaders.add(columnName);
                selectedPositions.add(i);
            }
        }
        return new HeaderDetection(headers, false, selectedHeaders.toArray(new ColumnName[selectedHeaders.size()]),
                toIntArray(selectedPositions));
    }

    private static int[] toIntArray(List<Integer> values)
    {
        int[] x = new int[values.size()];
        for (int i = 0; i < values.size(); ++i)
        {
            x[i] = values.get(i);
        }
        return x;
    }

    public static final class Builder
    {
        private RowStreamMarkable rows;
        private HeaderDetection headerDetection;
        private Map<ColumnName, ColumnName> columnRenames;
        private Map<ColumnName, Column.Type> nativeColumnTypes;
        private Map<ColumnName, Column.Type> columnTypes;
        private RowFilter rowFilter;
        private boolean stripping;

        private Builder()
        {
            this.rows = null;
            this.headerDetection = null;
            this.columnRenames = new LinkedHashMap<>();
            this.nativeColumnTypes = new LinkedHashMap<>();
            this.columnTypes = new LinkedHashMap<>();
            this.rowFilter = null;
            this.stripping = true;
        }

        public Builder withRows(RowStreamMarkable rows)
        {
            this.rows = ArgumentCheck.nonNull(rows);
            return this;
        }

        public Builder withHeaderDetection(HeaderDetection headerDetection)
        {
            this.headerDetection = ArgumentCheck.nonNull(headerDetection);
            return this;
        }

        public Builder withColumnRenames(Map<ColumnName, ColumnName> columnRenames)
        {
            this.columnRenames = Is.empty(columnRenames) ? new LinkedHashMap<>() : new LinkedHashMap<>(columnRenames);
            return this;
        }

        public Builder withColumnTypes(Map<ColumnName, Column.Type> columnTypes)
        {
            this.columnTypes = Is.empty(columnTypes) ? new LinkedHashMap<>() : new LinkedHashMap<>(columnTypes);
            return this;
        }

        public Builder withNativeColumnTypes(Map<ColumnName, Column.Type> nativeColumnTypes)
        {
            this.nativeColumnTypes = Is.empty(nativeColumnTypes)
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(nativeColumnTypes);
            return this;
        }

        public Builder withRowFilter(RowFilter rowFilter)
        {
            this.rowFilter = rowFilter;
            return this;
        }

        public Builder withStripping(boolean stripping)
        {
            this.stripping = stripping;
            return this;
        }

        public ProjectedRowReader build()
        {
            return new ProjectedRowReader(this);
        }
    }
}
