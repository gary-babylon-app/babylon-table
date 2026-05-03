package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnByte;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;

public class TransformToPrimitive extends TransformBase
{
    public static final String FUNCTION_NAME = "ToPrimitive";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;
    private final Column.Type type;
    private final TransformParseMode parseMode;

    private TransformToPrimitive(Builder builder)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = builder.existingColumnName;
        this.newColumnName = builder.newColumnName;
        this.type = builder.type;
        this.parseMode = builder.parseMode;
        if (!this.type.isPrimitive())
        {
            throw new IllegalArgumentException(
                    "TransformToPrimitive requires primitive target type: " + this.type.getValueClass().getName());
        }
    }

    public Column.Type getType()
    {
        return this.type;
    }

    public Column.Type type()
    {
        return this.type;
    }

    public ColumnName existingColumnName()
    {
        return this.existingColumnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public TransformParseMode getParseMode()
    {
        return this.parseMode;
    }

    public TransformParseMode parseMode()
    {
        return this.parseMode;
    }

    public TransformParseMode effectiveParseMode()
    {
        return this.parseMode == null ? TransformParseMode.EXACT : this.parseMode;
    }

    public ColumnName effectiveNewColumnName()
    {
        return this.newColumnName == null ? this.existingColumnName : this.newColumnName;
    }

    public static TransformToPrimitive of(ColumnName existingColumnName, ColumnName newColumnName, Column.Type type,
            TransformParseMode parseMode)
    {
        return builder(type, existingColumnName).withNewColumnName(newColumnName).withParseMode(parseMode).build();
    }

    public static Builder builder(Column.Type type, ColumnName existingColumnName)
    {
        return new Builder(type, existingColumnName);
    }

    public Column apply(Column x)
    {
        if (x == null)
        {
            return null;
        }
        if (this.type.equals(x.getType()))
        {
            return x.copy(effectiveNewColumnName());
        }
        if (!Columns.isStringColumn(x))
        {
            return null;
        }

        ColumnObject<String> strings = Columns.asStringColumn(x);
        if (strings instanceof ColumnCategorical<String> categorical)
        {
            return applyCategorical(categorical);
        }

        Column.Builder builder = Columns.newBuilder(effectiveNewColumnName(), this.type);
        for (int i = 0; i < strings.size(); ++i)
        {
            if (!strings.isSet(i))
            {
                addNull(builder);
                continue;
            }
            String s = strings.get(i);
            Object parsed = effectiveParseMode().apply(this.type.getParser()::parse, s);
            if (parsed == null)
            {
                addNull(builder);
            }
            else
            {
                addParsedValue(builder, parsed);
            }
        }
        return builder.build();
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column source = columnsByName.get(this.existingColumnName);
        Column transformed = apply(source);
        if (transformed != null)
        {
            columnsByName.put(effectiveNewColumnName(), transformed);
        }
    }

    private void addParsedValue(Column.Builder builder, Object parsed)
    {
        if (ColumnTypes.BYTE.equals(this.type))
        {
            ((ColumnByte.Builder) builder).add(((Byte) parsed).byteValue());
        }
        else if (ColumnTypes.BOOLEAN.equals(this.type))
        {
            ((ColumnBoolean.Builder) builder).add(((Boolean) parsed).booleanValue());
        }
        else if (ColumnTypes.INT.equals(this.type))
        {
            ((ColumnInt.Builder) builder).add(((Integer) parsed).intValue());
        }
        else if (ColumnTypes.LONG.equals(this.type))
        {
            ((ColumnLong.Builder) builder).add(((Long) parsed).longValue());
        }
        else if (ColumnTypes.DOUBLE.equals(this.type))
        {
            ((ColumnDouble.Builder) builder).add(((Double) parsed).doubleValue());
        }
        else
        {
            throw new IllegalStateException("Unsupported primitive type: " + this.type.getValueClass().getName());
        }
    }

    private Column applyCategorical(ColumnCategorical<String> strings)
    {
        int[] codes = strings.getCategoryCodes(null);
        Object[] parsedByCode = new Object[codes.length == 0 ? 1 : maxCode(codes) + 1];
        for (int code : codes)
        {
            String value = strings.getCategoryValue(code);
            parsedByCode[code] = value == null ? null : effectiveParseMode().apply(this.type.getParser()::parse, value);
        }

        Column.Builder builder = Columns.newBuilder(effectiveNewColumnName(), this.type);
        for (int i = 0; i < strings.size(); ++i)
        {
            int code = strings.getCategoryCode(i);
            if (code <= 0)
            {
                addNull(builder);
                continue;
            }
            Object parsed = parsedByCode[code];
            if (parsed == null)
            {
                addNull(builder);
            }
            else
            {
                addParsedValue(builder, parsed);
            }
        }
        return builder.build();
    }

    private static int maxCode(int[] codes)
    {
        int max = 0;
        for (int code : codes)
        {
            if (code > max)
            {
                max = code;
            }
        }
        return max;
    }

    private static void addNull(Column.Builder builder)
    {
        builder.add((CharSequence) null, 0, 0);
    }

    public static final class Builder
    {
        private final Column.Type type;
        private final ColumnName existingColumnName;
        private ColumnName newColumnName;
        private TransformParseMode parseMode;

        private Builder(Column.Type type, ColumnName existingColumnName)
        {
            this.type = ArgumentCheck.nonNull(type);
            this.existingColumnName = ArgumentCheck.nonNull(existingColumnName);
        }

        public Builder withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public Builder withParseMode(TransformParseMode parseMode)
        {
            this.parseMode = parseMode;
            return this;
        }

        public TransformToPrimitive build()
        {
            return new TransformToPrimitive(this);
        }
    }
}
