package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

public class TransformToInt extends TransformBase
{
    public static final String FUNCTION_NAME = "ToInt";

    private final TransformToPrimitive delegate;

    public TransformToInt(ColumnName columnName)
    {
        this(columnName, null);
    }

    public TransformToInt(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.delegate = new TransformToPrimitive(ArgumentCheck.nonNull(existingColumnName), newColumnName,
                ColumnTypes.INT);
    }

    public static TransformToInt of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public static TransformToInt of(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return new TransformToInt(existingColumnName, newColumnName);
    }

    public TransformToPrimitive delegate()
    {
        return this.delegate;
    }

    public ColumnInt apply(Column x)
    {
        return (ColumnInt) this.delegate.apply(x);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        this.delegate.apply(columnsByName);
    }
}
