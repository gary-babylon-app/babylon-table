package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.text.Strings;

public class TransformAbs extends TransformBase
{
    public static final String FUNCTION_NAME = "Abs";

    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final ColumnName conditionColumnName;

    private TransformAbs(ColumnName x)
    {
        this(x, null);
    }

    private TransformAbs(ColumnName x, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(x);
        this.newColumnName = newColumnName;
        this.conditionColumnName = null;
    }

    private TransformAbs(Builder builder)
    {
        super(FUNCTION_NAME);
        this.columnName = builder.columnName;
        this.newColumnName = builder.newColumnName;
        this.conditionColumnName = builder.conditionColumnName;
    }

    public static TransformAbs of(ColumnName columnName)
    {
        if (columnName == null)
        {
            return null;
        }
        return new TransformAbs(columnName);
    }

    public static TransformAbs of(ColumnName columnName, ColumnName newColumnName)
    {
        if (columnName == null || newColumnName == null)
        {
            return null;
        }
        return new TransformAbs(columnName, newColumnName);
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public ColumnName effectiveNewColumnName()
    {
        return this.newColumnName == null ? this.columnName : this.newColumnName;
    }

    public ColumnName conditionColumnName()
    {
        return this.conditionColumnName;
    }

    public static Builder builder(ColumnName columnName)
    {
        return new Builder(columnName);
    }

    public static TransformAbs of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        if (params.length == 1)
        {
            String s = params[0];
            if (Strings.isEmpty(s) || s.length() < FUNCTION_NAME.length() + 3 || !s.startsWith(FUNCTION_NAME + "(")
                    || !s.endsWith(")"))
            {
                return null;
            }
            s = s.substring(FUNCTION_NAME.length() + 1, s.length() - 1);
            ColumnName columnName = ColumnName.parse(s);
            return (columnName == null) ? null : new TransformAbs(columnName);
        }
        if (params.length >= 2)
        {
            ColumnName columnName = ColumnName.parse(params[0]);
            ColumnName newColumnName = ColumnName.parse(params[1]);
            return of(columnName, newColumnName);
        }
        return null;
    }

    public ColumnObject<BigDecimal> apply(Column column)
    {
        if (column == null)
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;

        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(bd.abs(MathContext.DECIMAL64));
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

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;

        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(shouldAbs(conditionColumn, i) ? bd.abs(MathContext.DECIMAL64) : bd);
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
        Column column = columnsByName.get(columnName);
        if (column == null)
        {
            return;
        }
        ColumnObject<BigDecimal> transformed = this.conditionColumnName == null
                ? apply(column)
                : apply(column, requireBoolean(columnsByName.get(this.conditionColumnName)));
        if (transformed != null)
        {
            columnsByName.put(effectiveNewColumnName(), transformed);
        }
    }

    private boolean shouldAbs(ColumnBoolean conditionColumn, int row)
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

    @Override
    public String toString()
    {
        return FUNCTION_NAME + "(" + Arrays.stream(new ColumnName[]
        {columnName}).map(ColumnName::toString).collect(Collectors.joining(",")) + ")";
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

        public TransformAbs build()
        {
            return new TransformAbs(this);
        }
    }
}
