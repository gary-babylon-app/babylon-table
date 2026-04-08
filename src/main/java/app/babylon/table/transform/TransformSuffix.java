package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.table.Is;

public class TransformSuffix extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Suffix";

    private final String suffix;

    public TransformSuffix(String suffix, ColumnName existingColumnName)
    {
        this(suffix, existingColumnName, existingColumnName);
    }

    public TransformSuffix(String suffix, ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.suffix = app.babylon.table.ArgumentChecks.nonEmpty(suffix);
    }

    public static TransformSuffix of(String... params)
    {
        if (Is.empty(params) || params.length < 2)
        {
            return null;
        }
        if (params.length == 2)
        {
            return new TransformSuffix(params[0], ColumnName.of(params[1]));
        }
        return new TransformSuffix(params[0], ColumnName.of(params[1]), ColumnName.of(params[2]));
    }

    @Override
    protected String transformString(String s)
    {
        return Strings.isEmpty(s) ? null : s.concat(this.suffix);
    }
}
