package app.babylon.table.transform;

import java.util.Map;

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

    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final Column.Type type;
    private final ColumnObject.Mode mode;
    private final TransformParseMode parseMode;

    private TransformToType(Builder<T> builder)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(builder.columnName);
        this.newColumnName = builder.newColumnName;
        this.mode = builder.mode;
        this.parseMode = builder.parseMode;
        this.type = ArgumentCheck.nonNull(builder.type);
    }

    public static <T> Builder<T> builder(Column.Type type)
    {
        return TransformToType.<T>builder().withType(type);
    }

    public static <T> Builder<T> builder(Column.Type type, ColumnName columnName)
    {
        return TransformToType.<T>builder(type).withColumnName(columnName);
    }

    public static <T> Builder<T> builder()
    {
        return new Builder<>();
    }

    public static final class Builder<T>
    {
        private ColumnName columnName;
        private ColumnName newColumnName;
        private Column.Type type;
        private ColumnObject.Mode mode;
        private TransformParseMode parseMode;

        private Builder()
        {
        }

        public Builder<T> withColumnName(ColumnName columnName)
        {
            this.columnName = columnName;
            return this;
        }

        public Builder<T> withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public Builder<T> withType(Column.Type type)
        {
            this.type = type;
            return this;
        }

        public Builder<T> withMode(ColumnObject.Mode mode)
        {
            this.mode = mode;
            return this;
        }

        public Builder<T> withParseMode(TransformParseMode parseMode)
        {
            this.parseMode = parseMode;
            return this;
        }

        public TransformToType<T> build()
        {
            return new TransformToType<>(this);
        }
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
            return TransformToType.<T>builder(type, from).withNewColumnName(to).withMode(mode).withParseMode(parseMode)
                    .build();
        }
        return null;
    }

    public ColumnObject.Mode mode()
    {
        return this.mode;
    }

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
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
        if (columnsByName == null)
        {
            return;
        }
        Column column = columnsByName.get(this.columnName);
        if (column == null)
        {
            return;
        }
        if (String.class.equals(column.getType().getValueClass()))
        {
            ColumnObject<String> stringColumn = Columns.asStringColumn(column);
            ColumnName transformedColumnName = this.newColumnName == null ? column.getName() : this.newColumnName;
            Transformer<String, T> transformer = transformer(transformedColumnName);
            ColumnObject.Mode effectiveMode = effectiveMode();
            boolean canTransformCategorical = effectiveMode == ColumnObject.Mode.CATEGORICAL
                    && stringColumn instanceof ColumnCategorical<?>;
            Column transformedColumn = canTransformCategorical
                    ? stringColumn.transform(transformer)
                    : rebuild(stringColumn, transformedColumnName, effectiveMode, transformer);
            columnsByName.put(transformedColumnName, transformedColumn);
            return;
        }
        throw new RuntimeException(
                "Can only convert String columns to " + type.getValueClass().getSimpleName() + ": " + column.getName());
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
