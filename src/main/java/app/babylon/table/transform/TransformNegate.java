package app.babylon.table.transform;

import java.math.BigDecimal;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;

public class TransformNegate extends TransformDecimalUnary
{
    public static final String FUNCTION_NAME = "Negate";

    private TransformNegate(Builder builder)
    {
        super(FUNCTION_NAME, builder.columnName, builder.newColumnName, builder.conditionColumnName);
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

    @Override
    protected BigDecimal transform(BigDecimal value, int row)
    {
        return value.negate();
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
