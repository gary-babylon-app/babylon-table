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
        this(columnName, null);
    }

    public TransformToLong(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.delegate = TransformToPrimitive.builder(ColumnTypes.LONG, ArgumentCheck.nonNull(existingColumnName))
                .withNewColumnName(newColumnName).build();
    }

    public static TransformToLong of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public static TransformToLong of(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return new TransformToLong(existingColumnName, newColumnName);
    }

    public TransformToPrimitive delegate()
    {
        return this.delegate;
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
