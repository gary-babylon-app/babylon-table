package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.util.Locale;

import app.babylon.table.column.ColumnName;
import app.babylon.lang.Is;

public class TransformToLowerCase extends TransformStringToString
{
    public static final String FUNCTION_NAME = "ToLowerCase";

    public TransformToLowerCase(ColumnName existingColumnName)
    {
        this(existingColumnName, null);
    }

    public TransformToLowerCase(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
    }

    public static TransformToLowerCase of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        if (params.length == 1)
        {
            ColumnName columnName = ColumnName.of(params[0]);
            return of(columnName);
        }
        if (params.length >= 2)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public static TransformToLowerCase of(ColumnName existingColumnName)
    {
        return new TransformToLowerCase(existingColumnName);
    }

    public static TransformToLowerCase of(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return new TransformToLowerCase(existingColumnName, newColumnName);
    }

    @Override
    protected String transformString(String s)
    {
        return Strings.isEmpty(s) ? s : s.toLowerCase(Locale.ROOT);
    }
}
