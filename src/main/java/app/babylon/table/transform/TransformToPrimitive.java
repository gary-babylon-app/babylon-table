package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;

public class TransformToPrimitive extends TransformConvert
{
    public static final String FUNCTION_NAME = "ToPrimitive";

    private TransformToPrimitive(Builder builder)
    {
        super(FUNCTION_NAME, builder.columnName, builder.newColumnName, builder.type, null);
        if (!type().isPrimitive())
        {
            throw new IllegalArgumentException(
                    "TransformToPrimitive requires primitive target type: " + type().getValueClass().getName());
        }
    }

    public static TransformToPrimitive of(ColumnName columnName, ColumnName newColumnName, Column.Type type)
    {
        return builder(type, columnName).withNewColumnName(newColumnName).build();
    }

    public static Builder builder(Column.Type type, ColumnName columnName)
    {
        return new Builder(type, columnName);
    }

    @Override
    public Column apply(Column x)
    {
        if (x == null)
        {
            return null;
        }
        if (type().equals(x.getType()))
        {
            if (newColumnName() == null)
            {
                return x;
            }
            return x.copy(effectiveNewColumnName());
        }
        if (!Columns.isStringColumn(x))
        {
            return null;
        }

        ColumnObject<String> strings = Columns.asStringColumn(x);
        Column.Builder builder = Columns.newBuilder(effectiveNewColumnName(), type());
        for (int i = 0; i < strings.size(); ++i)
        {
            if (!strings.isSet(i))
            {
                builder.addNull();
                continue;
            }
            String s = strings.get(i);
            builder.add(s, 0, s == null ? 0 : s.length());
        }
        return builder.build();
    }

    public static final class Builder
    {
        private final Column.Type type;
        private final ColumnName columnName;
        private ColumnName newColumnName;

        private Builder(Column.Type type, ColumnName columnName)
        {
            this.type = ArgumentCheck.nonNull(type);
            this.columnName = ArgumentCheck.nonNull(columnName);
        }

        public Builder withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public TransformToPrimitive build()
        {
            return new TransformToPrimitive(this);
        }
    }
}
