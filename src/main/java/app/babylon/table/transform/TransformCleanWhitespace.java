package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.lang.Is;

public class TransformCleanWhitespace extends TransformStringToString
{
    public static final String FUNCTION_NAME = "CleanWhitespace";

    public TransformCleanWhitespace(ColumnName existingColumnName)
    {
        this(existingColumnName, null);
    }

    public TransformCleanWhitespace(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
    }

    public static TransformCleanWhitespace of(String... params)
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

    public static TransformCleanWhitespace of(ColumnName existingColumnName)
    {
        return new TransformCleanWhitespace(existingColumnName);
    }

    public static TransformCleanWhitespace of(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return new TransformCleanWhitespace(existingColumnName, newColumnName);
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return s;
        }
        return s.strip().replaceAll("\\s+", " ");
    }
}
