package app.babylon.table.plans;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;
import app.babylon.table.TableColumnar;
import app.babylon.table.column.ColumnName;
import app.babylon.table.io.TableSink;

/**
 * Builder-style plan for exporting a {@link TableColumnar} to a configured
 * format and destination.
 */
public class TablePlanWrite
{
    private final Set<ColumnName> selectedColumns;
    private TableSink sink;

    public TablePlanWrite()
    {
        this.selectedColumns = new LinkedHashSet<>();
        this.sink = null;
    }

    public TablePlanWrite withSelectedColumn(ColumnName columnName)
    {
        this.selectedColumns.add(ArgumentCheck.nonNull(columnName));
        return this;
    }

    public TablePlanWrite withSelectedColumns(ColumnName... columnNames)
    {
        if (columnNames != null)
        {
            this.selectedColumns.addAll(Arrays.asList(columnNames));
        }
        return this;
    }

    public TablePlanWrite withSelectedColumns(Collection<ColumnName> columnNames)
    {
        return withSelectedColumns((Iterable<ColumnName>) columnNames);
    }

    public TablePlanWrite withSelectedColumns(Iterable<ColumnName> columnNames)
    {
        if (columnNames != null)
        {
            for (ColumnName columnName : columnNames)
            {
                this.selectedColumns.add(columnName);
            }
        }
        return this;
    }

    public ColumnName[] getSelectedColumns()
    {
        return this.selectedColumns.toArray(new ColumnName[0]);
    }

    public TablePlanWrite withSink(TableSink sink)
    {
        this.sink = ArgumentCheck.nonNull(sink);
        return this;
    }

    public TableSink getSink()
    {
        return this.sink;
    }

    public void execute(TableColumnar table)
    {
        TableSink checkedSink = ArgumentCheck.nonNull(this.sink, "sink");
        TableColumnar selectedTable = selectColumns(ArgumentCheck.nonNull(table));
        try
        {
            checkedSink.write(selectedTable);
        }
        catch (IOException e)
        {
            throw new TableException("Failed to write table to table sink '" + checkedSink.getName() + "'.", e);
        }
    }

    private TableColumnar selectColumns(TableColumnar table)
    {
        return this.selectedColumns.isEmpty() ? table : table.select(getSelectedColumns());
    }
}
