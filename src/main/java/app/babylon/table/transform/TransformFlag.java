package app.babylon.table.transform;

import java.util.Collection;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.dsl.ConditionExpression;
import app.babylon.table.selection.RowPredicate;

public class TransformFlag extends TransformBase implements TransformToColumn
{
    public static final String FUNCTION_NAME = "Flag";

    private final ConditionExpression condition;
    private final ColumnName newColumnName;

    private TransformFlag(Builder builder)
    {
        super(FUNCTION_NAME);
        this.condition = ArgumentCheck.nonNull(builder.condition);
        this.newColumnName = ArgumentCheck.nonNull(builder.newColumnName);
    }

    public static Builder builder(ConditionExpression condition)
    {
        return new Builder(condition);
    }

    public ConditionExpression condition()
    {
        return this.condition;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    @Override
    public ColumnName outputColumnName()
    {
        return this.newColumnName;
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        return this.condition.columnNames();
    }

    @Override
    public Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        RowPredicate predicate = this.condition.prepare(columnsByName);
        ColumnBoolean.Builder builder = ColumnBoolean.builder(this.newColumnName);
        for (int row = 0; row < rowCount; ++row)
        {
            builder.add(predicate.test(row));
        }
        return builder.build();
    }

    public static final class Builder
    {
        private final ConditionExpression condition;
        private ColumnName newColumnName;

        private Builder(ConditionExpression condition)
        {
            this.condition = ArgumentCheck.nonNull(condition);
        }

        public Builder withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public TransformFlag build()
        {
            return new TransformFlag(this);
        }
    }
}
