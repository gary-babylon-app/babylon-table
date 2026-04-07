package app.babylon.table.transform;

import java.util.Locale;

import app.babylon.table.ColumnName;
import app.babylon.table.Is;

public class TransformToUpperCase extends TransformStringToString
{
    public static final String FUNCTION_NAME = "ToUpperCase";

    public TransformToUpperCase(ColumnName existingColumnName)
    {
        this(existingColumnName, existingColumnName);
    }

    public TransformToUpperCase(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
    }

    public static TransformToUpperCase of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        if (params.length == 1)
        {
            ColumnName columnName = ColumnName.of(params[0]);
            return new TransformToUpperCase(columnName);
        }
        if (params.length >= 2)
        {
            return new TransformToUpperCase(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    @Override
    protected String transformString(String s)
    {
        return Is.empty(s) ? s : s.toUpperCase(Locale.ROOT);
    }
}
