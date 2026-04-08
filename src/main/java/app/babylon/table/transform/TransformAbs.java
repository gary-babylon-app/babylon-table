package app.babylon.table.transform;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformAbs extends TransformBase
{
    public static String FUNCTION_NAME = "Abs";

    private final ColumnName columnName;
    private final ColumnName newColumnName;

    public TransformAbs(ColumnName x)
    {
        this(x, x);
    }
    public TransformAbs(ColumnName x, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentChecks.nonNull(x);
        this.newColumnName = (newColumnName == null) ? x : newColumnName;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(columnName);
        if (column == null)
        {
            return;
        }
        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;

        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(newColumnName);
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            BigDecimal bd = oldColumn.get(i);
            if (bd != null)
            {
                newColumn.add(bd.abs(MathContext.DECIMAL64));
            } else
            {
                newColumn.addNull();
            }
        }
        columnsByName.put(newColumnName, newColumn.build());
    }

}
