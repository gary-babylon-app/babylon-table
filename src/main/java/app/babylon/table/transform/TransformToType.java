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

    public TransformToType(Column.Type type, ColumnName columnName)
    {
        this(type, (ColumnObject.Mode) null, (TransformParseMode) null, columnName);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, ColumnName columnName)
    {
        this(type, mode, (TransformParseMode) null, columnName);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, TransformParseMode parseMode,
            ColumnName columnName)
    {
        super(FUNCTION_NAME);
        this.columnNames = new ColumnName[]
        {ArgumentCheck.nonNull(columnName)};
        this.newColumnNames = null;
        this.mode = mode;
        this.parseMode = parseMode;
        this.type = ArgumentCheck.nonNull(type);
    }

    public TransformToType(Column.Type type, ColumnName columnName, Function<String, T> parser,
            ColumnName newColumnName)
    {
        this(type, null, null, columnName, newColumnName);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, ColumnName columnName, ColumnName newColumnName)
    {
        this(type, mode, null, columnName, newColumnName);
    }

    public TransformToType(Column.Type type, ColumnObject.Mode mode, TransformParseMode parseMode,
            ColumnName columnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.columnNames = new ColumnName[]
        {ArgumentCheck.nonNull(columnName)};
        this.newColumnNames = newColumnName == null ? null : new ColumnName[]
        {newColumnName};
        this.mode = mode;
        this.parseMode = parseMode;
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
                    : null;
            TransformParseMode parseMode = (params.length >= 4 && !Strings.isEmpty(params[3]))
                    ? TransformParseMode.parse(params[3])
                    : null;
            return of(type, from, to, mode, parseMode);
        }
        return null;
    }

    public static <T> TransformToType<T> of(Column.Type type, ColumnName from, ColumnName to, ColumnObject.Mode mode,
            TransformParseMode parseMode)
    {
        return new TransformToType<>(type, mode, parseMode, from, to);
    }

    public ColumnObject.Mode mode()
    {
        return this.mode;
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

    public Column.Type type()
    {
        return this.type;
    }

    public ColumnObject.Mode effectiveMode()
    {
        return this.mode == null ? ColumnObject.Mode.AUTO : this.mode;
    }

    public TransformParseMode parseMode()
    {
        return this.parseMode;
    }

    public TransformParseMode effectiveParseMode()
    {
        return this.parseMode == null ? TransformParseMode.EXACT : this.parseMode;
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
                ColumnObject.Mode effectiveMode = effectiveMode();
                boolean canTransformCategorical = effectiveMode == ColumnObject.Mode.CATEGORICAL
                        && stringColumn instanceof ColumnCategorical<?>;
                transformedColumns[i] = canTransformCategorical
                        ? stringColumn.transform(transformer)
                        : rebuild(stringColumn, newColumnName, effectiveMode, transformer);
            }
            else
            {
                throw new RuntimeException("Can only convert String columns to " + type.getValueClass().getSimpleName()
                        + ": " + column.getName());
            }
        }
        putColumns(columnsByName, transformedColumns);
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
        return Transformer.parser(type, effectiveParseMode(), newColumnName);
    }

}
