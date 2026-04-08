package app.babylon.table.transform;

import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.Is;

public class TransformToInt extends TransformBase
{
    public static final String FUNCTION_NAME = "ToInt";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;

    public TransformToInt(ColumnName columnName)
    {
        this(columnName, columnName);
    }

    public TransformToInt(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = ArgumentChecks.nonNull(existingColumnName);
        this.newColumnName = ArgumentChecks.nonNull(newColumnName);
    }

    public static TransformToInt of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return new TransformToInt(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public ColumnInt apply(Column x)
    {
        if (x == null)
        {
            return null;
        }
        if (x instanceof ColumnInt ints)
        {
            return ints.copy(this.newColumnName);
        }
        if (!Columns.isStringColumn(x))
        {
            return null;
        }

        ColumnObject<String> stringColumn = Columns.asStringColumn(x);
        ColumnInt.Builder builder = ColumnInt.builder(this.newColumnName);
        for (int i = 0; i < stringColumn.size(); ++i)
        {
            if (!stringColumn.isSet(i))
            {
                builder.addNull();
                continue;
            }
            String s = stringColumn.get(i);
            if (!Is.integer(s))
            {
                builder.addNull();
                continue;
            }
            try
            {
                builder.add(Integer.parseInt(s));
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
        ColumnInt ints = apply(source);
        if (ints != null)
        {
            columnsByName.put(this.newColumnName, ints);
        }
    }
}
