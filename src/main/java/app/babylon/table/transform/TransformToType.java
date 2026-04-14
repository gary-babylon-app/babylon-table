package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import app.babylon.text.Strings;

import java.util.Map;
import java.util.function.Function;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.lang.Is;
import app.babylon.table.column.Transformer;

public class TransformToType<T> extends TransformBase
{
    public static final String FUNCTION_NAME = "ToType";

    private final ColumnName[] columnNames;
    private final ColumnName[] newColumnNames;
    private final Function<String, T> parser;
    private final Class<T> valueClass;
    private final ColumnObject.Mode mode;
    private final TransformParseMode parseMode;

    public TransformToType(Class<T> valueClass, Function<String, T> parser, ColumnName... columnNames)
    {
        this(valueClass, ColumnObject.Mode.AUTO, TransformParseMode.EXACT, parser, columnNames);
    }

    public TransformToType(Class<T> valueClass, ColumnObject.Mode mode, Function<String, T> parser,
            ColumnName... columnNames)
    {
        this(valueClass, mode, TransformParseMode.EXACT, parser, columnNames);
    }

    public TransformToType(Class<T> valueClass, ColumnObject.Mode mode, TransformParseMode parseMode,
            Function<String, T> parser, ColumnName... columnNames)
    {
        super(FUNCTION_NAME);
        this.parser = ArgumentCheck.nonNull(parser);
        this.columnNames = ArgumentCheck.nonEmpty(columnNames);
        this.newColumnNames = null;
        this.valueClass = ArgumentCheck.nonNull(valueClass);
        this.mode = mode == null ? ColumnObject.Mode.AUTO : mode;
        this.parseMode = parseMode == null ? TransformParseMode.EXACT : parseMode;
    }

    public TransformToType(Class<T> valueClass, ColumnName columnName, Function<String, T> parser,
            ColumnName newColumnName)
    {
        this(valueClass, ColumnObject.Mode.AUTO, TransformParseMode.EXACT, columnName, parser, newColumnName);
    }

    public TransformToType(Class<T> valueClass, ColumnObject.Mode mode, ColumnName columnName,
            Function<String, T> parser, ColumnName newColumnName)
    {
        this(valueClass, mode, TransformParseMode.EXACT, columnName, parser, newColumnName);
    }

    public TransformToType(Class<T> valueClass, ColumnObject.Mode mode, TransformParseMode parseMode,
            ColumnName columnName, Function<String, T> parser, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.parser = ArgumentCheck.nonNull(parser);
        this.columnNames = new ColumnName[]
        {ArgumentCheck.nonNull(columnName)};
        this.newColumnNames = new ColumnName[]
        {ArgumentCheck.nonNull(newColumnName)};
        this.valueClass = ArgumentCheck.nonNull(valueClass);
        this.mode = mode == null ? ColumnObject.Mode.AUTO : mode;
        this.parseMode = parseMode == null ? TransformParseMode.EXACT : parseMode;
    }

    public static <T> TransformToType<T> of(Class<T> valueClass, Function<String, T> parser, String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            ColumnName from = ColumnName.of(params[0]);
            ColumnName to = ColumnName.of(params[1]);
            ColumnObject.Mode mode = (params.length >= 3 && !Strings.isEmpty(params[2]))
                    ? ColumnObject.Mode.parse(params[2])
                    : ColumnObject.Mode.CATEGORICAL;
            TransformParseMode parseMode = (params.length >= 4 && !Strings.isEmpty(params[3]))
                    ? TransformParseMode.parse(params[3])
                    : TransformParseMode.EXACT;
            return new TransformToType<>(valueClass, mode, parseMode, from, parser, to);
        }
        return null;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null || Is.empty(this.columnNames))
        {
            return;
        }
        Column[] validColumns = getColumns(columnsByName, this.columnNames);
        Column[] transformedColumns = new Column[validColumns.length];

        for (int i = 0; i < validColumns.length; ++i)
        {
            Column column = validColumns[i];
            if (String.class.equals(column.getType().getValueClass()))
            {
                ColumnObject<String> stringColumn = Columns.asStringColumn(column);
                ColumnName newColumnName = (newColumnNames == null) ? column.getName() : newColumnNames[i];
                if (mode == ColumnObject.Mode.AUTO)
                {
                    transformedColumns[i] = rebuild(stringColumn, newColumnName, mode, this::parseValue);
                }
                else
                {
                    transformedColumns[i] = sourceMatchesMode(stringColumn, mode)
                            ? stringColumn.transform(transformer(newColumnName))
                            : rebuild(stringColumn, newColumnName, mode, this::parseValue);
                }
            }
            else if (valueClass.equals(column.getType().getValueClass()))
            {
                @SuppressWarnings("unchecked")
                ColumnObject<T> typedColumn = (ColumnObject<T>) column;
                ColumnName newColumnName = (newColumnNames == null) ? column.getName() : newColumnNames[i];
                if (newColumnName.equals(column.getName())
                        && (mode == ColumnObject.Mode.AUTO || sourceMatchesMode(typedColumn, mode)))
                {
                    transformedColumns[i] = column;
                }
                else if (sourceMatchesMode(typedColumn, mode))
                {
                    transformedColumns[i] = column.copy(newColumnName);
                }
                else
                {
                    transformedColumns[i] = rebuild(typedColumn, newColumnName, mode, Function.identity());
                }
            }
            else
            {
                throw new RuntimeException(
                        "Cannot convert to " + valueClass.getSimpleName() + " from " + column.getName());
            }
        }
        putColumns(columnsByName, transformedColumns);
    }

    private static boolean sourceMatchesMode(ColumnObject<?> column, ColumnObject.Mode mode)
    {
        return switch (mode)
        {
            case AUTO -> false;
            case CATEGORICAL -> column instanceof ColumnCategorical<?>;
            case ARRAY -> !(column instanceof ColumnCategorical<?>);
        };
    }

    private <S> ColumnObject<T> rebuild(ColumnObject<S> input, ColumnName newColumnName, ColumnObject.Mode mode,
            Function<? super S, ? extends T> mapper)
    {
        ColumnObject.Builder<T> transformed = ColumnObject.builder(newColumnName, Column.Type.of(valueClass), mode);
        for (int j = 0; j < input.size(); ++j)
        {
            if (input.isSet(j))
            {
                S s = input.get(j);
                transformed.add(s == null ? null : mapper.apply(s));
            }
            else
            {
                transformed.addNull();
            }
        }
        return transformed.build();
    }

    private Transformer<String, T> transformer(ColumnName newColumnName)
    {
        return Transformer.of(this::parseValue, valueClass, newColumnName);
    }

    private T parseValue(String s)
    {
        return Strings.isEmpty(s) ? null : parseMode.apply(parser, s);
    }

}
