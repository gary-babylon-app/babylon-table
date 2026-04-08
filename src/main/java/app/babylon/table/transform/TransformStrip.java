package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.ColumnName;
import app.babylon.table.Is;

public class TransformStrip extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Strip";

    public TransformStrip(ColumnName existingColumnName)
    {
        this(existingColumnName, existingColumnName);
    }

    public TransformStrip(ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
    }

    public static TransformStrip of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        if (params.length == 1)
        {
            ColumnName columnName = ColumnName.of(params[0]);
            return new TransformStrip(columnName);
        }
        if (params.length >= 2)
        {
            return new TransformStrip(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    @Override
    protected String transformString(String s)
    {
        return Strings.isEmpty(s) ? s : s.strip();
    }
}
