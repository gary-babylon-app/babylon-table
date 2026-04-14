package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformCoalesce extends TransformBase
{
    public static final String FUNCTION_NAME = "Coalesce";

    private final ColumnName newColumnName;
    private final ColumnObject.Mode mode;
    private final ColumnName firstColumnName;
    private final ColumnName secondColumnName;
    private final ColumnName thirdColumnName;

    public TransformCoalesce(ColumnName newColumnName, ColumnObject.Mode mode, ColumnName firstColumnName,
            ColumnName secondColumnName, ColumnName thirdColumnName)
    {
        super(FUNCTION_NAME);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.mode = mode == null ? ColumnObject.Mode.AUTO : mode;
        this.firstColumnName = ArgumentCheck.nonNull(firstColumnName);
        this.secondColumnName = ArgumentCheck.nonNull(secondColumnName);
        this.thirdColumnName = ArgumentCheck.nonNull(thirdColumnName);
    }

    public static TransformCoalesce of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        if (params.length == 4)
        {
            return new TransformCoalesce(ColumnName.of(params[0]), ColumnObject.Mode.AUTO, ColumnName.of(params[1]),
                    ColumnName.of(params[2]), ColumnName.of(params[3]));
        }
        if (params.length >= 5)
        {
            return new TransformCoalesce(ColumnName.of(params[0]), ColumnObject.Mode.parse(params[1]),
                    ColumnName.of(params[2]), ColumnName.of(params[3]), ColumnName.of(params[4]));
        }
        return null;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column first = columnsByName.get(this.firstColumnName);
        Column second = columnsByName.get(this.secondColumnName);
        Column third = columnsByName.get(this.thirdColumnName);

        if (!(first instanceof ColumnObject<?> firstObject) || !(second instanceof ColumnObject<?> secondObject)
                || !(third instanceof ColumnObject<?> thirdObject))
        {
            return;
        }

        Class<?> valueClass = first.getType().getValueClass();
        if (!valueClass.equals(second.getType().getValueClass()) || !valueClass.equals(third.getType().getValueClass()))
        {
            throw new IllegalArgumentException("Coalesce requires matching value classes: " + first.getName() + ", "
                    + second.getName() + ", " + third.getName());
        }
        if (first.size() != second.size() || first.size() != third.size())
        {
            throw new IllegalArgumentException("Coalesce requires columns to have matching sizes: " + first.getName()
                    + ", " + second.getName() + ", " + third.getName());
        }

        coalesce(columnsByName, firstObject, secondObject, thirdObject, valueClass);
    }

    @SuppressWarnings(
    {"unchecked", "rawtypes"})
    private void coalesce(Map<ColumnName, Column> columnsByName, ColumnObject<?> first, ColumnObject<?> second,
            ColumnObject<?> third, Class<?> valueClass)
    {
        ColumnObject.Builder builder = ColumnObject.builder(this.newColumnName, Column.Type.of(valueClass), this.mode);
        for (int i = 0; i < first.size(); ++i)
        {
            if (first.isSet(i))
            {
                builder.add(first.get(i));
            }
            else if (second.isSet(i))
            {
                builder.add(second.get(i));
            }
            else if (third.isSet(i))
            {
                builder.add(third.get(i));
            }
            else
            {
                builder.addNull();
            }
        }
        columnsByName.put(this.newColumnName, builder.build());
    }
}
