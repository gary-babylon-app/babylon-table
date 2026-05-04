package app.babylon.table.dsl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.selection.RowPredicate;

public final class LogicalCondition implements ConditionExpression
{
    public enum Operator
    {
        AND, OR
    }

    private final ConditionExpression left;
    private final Operator operator;
    private final ConditionExpression right;

    public LogicalCondition(ConditionExpression left, Operator operator, ConditionExpression right)
    {
        this.left = ArgumentCheck.nonNull(left);
        this.operator = ArgumentCheck.nonNull(operator);
        this.right = ArgumentCheck.nonNull(right);
    }

    public ConditionExpression left()
    {
        return this.left;
    }

    public Operator operator()
    {
        return this.operator;
    }

    public ConditionExpression right()
    {
        return this.right;
    }

    @Override
    public RowPredicate prepare(Map<ColumnName, Column> columnsByName)
    {
        RowPredicate leftPredicate = this.left.prepare(columnsByName);
        RowPredicate rightPredicate = this.right.prepare(columnsByName);
        return switch (this.operator)
        {
            case AND -> row -> leftPredicate.test(row) && rightPredicate.test(row);
            case OR -> row -> leftPredicate.test(row) || rightPredicate.test(row);
        };
    }

    @Override
    public Set<ColumnName> columnNames()
    {
        Set<ColumnName> names = new HashSet<>(this.left.columnNames());
        names.addAll(this.right.columnNames());
        return names;
    }

    @Override
    public String toDsl()
    {
        return this.left.toDsl() + " " + this.operator.name().toLowerCase() + " " + this.right.toDsl();
    }
}
