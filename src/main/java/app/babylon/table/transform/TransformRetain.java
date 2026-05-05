package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

public class TransformRetain extends TransformBase
{
    public static final String FUNCTION_NAME = "Retain";

    private final ColumnName[] columnNames;

    public TransformRetain(ColumnName... columnNames)
    {
        super(FUNCTION_NAME);
        this.columnNames = Arrays.copyOf(ArgumentCheck.nonEmpty(columnNames), columnNames.length);
    }

    public static TransformRetain of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        return of(ColumnName.of(params));
    }

    public static TransformRetain of(ColumnName... columnNames)
    {
        return new TransformRetain(columnNames);
    }

    public ColumnName[] columnNames()
    {
        return Arrays.copyOf(this.columnNames, this.columnNames.length);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null)
        {
            return;
        }
        Column[] retainedColumns = getColumns(columnsByName, Arrays.asList(this.columnNames));
        columnsByName.clear();
        putColumns(columnsByName, retainedColumns);
    }
}
