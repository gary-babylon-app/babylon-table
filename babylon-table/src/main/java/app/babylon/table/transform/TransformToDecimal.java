package app.babylon.table.transform;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.text.BigDecimals;
import app.babylon.text.Split;
import app.babylon.text.Strings;

public class TransformToDecimal extends TransformStringColumnsBase<BigDecimal>
{
    public static final String FUNCTION_NAME = "ToDecimal";
    private final Function<CharSequence, BigDecimal> parser;

    private TransformToDecimal(ColumnName... columnNames)
    {
        super(FUNCTION_NAME, ArgumentCheck.nonEmpty(columnNames), null);
        this.parser = TransformToDecimal::parseDecimal;
    }

    private TransformToDecimal(ColumnName columnName, ColumnName newColumnName)
    {
        this(columnName, newColumnName, null);
    }

    public static TransformToDecimal of(ColumnName... columnNames)
    {
        if (Is.empty(columnNames))
        {
            return null;
        }
        for (ColumnName columnName : columnNames)
        {
            if (columnName == null)
            {
                return null;
            }
        }
        return new TransformToDecimal(columnNames);
    }

    public static TransformToDecimal of(String... params)
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
            return new TransformToDecimal(ColumnName.of(Split.commaSeparatedParams(s)));
        }
        if (params.length >= 2)
        {
            ColumnName columnName = ColumnName.parse(params[0]);
            ColumnName newColumnName = ColumnName.parse(params[1]);
            return new TransformToDecimal(columnName, newColumnName);
        }
        return null;
    }

    @Override
    protected Column.Type type()
    {
        return ColumnTypes.DECIMAL;
    }

    @Override
    protected Function<CharSequence, BigDecimal> createParser(Column[] selectedColumns)
    {
        return this.parser;
    }

    static BigDecimal parseDecimal(CharSequence s)
    {
        BigDecimal bd = BigDecimals.parse(s);
        if (bd == null)
        {
            bd = BigDecimals.extract(s);
        }
        return bd;
    }

    private TransformToDecimal(ColumnName columnName, ColumnName newColumnName,
            Function<CharSequence, BigDecimal> parser)
    {
        super(FUNCTION_NAME, new ColumnName[]
        {ArgumentCheck.nonNull(columnName)}, new ColumnName[]
        {ArgumentCheck.nonNull(newColumnName)});
        this.parser = resolveParser(parser, TransformToDecimal::parseDecimal);
    }

    public ColumnObject<BigDecimal> apply(Column x)
    {
        Column[] y = new Column[]
        {x};
        Column[] z = apply(y);
        if (z != null && z.length > 0)
        {
            @SuppressWarnings("unchecked")
            ColumnObject<BigDecimal> result = (ColumnObject<BigDecimal>) z[0];
            return result;
        }
        return null;
    }

    private Column[] apply(Column[] x)
    {
        return transformColumns(x);
    }

    @Override
    public String toString()
    {
        return FUNCTION_NAME + "("
                + Arrays.stream(columnNames).map(ColumnName::toString).collect(Collectors.joining(",")) + ")";
    }
}
