package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.table.Is;

public class TransformPrefix extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Prefix";

    private final String prefix;

    public TransformPrefix(String prefix, ColumnName existingColumnName)
    {
        this(prefix, existingColumnName, existingColumnName);
    }

    public TransformPrefix(String prefix, ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.prefix = app.babylon.table.ArgumentChecks.nonEmpty(prefix);
    }

    public static TransformPrefix of(String... params)
    {
        if (Is.empty(params) || params.length < 2)
        {
            return null;
        }
        if (params.length == 2)
        {
            return new TransformPrefix(params[0], ColumnName.of(params[1]));
        }
        return new TransformPrefix(params[0], ColumnName.of(params[1]), ColumnName.of(params[2]));
    }

    @Override
    protected String transformString(String s)
    {
        return Strings.isEmpty(s) ? null : this.prefix.concat(s);
    }
}
