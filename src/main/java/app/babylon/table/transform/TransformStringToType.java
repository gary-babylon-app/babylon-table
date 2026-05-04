package app.babylon.table.transform;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.column.Transformer;
import app.babylon.text.Sentence.ParseMode;
import app.babylon.text.Strings;

public class TransformStringToType<T> extends TransformConvert
{
    public static final String FUNCTION_NAME = "ToType";

    private final ColumnObject.Mode mode;

    private TransformStringToType(Builder<T> builder)
    {
        super(FUNCTION_NAME, builder.columnName, builder.newColumnName, builder.type, builder.parseMode);
        if (type().isPrimitive() && builder.mode != null)
        {
            throw new IllegalArgumentException("Conversion mode is only supported for non-primitive target types: "
                    + type().getValueClass().getName());
        }
        this.mode = builder.mode;
    }

    public static <T> Builder<T> builder(Column.Type type)
    {
        return TransformStringToType.<T>builder().withType(type);
    }

    public static <T> Builder<T> builder(Column.Type type, ColumnName columnName)
    {
        return TransformStringToType.<T>builder(type).withColumnName(columnName);
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
        private ParseMode parseMode;

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

        public Builder<T> withParseMode(ParseMode parseMode)
        {
            this.parseMode = parseMode;
            return this;
        }

        public TransformStringToType<T> build()
        {
            return new TransformStringToType<>(this);
        }
    }

    public static <T> TransformStringToType<T> of(Column.Type type, String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            ColumnName from = ColumnName.of(params[0]);
            ColumnName to = ColumnName.of(params[1]);
            ColumnObject.Mode mode = (params.length >= 3 && !Strings.isEmpty(params[2]))
                    ? ColumnObject.Mode.parse(params[2])
                    : null;
            ParseMode parseMode = (params.length >= 4 && !Strings.isEmpty(params[3]))
                    ? ParseMode.parse(params[3])
                    : null;
            return TransformStringToType.<T>builder(type, from).withNewColumnName(to).withMode(mode)
                    .withParseMode(parseMode).build();
        }
        return null;
    }

    public ColumnObject.Mode mode()
    {
        return this.mode;
    }

    public ColumnObject.Mode effectiveMode()
    {
        return this.mode == null ? ColumnObject.Mode.AUTO : this.mode;
    }

    @Override
    public Column apply(Column column)
    {
        if (column == null)
        {
            return null;
        }
        if (type().equals(column.getType()))
        {
            if (newColumnName() == null)
            {
                return column;
            }
            return column.copy(effectiveNewColumnName());
        }
        if (type().isPrimitive())
        {
            return rebuildPrimitive(column);
        }
        if (String.class.equals(column.getType().getValueClass()))
        {
            ColumnObject<String> stringColumn = Columns.asStringColumn(column);
            ColumnName transformedColumnName = effectiveNewColumnName();
            Transformer<String, T> transformer = transformer(transformedColumnName);
            ColumnObject.Mode effectiveMode = effectiveMode();
            boolean canTransformCategorical = effectiveMode == ColumnObject.Mode.CATEGORICAL
                    && stringColumn instanceof ColumnCategorical<?>;
            return canTransformCategorical
                    ? stringColumn.transform(transformer)
                    : rebuild(stringColumn, transformedColumnName, effectiveMode, transformer);
        }
        throw new RuntimeException("Can only convert String columns to " + type().getValueClass().getSimpleName() + ": "
                + column.getName());
    }

    private Column rebuildPrimitive(Column column)
    {
        if (!Columns.isStringColumn(column))
        {
            return null;
        }
        ColumnObject<String> stringColumn = Columns.asStringColumn(column);
        Column.Builder builder = Columns.newBuilder(effectiveNewColumnName(), type());
        ParseMode resolvedParseMode = effectiveParseMode();
        for (int i = 0; i < stringColumn.size(); ++i)
        {
            if (!stringColumn.isSet(i))
            {
                builder.addNull();
                continue;
            }
            String s = stringColumn.get(i);
            builder.add(resolvedParseMode, s);
        }
        return builder.build();
    }

    private ColumnObject<T> rebuild(ColumnObject<String> input, ColumnName newColumnName, ColumnObject.Mode mode,
            Transformer<String, T> transformer)
    {
        ColumnObject.Builder<T> transformed = ColumnObject.builder(newColumnName, type(), mode);
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
        return Transformer.parser(type(), effectiveParseMode(), newColumnName);
    }

}
