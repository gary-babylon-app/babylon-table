package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

public class TransformToDouble extends TransformBase
{
    public static final String FUNCTION_NAME = "ToDouble";

    private final TransformToPrimitive delegate;

    public TransformToDouble(ColumnName columnName)
    {
        this(columnName, columnName);
    }

    public TransformToDouble(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.delegate = new TransformToPrimitive(ArgumentCheck.nonNull(existingColumnName),
                ArgumentCheck.nonNull(newColumnName), ColumnTypes.DOUBLE, TransformParseMode.ONLY_ONE_IN);
    }

    public static TransformToDouble of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return new TransformToDouble(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public ColumnDouble apply(Column x)
    {
        return (ColumnDouble) this.delegate.apply(x);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        this.delegate.apply(columnsByName);
    }
}
