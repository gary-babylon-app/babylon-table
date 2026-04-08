package app.babylon.table.transform;

import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.Is;

public class TransformCopy extends TransformBase
{
    public static final String FUNCTION_NAME = "Copy";

    public ColumnName columnToCopy;
    public ColumnName newCopyName;

    public TransformCopy(ColumnName columnToCopy, ColumnName newCopyName)
    {
        super(FUNCTION_NAME);
        this.columnToCopy = ArgumentChecks.nonNull(columnToCopy);
        this.newCopyName = ArgumentChecks.nonNull(newCopyName);
    }

    public static TransformCopy of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return new TransformCopy(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(columnToCopy);
        if (column != null)
        {
            columnsByName.put(newCopyName, column.copy(newCopyName));
        }
    }
}
