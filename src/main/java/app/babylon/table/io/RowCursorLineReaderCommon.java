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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.TableException;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;

abstract class RowCursorLineReaderCommon implements RowCursor
{
    private final LineReader lineReader;
    private final RowStreamMarkable rowStream;
    private final HeaderStrategy headerStrategy;
    private final RowProjected projectedRow;
    private final Predicate<Row> rowFilter;
    private final ColumnDefinition[] columnDefinitions;
    private final boolean stripping;
    private Row currentRow;

    protected RowCursorLineReaderCommon(LineReader lineReader, BuilderBase<?> builder)
    {
        this.lineReader = ArgumentCheck.nonNull(lineReader);
        this.rowStream = new RowStreamBuffered(this.lineReader);
        this.headerStrategy = ArgumentCheck.nonNull(builder.headerStrategy);
        this.stripping = builder.stripping;
        try
        {
            HeaderDetection headerDetection = this.headerStrategy.detect(this.rowStream, builder.selectedColumns);
            this.projectedRow = builder.createProjectedRow(headerDetection);
            ColumnName[] projectedColumnNames = builder
                    .createProjectedColumnNames(headerDetection.getSelectedHeaders());
            this.columnDefinitions = builder.createColumnDefinitions(headerDetection.getSelectedHeaders(),
                    projectedColumnNames);
            this.rowFilter = builder.rowFilter == null ? null : builder.rowFilter.bind(projectedColumnNames);
            this.currentRow = null;
            this.rowStream.reset();
        }
        catch (IOException e)
        {
            try
            {
                this.lineReader.close();
            }
            catch (IOException closeException)
            {
                e.addSuppressed(closeException);
            }
            throw new TableException("Failed to prepare row cursor.", e);
        }
    }

    @Override
    public ColumnDefinition[] columns()
    {
        return Arrays.copyOf(this.columnDefinitions, this.columnDefinitions.length);
    }

    @Override
    public boolean next()
    {
        try
        {
            if (this.rowFilter == null)
            {
                if (this.rowStream.next())
                {
                    this.currentRow = this.projectedRow.with(this.rowStream.current());
                    return true;
                }
                this.currentRow = null;
                return false;
            }
            while (this.rowStream.next())
            {
                Row row = this.projectedRow.with(this.rowStream.current());
                if (this.rowFilter.test(row))
                {
                    this.currentRow = row;
                    return true;
                }
            }
            this.currentRow = null;
            return false;
        }
        catch (IOException e)
        {
            throw new TableException("Failed to read row.", e);
        }
    }

    @Override
    public Row current()
    {
        return ArgumentCheck.nonNull(this.currentRow, "current row is not available until next() succeeds");
    }

    @Override
    public void close() throws IOException
    {
        this.lineReader.close();
    }

    protected HeaderStrategy getHeaderStrategy()
    {
        return this.headerStrategy;
    }

    protected boolean isStripping()
    {
        return this.stripping;
    }

    static abstract class BuilderBase<B extends BuilderBase<B>>
    {
        final Map<ColumnName, Column.Type> explicitColumnTypes;
        final Set<ColumnName> selectedColumns;
        final Map<ColumnName, ColumnName> columnRenames;
        final Set<ColumnName> renamedTargets;
        HeaderStrategy headerStrategy;
        RowFilter rowFilter;
        boolean stripping;

        protected BuilderBase()
        {
            this.explicitColumnTypes = new LinkedHashMap<>();
            this.selectedColumns = new LinkedHashSet<>();
            this.columnRenames = new LinkedHashMap<>();
            this.renamedTargets = new HashSet<>();
            this.headerStrategy = new HeaderStrategyAuto(HeaderStrategy.DEFAULT_SCAN_LIMIT);
            this.rowFilter = null;
            this.stripping = true;
        }

        public B withHeaderStrategy(HeaderStrategy headerStrategy)
        {
            this.headerStrategy = ArgumentCheck.nonNull(headerStrategy);
            return self();
        }

        public B withSelectedColumn(ColumnName columnName)
        {
            this.selectedColumns.add(ArgumentCheck.nonNull(columnName));
            return self();
        }

        public B withSelectedColumns(ColumnName... columnNames)
        {
            if (!Is.empty(columnNames))
            {
                this.selectedColumns.addAll(Arrays.asList(columnNames));
            }
            return self();
        }

        public B withSelectedColumns(Collection<ColumnName> columnNames)
        {
            if (!Is.empty(columnNames))
            {
                this.selectedColumns.addAll(columnNames);
            }
            return self();
        }

        public B withColumnRename(ColumnName original, ColumnName newName)
        {
            ColumnName originalChecked = ArgumentCheck.nonNull(original);
            ColumnName newNameChecked = ArgumentCheck.nonNull(newName);
            if (this.columnRenames.containsKey(originalChecked))
            {
                throw new IllegalArgumentException("Rename failed, column " + originalChecked + " already renamed");
            }
            if (this.renamedTargets.contains(newNameChecked))
            {
                throw new IllegalArgumentException("Rename failed, target column " + newNameChecked + " already used");
            }
            this.columnRenames.put(originalChecked, newNameChecked);
            this.renamedTargets.add(newNameChecked);
            return self();
        }

        public B withColumnRenames(Map<ColumnName, ColumnName> renames)
        {
            if (!Is.empty(renames))
            {
                for (Map.Entry<ColumnName, ColumnName> entry : renames.entrySet())
                {
                    withColumnRename(entry.getKey(), entry.getValue());
                }
            }
            return self();
        }

        public B withRowFilter(RowFilter rowFilter)
        {
            this.rowFilter = ArgumentCheck.nonNull(rowFilter);
            return self();
        }

        public B withStripping(boolean stripping)
        {
            this.stripping = stripping;
            return self();
        }

        public B withColumnType(ColumnName columnName, Column.Type columnType)
        {
            this.explicitColumnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
            return self();
        }

        public B withColumnTypes(Map<ColumnName, Column.Type> columnTypes)
        {
            if (!Is.empty(columnTypes))
            {
                for (Map.Entry<ColumnName, Column.Type> entry : columnTypes.entrySet())
                {
                    withColumnType(entry.getKey(), entry.getValue());
                }
            }
            return self();
        }

        protected final ColumnName[] createProjectedColumnNames(ColumnName[] sourceHeaders)
        {
            ColumnName[] columnNames = new ColumnName[sourceHeaders.length];
            for (int i = 0; i < sourceHeaders.length; ++i)
            {
                ColumnName originalColumnName = sourceHeaders[i];
                columnNames[i] = this.columnRenames.getOrDefault(originalColumnName, originalColumnName);
            }
            return columnNames;
        }

        protected final ColumnDefinition[] createColumnDefinitions(ColumnName[] sourceHeaders,
                ColumnName[] projectedNames)
        {
            ColumnDefinition[] definitions = new ColumnDefinition[sourceHeaders.length];
            for (int i = 0; i < sourceHeaders.length; ++i)
            {
                ColumnName sourceColumnName = sourceHeaders[i];
                Column.Type explicitType = this.explicitColumnTypes.get(sourceColumnName);
                definitions[i] = new ColumnDefinition(projectedNames[i], explicitType);
            }
            return definitions;
        }

        protected final RowProjected createProjectedRow(HeaderDetection headerDetection)
        {
            return this.stripping
                    ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                    : new RowProjectedDefault(headerDetection.getSelectedPositions());
        }

        protected final void copyCommonTo(BuilderBase<?> copy)
        {
            copy.headerStrategy = this.headerStrategy;
            copy.rowFilter = this.rowFilter;
            copy.stripping = this.stripping;
            copy.explicitColumnTypes.putAll(this.explicitColumnTypes);
            copy.selectedColumns.addAll(this.selectedColumns);
            copy.columnRenames.putAll(this.columnRenames);
            copy.renamedTargets.addAll(this.renamedTargets);
        }

        protected abstract B self();
    }
}
