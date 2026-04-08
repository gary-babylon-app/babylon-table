package app.babylon.table.transform;

import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.Is;

public class TransformToLong extends TransformBase
{
    public static final String FUNCTION_NAME = "ToLong";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;

    public TransformToLong(ColumnName columnName)
    {
        this(columnName, columnName);
    }

    public TransformToLong(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = ArgumentChecks.nonNull(existingColumnName);
        this.newColumnName = ArgumentChecks.nonNull(newColumnName);
    }

    public static TransformToLong of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return new TransformToLong(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public ColumnLong apply(Column x)
    {
        if (x == null)
        {
            return null;
        }
        if (x instanceof ColumnLong longs)
        {
            return longs.copy(this.newColumnName);
        }
        if (!Columns.isStringColumn(x))
        {
            return null;
        }

        ColumnObject<String> strings = Columns.asStringColumn(x);
        ColumnLong.Builder builder = ColumnLong.builder(this.newColumnName);
        for (int i = 0; i < strings.size(); ++i)
        {
            if (!strings.isSet(i))
            {
                builder.addNull();
                continue;
            }
            String s = strings.get(i);
            if (!Is.integer(s))
            {
                builder.addNull();
                continue;
            }
            try
            {
                builder.add(Long.parseLong(s));
            } catch (NumberFormatException e)
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column source = columnsByName.get(this.existingColumnName);
        ColumnLong longs = apply(source);
        if (longs != null)
        {
            columnsByName.put(this.newColumnName, longs);
        }
    }
}
