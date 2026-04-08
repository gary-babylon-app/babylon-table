package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.ColumnName;
import app.babylon.table.Is;

public class TransformRight extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Right";
    private final int length;

    public TransformRight(ColumnName existingColumnName, ColumnName newColumnName, int length)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.length = Math.max(0, length);
    }

    public static TransformRight of(String... params)
    {
        if (!Is.empty(params) && params.length >= 3)
        {
            return new TransformRight(ColumnName.of(params[0]), ColumnName.of(params[1]), Integer.parseInt(params[2]));
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
        int start = Math.max(0, s.length() - this.length);
        return s.substring(start);
    }
}
