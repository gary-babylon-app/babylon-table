package app.babylon.table.dsl;

import java.util.Arrays;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

public final class Conditions
{
    private Conditions()
    {
    }

    public static ColumnCondition column(String columnName)
    {
        return column(ColumnName.of(columnName));
    }

    public static ColumnCondition column(ColumnName columnName)
    {
        return new ColumnCondition(ArgumentCheck.nonNull(columnName));
    }

    public static final class ColumnCondition
    {
        private final ColumnName columnName;

        private ColumnCondition(ColumnName columnName)
        {
            this.columnName = columnName;
        }

        public ComparisonCondition is(String value)
        {
            return eq(value);
        }

        public ComparisonCondition eq(String value)
        {
            return comparison(Column.Operator.EQUAL, value);
        }

        public ComparisonCondition ne(String value)
        {
            return comparison(Column.Operator.NOT_EQUAL, value);
        }

        public ComparisonCondition gt(String value)
        {
            return comparison(Column.Operator.GREATER_THAN, value);
        }

        public ComparisonCondition gte(String value)
        {
            return comparison(Column.Operator.GREATER_THAN_OR_EQUAL, value);
        }

        public ComparisonCondition lt(String value)
        {
            return comparison(Column.Operator.LESS_THAN, value);
        }

        public ComparisonCondition lte(String value)
        {
            return comparison(Column.Operator.LESS_THAN_OR_EQUAL, value);
        }

        public ComparisonCondition in(String... values)
        {
            return comparison(Column.Operator.IN, values);
        }

        public ComparisonCondition notIn(String... values)
        {
            return comparison(Column.Operator.NOT_IN, values);
        }

        private ComparisonCondition comparison(Column.Operator operator, String... values)
        {
            String[] copy = values == null ? new String[0] : Arrays.copyOf(values, values.length);
            if (copy.length == 0)
            {
                throw new IllegalArgumentException("Condition requires at least one value.");
            }
            for (String value : copy)
            {
                ArgumentCheck.nonNull(value, "Condition values must not be null.");
            }
            return new ComparisonCondition(this.columnName, operator, copy);
        }
    }
}
