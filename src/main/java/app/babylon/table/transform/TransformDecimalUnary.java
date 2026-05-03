package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.math.BigDecimal;
import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

abstract class TransformDecimalUnary extends TransformBase
{
    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final ColumnName conditionColumnName;

    TransformDecimalUnary(String name, ColumnName columnName, ColumnName newColumnName, ColumnName conditionColumnName)
    {
        super(name);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = newColumnName;
        this.conditionColumnName = conditionColumnName;
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

    public ColumnName conditionColumnName()
    {
        return this.conditionColumnName;
    }

    public ColumnObject<BigDecimal> apply(Column column)
    {
        return apply(column, null, false);
    }

    public ColumnObject<BigDecimal> apply(Column column, ColumnBoolean conditionColumn)
    {
        return apply(column, conditionColumn, false);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnName);
        if (column == null)
        {
            return;
        }
        ColumnBoolean conditionColumn = requireBoolean(columnsByName.get(this.conditionColumnName));
        if (this.conditionColumnName != null && conditionColumn == null)
        {
            return;
        }
        ColumnObject<BigDecimal> transformed = this.conditionColumnName == null
                ? apply(column)
                : apply(column, conditionColumn);
        if (transformed != null)
        {
            columnsByName.put(effectiveNewColumnName(), transformed);
        }
    }

    protected boolean shouldApply(ColumnBoolean conditionColumn, int row)
    {
        return conditionColumn == null
                || (row < conditionColumn.size() && conditionColumn.isSet(row) && conditionColumn.get(row));
    }

    protected ColumnBoolean requireBoolean(Column column)
    {
        if (this.conditionColumnName == null)
        {
            return null;
        }
        if (column instanceof ColumnBoolean booleanColumn)
        {
            return booleanColumn;
        }
        if (column == null)
        {
            return null;
        }
        throw new IllegalArgumentException(
                getName() + " when requires Boolean column '" + column.getName() + "' but found " + column.getType());
    }

    protected void requireDecimal(Column column)
    {
        if (!ColumnTypes.DECIMAL.equals(column.getType()))
        {
            throw new IllegalArgumentException(
                    getName() + " requires Decimal column '" + column.getName() + "' but found " + column.getType());
        }
    }

    protected abstract BigDecimal transform(BigDecimal value, int row);

    private ColumnObject<BigDecimal> apply(Column column, ColumnBoolean conditionColumn, boolean conditional)
    {
        if (column == null || (conditional && conditionColumn == null))
        {
            return null;
        }
        requireDecimal(column);

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;
        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                BigDecimal value = oldColumn.get(i);
                newColumn.add(shouldApply(conditionColumn, i) ? transform(value, i) : value);
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
    }
}
