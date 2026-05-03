package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.selection.RowPredicate;

public final class ComparisonCondition implements ConditionExpression
{
    private final ColumnName columnName;
    private final Column.Operator operator;
    private final String[] values;

    public ComparisonCondition(ColumnName columnName, Column.Operator operator, String... values)
    {
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.operator = ArgumentCheck.nonNull(operator);
        this.values = values == null ? new String[0] : Arrays.copyOf(values, values.length);
    }

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public Column.Operator operator()
    {
        return this.operator;
    }

    public String[] values()
    {
        return Arrays.copyOf(this.values, this.values.length);
    }

    @Override
    public RowPredicate prepare(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnName);
        if (column == null)
        {
            throw new IllegalArgumentException("Unknown condition column '" + this.columnName + "'.");
        }
        return column.predicate(this.operator, this.values);
    }

    @Override
    public Set<ColumnName> columnNames()
    {
        return Set.of(this.columnName);
    }

    @Override
    public String toDsl()
    {
        return column(this.columnName) + " " + this.operator.preferredText() + " " + joinedValues();
    }

    private String joinedValues()
    {
        if (this.operator == Column.Operator.IN || this.operator == Column.Operator.NOT_IN)
        {
            return String.join(", ", this.values);
        }
        return this.values.length == 0 ? "" : this.values[0];
    }

    private static String column(ColumnName columnName)
    {
        return columnName.toString();
    }

}
