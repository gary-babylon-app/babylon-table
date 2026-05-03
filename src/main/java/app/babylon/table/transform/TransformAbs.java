package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.stream.Collectors;

import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TransformAbs extends TransformDecimalUnary
{
    public static final String FUNCTION_NAME = "Abs";

    private TransformAbs(ColumnName x)
    {
        this(x, null);
    }

    private TransformAbs(ColumnName x, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, x, newColumnName, null);
    }

    private TransformAbs(Builder builder)
    {
        super(FUNCTION_NAME, builder.columnName, builder.newColumnName, builder.conditionColumnName);
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

    @Override
    protected BigDecimal transform(BigDecimal value, int row)
    {
        return value.abs(MathContext.DECIMAL64);
    }

    @Override
    public String toString()
    {
        return FUNCTION_NAME + "(" + Arrays.stream(new ColumnName[]
        {columnName()}).map(ColumnName::toString).collect(Collectors.joining(",")) + ")";
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
