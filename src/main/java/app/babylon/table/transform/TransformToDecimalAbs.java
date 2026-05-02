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
import app.babylon.text.Strings;

public class TransformToDecimalAbs extends TransformStringColumnsBase<BigDecimal>
{
    public static final String FUNCTION_NAME = "ToDecimalAbs";
    private final Function<CharSequence, BigDecimal> parser;

    private TransformToDecimalAbs(ColumnName columnName)
    {
        super(FUNCTION_NAME, new ColumnName[]
        {ArgumentCheck.nonNull(columnName)}, null);
        this.parser = TransformToDecimalAbs::parseAbsDecimal;
    }

    private TransformToDecimalAbs(ColumnName columnName, ColumnName newColumnName)
    {
        this(columnName, newColumnName, null);
    }

    public static TransformToDecimalAbs of(ColumnName columnName)
    {
        return columnName == null ? null : new TransformToDecimalAbs(columnName);
    }

    public static TransformToDecimalAbs of(String... params)
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
            String[] columnNames = Strings.split(s);
            return columnNames.length == 1 ? of(ColumnName.of(columnNames[0])) : null;
        }
        if (params.length >= 2)
        {
            ColumnName columnName = ColumnName.parse(params[0]);
            ColumnName newColumnName = ColumnName.parse(params[1]);
            return of(columnName, newColumnName);
        }
        return null;
    }

    public static TransformToDecimalAbs of(ColumnName columnName, ColumnName newColumnName)
    {
        return new TransformToDecimalAbs(columnName, newColumnName);
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

    static BigDecimal parseAbsDecimal(CharSequence s)
    {
        BigDecimal bd = TransformToDecimal.parseDecimal(s);
        if (bd != null)
        {
            return bd.abs();
        }
        return null;
    }

    private TransformToDecimalAbs(ColumnName columnName, ColumnName newColumnName,
            Function<CharSequence, BigDecimal> parser)
    {
        super(FUNCTION_NAME, new ColumnName[]
        {ArgumentCheck.nonNull(columnName)}, newColumnNames(newColumnName));
        this.parser = resolveParser(parser, TransformToDecimalAbs::parseAbsDecimal);
    }

    private static ColumnName[] newColumnNames(ColumnName newColumnName)
    {
        return newColumnName == null ? null : new ColumnName[]
        {newColumnName};
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
