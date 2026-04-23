package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.lang.Is;

public class TransformCleanWhitespace extends TransformStringToString
{
    public static final String FUNCTION_NAME = "CleanWhitespace";

    public TransformCleanWhitespace(ColumnName existingColumnName)
    {
        this(existingColumnName, existingColumnName);
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
            return new TransformCleanWhitespace(columnName);
        }
        if (params.length >= 2)
        {
            return new TransformCleanWhitespace(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
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
