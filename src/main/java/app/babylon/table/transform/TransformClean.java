package app.babylon.table.transform;

import app.babylon.lang.Is;
import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;

public class TransformClean extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Clean";
    private final String cleanCharacters;

    public TransformClean(ColumnName existingColumnName)
    {
        this(existingColumnName, null, null);
    }

    public TransformClean(ColumnName existingColumnName, ColumnName newColumnName)
    {
        this(existingColumnName, newColumnName, null);
    }

    public TransformClean(ColumnName existingColumnName, ColumnName newColumnName, String cleanCharacters)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.cleanCharacters = cleanCharacters;
    }

    public static TransformClean of(String... params)
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
            if (params.length >= 3)
            {
                return of(ColumnName.of(params[0]), ColumnName.of(params[1]), params[2]);
            }
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public static TransformClean of(ColumnName existingColumnName)
    {
        return new TransformClean(existingColumnName);
    }

    public static TransformClean of(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return new TransformClean(existingColumnName, newColumnName);
    }

    public static TransformClean of(ColumnName existingColumnName, ColumnName newColumnName, String cleanCharacters)
    {
        return new TransformClean(existingColumnName, newColumnName, cleanCharacters);
    }

    public String cleanCharacters()
    {
        return this.cleanCharacters;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return s;
        }
        return this.cleanCharacters == null ? Strings.clean(s) : Strings.clean(s, this.cleanCharacters.toCharArray());
    }
}
