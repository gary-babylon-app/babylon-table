package app.babylon.table.transform;

import java.util.Map;
import java.util.function.Function;
import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.column.Transformer;
import app.babylon.text.Strings;

public class TransformToType<T> extends TransformBase
{
    public static final String FUNCTION_NAME = "ToType";

    private final ColumnName[] columnNames;
    private final ColumnName[] newColumnNames;
    private final Column.Type type;
    private final ColumnObject.Mode mode;
    private final TransformParseMode parseMode;

    public TransformToType(Column.Type type, ColumnName... columnNames)
    {
        this(type, ColumnObject.Mode.AUTO, TransformParseMode.EXACT, columnNames);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, ColumnName... columnNames)
    {
        this(type, mode, TransformParseMode.EXACT, columnNames);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, TransformParseMode parseMode,
            ColumnName... columnNames)
    {
        super(FUNCTION_NAME);
        this.columnNames = ArgumentCheck.nonEmpty(columnNames);
        this.newColumnNames = null;
        this.mode = mode == null ? ColumnObject.Mode.AUTO : mode;
        this.parseMode = parseMode == null ? TransformParseMode.EXACT : parseMode;
        this.type = ArgumentCheck.nonNull(type);
    }

    public TransformToType(Column.Type type, ColumnName columnName, Function<String, T> parser,
            ColumnName newColumnName)
    {
        this(type, ColumnObject.Mode.AUTO, TransformParseMode.EXACT, columnName, newColumnName);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, ColumnName columnName, ColumnName newColumnName)
    {
        this(type, mode, TransformParseMode.EXACT, columnName, newColumnName);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, TransformParseMode parseMode,
            ColumnName columnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.columnNames = new ColumnName[]
        {ArgumentCheck.nonNull(columnName)};
        this.newColumnNames = new ColumnName[]
        {ArgumentCheck.nonNull(newColumnName)};
        this.mode = mode == null ? ColumnObject.Mode.AUTO : mode;
        this.parseMode = parseMode == null ? TransformParseMode.EXACT : parseMode;
        this.type = ArgumentCheck.nonNull(type);
    }

    public static <T> TransformToType<T> of(Column.Type type, String... params)
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
            return new TransformToType<>(type, mode, parseMode, from, to);
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
                Transformer<String, T> transformer = transformer(newColumnName);
                boolean canTransformCategorical = mode == ColumnObject.Mode.CATEGORICAL
                        && stringColumn instanceof ColumnCategorical<?>;
                transformedColumns[i] = canTransformCategorical
                        ? stringColumn.transform(transformer)
                        : rebuild(stringColumn, newColumnName, mode, transformer);
            }
            else
            {
                throw new RuntimeException("Can only convert String columns to " + type.getValueClass().getSimpleName()
                        + ": " + column.getName());
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

    private ColumnObject<T> rebuild(ColumnObject<String> input, ColumnName newColumnName, ColumnObject.Mode mode,
            Transformer<String, T> transformer)
    {
        ColumnObject.Builder<T> transformed = ColumnObject.builder(newColumnName, type, mode);
        for (int j = 0; j < input.size(); ++j)
        {
            if (input.isSet(j))
            {
                String s = input.get(j);
                transformed.add(s == null ? null : transformer.apply(s));
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
        return Transformer.parser(type, this.parseMode, newColumnName);
    }

}
