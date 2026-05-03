package app.babylon.table.transform;

import java.math.BigDecimal;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;

public class TransformNormalise extends TransformDecimalUnary
{
    public static final String FUNCTION_NAME = "Normalise";

    private TransformNormalise(ColumnName columnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, columnName, newColumnName, null);
    }

    public static TransformNormalise of(ColumnName columnName)
    {
        return columnName == null ? null : new TransformNormalise(columnName, null);
    }

    public static TransformNormalise of(ColumnName columnName, ColumnName newColumnName)
    {
        return columnName == null || newColumnName == null ? null : new TransformNormalise(columnName, newColumnName);
    }

    public static TransformNormalise of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        if (params.length >= 2)
        {
            return of(ColumnName.parse(params[0]), ColumnName.parse(params[1]));
        }
        return of(ColumnName.parse(params[0]));
    }

    @Override
    protected BigDecimal transform(BigDecimal value, int row)
    {
        return normalise(value);
    }

    public static BigDecimal normalise(BigDecimal value)
    {
        BigDecimal stripped = ArgumentCheck.nonNull(value).stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
