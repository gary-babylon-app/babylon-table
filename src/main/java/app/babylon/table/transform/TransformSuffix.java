package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.lang.Is;

public class TransformSuffix extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Suffix";

    private final String suffix;

    public TransformSuffix(String suffix, ColumnName existingColumnName)
    {
        this(suffix, existingColumnName, null);
    }

    public TransformSuffix(String suffix, ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.suffix = ArgumentCheck.nonEmpty(suffix);
    }

    public static TransformSuffix of(String... params)
    {
        if (Is.empty(params) || params.length < 2)
        {
            return null;
        }
        if (params.length == 2)
        {
            return of(params[0], ColumnName.of(params[1]));
        }
        return of(params[0], ColumnName.of(params[1]), ColumnName.of(params[2]));
    }

    public static TransformSuffix of(String suffix, ColumnName existingColumnName)
    {
        return new TransformSuffix(suffix, existingColumnName);
    }

    public static TransformSuffix of(String suffix, ColumnName existingColumnName, ColumnName newColumnName)
    {
        return new TransformSuffix(suffix, existingColumnName, newColumnName);
    }

    public String suffix()
    {
        return this.suffix;
    }

    @Override
    protected String transformString(String s)
    {
        return Strings.isEmpty(s) ? null : s.concat(this.suffix);
    }
}
