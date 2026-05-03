package app.babylon.table.transform;

import java.math.BigDecimal;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class TransformNegate extends TransformBase
{
    public static final String FUNCTION_NAME = "Negate";

    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final ColumnName conditionColumnName;

    private TransformNegate(Builder builder)
    {
        super(FUNCTION_NAME);
        this.columnName = builder.columnName;
        this.newColumnName = builder.newColumnName;
        this.conditionColumnName = builder.conditionColumnName;
    }

    public static TransformNegate of(ColumnName columnName)
    {
        return columnName == null ? null : builder(columnName).build();
    }

    public static TransformNegate of(ColumnName columnName, ColumnName newColumnName)
    {
        return columnName == null || newColumnName == null
                ? null
                : builder(columnName).withNewColumnName(newColumnName).build();
    }

    public static TransformNegate of(String... params)
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

    public static Builder builder(ColumnName columnName)
    {
        return new Builder(columnName);
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
        if (column == null)
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
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(bd.negate());
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
    }

    public ColumnObject<BigDecimal> apply(Column column, ColumnBoolean conditionColumn)
    {
        if (column == null || conditionColumn == null)
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
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(shouldNegate(conditionColumn, i) ? bd.negate() : bd);
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
            ColumnObject<BigDecimal> transformed = this.conditionColumnName == null
                    ? apply(column)
                    : apply(column, requireBoolean(columnsByName.get(this.conditionColumnName)));
            if (transformed != null)
            {
                columnsByName.put(effectiveNewColumnName(), transformed);
            }
        }
    }

    private boolean shouldNegate(ColumnBoolean conditionColumn, int row)
    {
        return row < conditionColumn.size() && conditionColumn.isSet(row) && conditionColumn.get(row);
    }

    private ColumnBoolean requireBoolean(Column column)
    {
        if (column instanceof ColumnBoolean booleanColumn)
        {
            return booleanColumn;
        }
        if (column == null)
        {
            return null;
        }
        throw new IllegalArgumentException(FUNCTION_NAME + " when requires Boolean column '" + column.getName()
                + "' but found " + column.getType());
    }

    private void requireDecimal(Column column)
    {
        if (!ColumnTypes.DECIMAL.equals(column.getType()))
        {
            throw new IllegalArgumentException(FUNCTION_NAME + " requires Decimal column '" + column.getName()
                    + "' but found " + column.getType());
        }
    }

    public static final class Builder
    {
        private final ColumnName columnName;
        private ColumnName newColumnName;
        private ColumnName conditionColumnName;

        private Builder(ColumnName columnName)
        {
            this.columnName = ArgumentCheck.nonNull(columnName);
        }

        public Builder withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public Builder when(ColumnName conditionColumnName)
        {
            this.conditionColumnName = ArgumentCheck.nonNull(conditionColumnName);
            return this;
        }

        public TransformNegate build()
        {
            return new TransformNegate(this);
        }
    }
}
