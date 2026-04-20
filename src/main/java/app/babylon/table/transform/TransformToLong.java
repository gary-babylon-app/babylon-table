package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

public class TransformToLong extends TransformBase
{
    public static final String FUNCTION_NAME = "ToLong";

    private final TransformToPrimitive delegate;

    public TransformToLong(ColumnName columnName)
    {
        this(columnName, columnName);
    }

    public TransformToLong(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.delegate = new TransformToPrimitive(ArgumentCheck.nonNull(existingColumnName),
                ArgumentCheck.nonNull(newColumnName), ColumnTypes.LONG);
    }

    public static TransformToLong of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return new TransformToLong(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public ColumnLong apply(Column x)
    {
        return (ColumnLong) this.delegate.apply(x);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        this.delegate.apply(columnsByName);
    }
}
