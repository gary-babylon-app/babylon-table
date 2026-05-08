package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

public class TransformCopy extends TransformBase implements TransformToColumn
{
    public static final String FUNCTION_NAME = "Copy";

    public ColumnName columnToCopy;
    public ColumnName newCopyName;

    public TransformCopy(ColumnName columnToCopy, ColumnName newCopyName)
    {
        super(FUNCTION_NAME);
        this.columnToCopy = ArgumentCheck.nonNull(columnToCopy);
        this.newCopyName = ArgumentCheck.nonNull(newCopyName);
    }

    public static TransformCopy of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public static TransformCopy of(ColumnName columnToCopy, ColumnName newCopyName)
    {
        return new TransformCopy(columnToCopy, newCopyName);
    }

    public ColumnName columnToCopy()
    {
        return this.columnToCopy;
    }

    public ColumnName newCopyName()
    {
        return this.newCopyName;
    }

    @Override
    public ColumnName outputColumnName()
    {
        return this.newCopyName;
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        return List.of(this.columnToCopy);
    }

    @Override
    public Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        Column column = columnsByName.get(this.columnToCopy);
        return column == null ? null : column.copy(this.newCopyName);
    }
}
