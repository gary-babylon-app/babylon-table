package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.lang.Is;

public class TransformBefore extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Before";
    private final String delimiter;

    public TransformBefore(ColumnName existingColumnName, ColumnName newColumnName, String delimiter)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.delimiter = app.babylon.lang.ArgumentCheck.nonEmpty(delimiter);
    }

    public static TransformBefore of(String... params)
    {
        if (!Is.empty(params) && params.length >= 3)
        {
            return new TransformBefore(ColumnName.of(params[0]), ColumnName.of(params[1]), params[2]);
        }
        return null;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        int index = s.indexOf(this.delimiter);
        if (index < 0)
        {
            return null;
        }
        return s.substring(0, index);
    }
}
