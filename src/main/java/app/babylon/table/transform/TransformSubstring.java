package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TransformSubstring extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Substring";

    private final int first;
    private final int last;

    public TransformSubstring(ColumnName existingColumnName, ColumnName newColumnName, int first, int last)
    {
        super(FUNCTION_NAME, existingColumnName, ArgumentCheck.nonNull(newColumnName));
        this.first = Math.max(0, first);
        this.last = Math.max(this.first + 1, last);
    }

    public static TransformSubstring of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 4)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]), Integer.parseInt(params[2]),
                    Integer.parseInt(params[3]));
        }
        return null;
    }

    public static TransformSubstring of(ColumnName existingColumnName, ColumnName newColumnName, int first, int last)
    {
        return new TransformSubstring(existingColumnName, newColumnName, first, last);
    }

    public int first()
    {
        return this.first;
    }

    public int last()
    {
        return this.last;
    }

    @Override
    protected String transformString(String s)
    {
        return !Strings.isEmpty(s) && s.length() >= this.last ? s.substring(this.first, this.last) : null;
    }

}
