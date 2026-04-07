package app.babylon.table.transform;

import app.babylon.table.ColumnName;
import app.babylon.table.Is;

public class TransformLeft extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Left";
    private final int length;

    public TransformLeft(ColumnName existingColumnName, ColumnName newColumnName, int length)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.length = Math.max(0, length);
    }

    public static TransformLeft of(String... params)
    {
        if (!Is.empty(params) && params.length >= 3)
        {
            return new TransformLeft(ColumnName.of(params[0]), ColumnName.of(params[1]), Integer.parseInt(params[2]));
        }
        return null;
    }

    @Override
    protected String transformString(String s)
    {
        if (Is.empty(s))
        {
            return null;
        }
        int end = Math.min(this.length, s.length());
        return s.substring(0, end);
    }
}
