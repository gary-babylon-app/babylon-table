package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.table.dsl.ConditionExpression;
import app.babylon.lang.Is;

public class TransformBefore extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Before";
    private final String delimiter;

    public TransformBefore(ColumnName existingColumnName, ColumnName newColumnName, String delimiter)
    {
        this(existingColumnName, newColumnName, delimiter, null);
    }

    public TransformBefore(ColumnName existingColumnName, ColumnName newColumnName, String delimiter,
            ConditionExpression condition)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName, condition);
        this.delimiter = ArgumentCheck.nonEmpty(delimiter);
    }

    public static TransformBefore of(String... params)
    {
        if (!Is.empty(params) && params.length >= 3)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]), params[2]);
        }
        return null;
    }

    public static TransformBefore of(ColumnName existingColumnName, ColumnName newColumnName, String delimiter)
    {
        return new TransformBefore(existingColumnName, newColumnName, delimiter);
    }

    public static TransformBefore of(ColumnName existingColumnName, ColumnName newColumnName, String delimiter,
            ConditionExpression condition)
    {
        return new TransformBefore(existingColumnName, newColumnName, delimiter, condition);
    }

    public String delimiter()
    {
        return this.delimiter;
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
