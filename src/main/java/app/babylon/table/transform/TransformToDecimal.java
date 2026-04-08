package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.BigDecimals;
import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.Is;
import app.babylon.text.Split;

public class TransformToDecimal extends TransformStringColumnsBase<BigDecimal>
{
    public static final String FUNCTION_NAME = "ToDecimal";
    private final Function<CharSequence, BigDecimal> parser;

    public TransformToDecimal(ColumnName... columnNames)
    {
        this(false, columnNames);
    }

    public TransformToDecimal(Function<CharSequence, BigDecimal> parser, ColumnName... columnNames)
    {
        super(FUNCTION_NAME, ArgumentChecks.nonEmpty(columnNames), null);
        this.parser = resolveParser(parser, newTransformFunction(false));
    }

    public TransformToDecimal(boolean toAbsolute, ColumnName... columnNames)
    {
        super(FUNCTION_NAME, ArgumentChecks.nonEmpty(columnNames), null);
        this.parser = newTransformFunction(toAbsolute);
    }

    public TransformToDecimal(ColumnName columnName, ColumnName newColumnName, boolean toAbsolute)
    {
        this(columnName, newColumnName, null, toAbsolute);
    }

    public TransformToDecimal(ColumnName columnName, ColumnName newColumnName, Function<CharSequence, BigDecimal> parser)
    {
        this(columnName, newColumnName, parser, false);
    }

    public static TransformToDecimal of(String... params)
    {
        if (!Is.empty(params) && params.length>=3)
        {
            ColumnName columnName = ColumnName.parse(params[0]);
            ColumnName newColumnName = ColumnName.parse(params[1]);
            boolean toAbsolute = Boolean.parseBoolean(params[2]);
            return new TransformToDecimal(columnName, newColumnName, toAbsolute);
        }
        return null;
    }

    public static TransformToDecimal of(ColumnName x)
    {
        if (x!=null)
        {
            return new TransformToDecimal(x, x, false);
        }
        return null;
    }


    public TransformToDecimal(Collection<ColumnName> columnNames)
    {
        this(columnNames.toArray(new ColumnName[columnNames.size()]));
    }

    public ColumnObject<BigDecimal> apply(Column x)
    {
        Column[] y = new Column[] {x};
        Column[] z = apply(y);
        if (z!=null && z.length>0)
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
    protected Class<BigDecimal> valueClass()
    {
        return BigDecimal.class;
    }

    @Override
    protected Function<CharSequence, BigDecimal> createParser(Column[] selectedColumns)
    {
        return this.parser;
    }

    private static Function<CharSequence, BigDecimal> newTransformFunction(boolean toAbsolute)
    {
        return s ->
        {
            BigDecimal bd = BigDecimals.parse(s);
            if (bd == null)
            {
                bd = BigDecimals.extract(s);
            }
            if (toAbsolute && bd != null)
            {
                bd = bd.abs();
            }
            return bd;
        };
    }

    private TransformToDecimal(ColumnName columnName, ColumnName newColumnName, Function<CharSequence, BigDecimal> parser,
            boolean toAbsolute)
    {
        super(FUNCTION_NAME,
                new ColumnName[]{ArgumentChecks.nonNull(columnName)},
                new ColumnName[]{ArgumentChecks.nonNull(newColumnName)});
        this.parser = resolveParser(parser, newTransformFunction(toAbsolute));
    }
    public static TransformToDecimal of(String s)
    {
        if (Strings.isEmpty(s) || s.length()<FUNCTION_NAME.length()+3)
        {
            throw new RuntimeException("Invalid string for " + FUNCTION_NAME + " construction.");
        }
        s = s.substring(FUNCTION_NAME.length()+1, s.length()-1);
        String[] params = Split.commaSeparatedParams(s);
        ColumnName[] columnNames = ColumnName.of(params);

        return new TransformToDecimal(columnNames);
    }

    @Override
    public String toString()
    {
        return FUNCTION_NAME + "("
                + Arrays.stream(columnNames).map(ColumnName::toString).collect(Collectors.joining(","))
                + ")";
    }
}
