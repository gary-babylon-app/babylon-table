package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Map;
import java.util.function.Function;

import app.babylon.table.column.Column;
import app.babylon.table.column.Column.Type;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

abstract class TransformStringColumnsBase<T> extends TransformBase
{
    protected final ColumnName[] columnNames;
    protected final ColumnName[] newColumnNames;

    TransformStringColumnsBase(String name, ColumnName[] columnNames, ColumnName[] newColumnNames)
    {
        super(name);
        this.columnNames = ArgumentCheck.nonEmpty(columnNames);
        this.newColumnNames = newColumnNames;
    }

    public ColumnName[] columnNames()
    {
        return java.util.Arrays.copyOf(this.columnNames, this.columnNames.length);
    }

    public ColumnName[] newColumnNames()
    {
        return this.newColumnNames == null
                ? null
                : java.util.Arrays.copyOf(this.newColumnNames, this.newColumnNames.length);
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
        return transformStringColumns(validColumns, this.newColumnNames, type(), s -> parseValue(parser, s));
    }

    protected abstract Type type();

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
        return (parser != null) ? parser : ArgumentCheck.nonNull(fallbackParser);
    }
}
