package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.util.Map;
import java.util.function.Function;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.table.Is;

abstract class TransformStringColumnsBase<T> extends TransformBase
{
    protected final ColumnName[] columnNames;
    protected final ColumnName[] newColumnNames;

    TransformStringColumnsBase(String name, ColumnName[] columnNames, ColumnName[] newColumnNames)
    {
        super(name);
        this.columnNames = ArgumentChecks.nonEmpty(columnNames);
        this.newColumnNames = newColumnNames;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column[] transformedColumns = transformColumns(getColumns(columnsByName, this.columnNames));
        putColumns(columnsByName, transformedColumns);
    }

    protected Column[] transformColumns(Column[] validColumns)
    {
        Function<CharSequence, T> parser = createParser(validColumns);
        return transformStringColumns(validColumns, this.newColumnNames, valueClass(), s -> parseValue(parser, s));
    }

    protected abstract Class<T> valueClass();

    protected abstract Function<CharSequence, T> createParser(Column[] selectedColumns);

    private T parseValue(Function<CharSequence, T> parser, CharSequence s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        return parser.apply(s);
    }

    protected static <T> Function<CharSequence, T> resolveParser(Function<CharSequence, T> parser,
            Function<CharSequence, T> fallbackParser)
    {
        return (parser != null) ? parser : ArgumentChecks.nonNull(fallbackParser);
    }
}
