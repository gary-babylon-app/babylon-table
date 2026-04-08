package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.util.Locale;

import app.babylon.table.ColumnName;
import app.babylon.table.Is;

public class TransformToLowerCase extends TransformStringToString
{
    public static final String FUNCTION_NAME = "ToLowerCase";

    public TransformToLowerCase(ColumnName existingColumnName)
    {
        this(existingColumnName, existingColumnName);
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
            return new TransformToLowerCase(columnName);
        }
        if (params.length >= 2)
        {
            return new TransformToLowerCase(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    @Override
    protected String transformString(String s)
    {
        return Strings.isEmpty(s) ? s : s.toLowerCase(Locale.ROOT);
    }
}
