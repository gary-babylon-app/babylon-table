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
import app.babylon.text.Strings;

/**
 * Legacy decimal conversion transform. New transformation statements should use
 * {@link TransformToType} with
 * {@link app.babylon.table.column.ColumnTypes#DECIMAL}.
 */
@Deprecated(since = "0.3.25", forRemoval = true)
public class TransformToDecimal extends TransformStringColumnsBase<BigDecimal>
{
    public static final String FUNCTION_NAME = "ToDecimal";
    private final Function<CharSequence, BigDecimal> parser;

    private TransformToDecimal(ColumnName columnName)
    {
        super(FUNCTION_NAME, new ColumnName[]
        {ArgumentCheck.nonNull(columnName)}, null);
        this.parser = TransformToDecimal::parseDecimal;
    }

    private TransformToDecimal(ColumnName columnName, ColumnName newColumnName)
    {
        this(columnName, newColumnName, null);
    }

    public static TransformToDecimal of(ColumnName columnName)
    {
        return columnName == null ? null : new TransformToDecimal(columnName);
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

    public static TransformToDecimal of(ColumnName columnName, ColumnName newColumnName)
    {
        return new TransformToDecimal(columnName, newColumnName);
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
        return BigDecimals.parse(s);
    }

    private TransformToDecimal(ColumnName columnName, ColumnName newColumnName,
            Function<CharSequence, BigDecimal> parser)
    {
        super(FUNCTION_NAME, new ColumnName[]
        {ArgumentCheck.nonNull(columnName)}, newColumnNames(newColumnName));
        this.parser = resolveParser(parser, TransformToDecimal::parseDecimal);
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
