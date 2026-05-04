package app.babylon.table.dsl;

import java.util.Map;
import java.util.Set;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.selection.RowPredicate;

public interface ConditionExpression
{
    RowPredicate prepare(Map<ColumnName, Column> columnsByName);

    Set<ColumnName> columnNames();

    String toDsl();

    default ConditionExpression and(ConditionExpression other)
    {
        return new LogicalCondition(this, LogicalCondition.Operator.AND, other);
    }

    default ConditionExpression or(ConditionExpression other)
    {
        return new LogicalCondition(this, LogicalCondition.Operator.OR, other);
    }
}
