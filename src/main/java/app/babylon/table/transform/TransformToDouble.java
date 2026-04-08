package app.babylon.table.transform;

import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.BigDecimals;
import app.babylon.table.Column;
import app.babylon.table.ColumnDouble;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.Columns;
import app.babylon.table.Is;

public class TransformToDouble extends TransformBase
{
    public static final String FUNCTION_NAME = "ToDouble";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;

    public TransformToDouble(ColumnName columnName)
    {
        this(columnName, columnName);
    }

    public TransformToDouble(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = ArgumentChecks.nonNull(existingColumnName);
        this.newColumnName = ArgumentChecks.nonNull(newColumnName);
    }

    public static TransformToDouble of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return new TransformToDouble(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public ColumnDouble apply(Column x)
    {
        if (x == null)
        {
            return null;
        }
        if (x instanceof ColumnDouble doubles)
        {
            return doubles.copy(this.newColumnName);
        }
        if (!Columns.isStringColumn(x))
        {
            return null;
        }

        ColumnObject<String> strings = Columns.asStringColumn(x);
        ColumnDouble.Builder builder = ColumnDouble.builder(this.newColumnName);
        for (int i = 0; i < strings.size(); ++i)
        {
            if (!strings.isSet(i))
            {
                builder.addNull();
                continue;
            }
            String s = strings.get(i);
            Double parsed = BigDecimals.parseDouble(s);
            if (parsed == null)
            {
                parsed = BigDecimals.extractDouble(s);
            }
            if (parsed == null)
            {
                builder.addNull();
            } else
            {
                builder.add(parsed.doubleValue());
            }
        }
        return builder.build();
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column source = columnsByName.get(this.existingColumnName);
        ColumnDouble doubles = apply(source);
        if (doubles != null)
        {
            columnsByName.put(this.newColumnName, doubles);
        }
    }
}
