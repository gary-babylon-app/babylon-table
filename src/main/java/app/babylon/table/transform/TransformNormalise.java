package app.babylon.table.transform;

import java.math.BigDecimal;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformNormalise extends TransformBase
{
    public static final String FUNCTION_NAME = "Normalise";

    private final ColumnName columnName;
    private final ColumnName newColumnName;

    private TransformNormalise(ColumnName columnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = newColumnName;
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

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public ColumnName effectiveNewColumnName()
    {
        return this.newColumnName == null ? this.columnName : this.newColumnName;
    }

    public ColumnObject<BigDecimal> apply(Column column)
    {
        if (column == null)
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;
        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                newColumn.add(normalise(oldColumn.get(i)));
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnName);
        if (column != null)
        {
            columnsByName.put(effectiveNewColumnName(), apply(column));
        }
    }

    public static BigDecimal normalise(BigDecimal value)
    {
        BigDecimal stripped = ArgumentCheck.nonNull(value).stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
