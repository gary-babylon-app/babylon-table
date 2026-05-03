package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.selection.RowPredicate;

public class TransformFlag extends TransformBase
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
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        RowPredicate predicate = this.condition.prepare(columnsByName);
        int rowCount = rowCount(columnsByName);
        ColumnBoolean.Builder builder = ColumnBoolean.builder(this.newColumnName);
        for (int row = 0; row < rowCount; ++row)
        {
            builder.add(predicate.test(row));
        }
        columnsByName.put(this.newColumnName, builder.build());
    }

    private int rowCount(Map<ColumnName, Column> columnsByName)
    {
        for (ColumnName columnName : this.condition.columnNames())
        {
            Column column = columnsByName.get(columnName);
            if (column != null)
            {
                return column.size();
            }
        }
        return 0;
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
