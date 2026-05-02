package app.babylon.table.transform;

import app.babylon.text.Strings;

import app.babylon.table.column.ColumnName;
import app.babylon.lang.Is;

public class TransformStrip extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Strip";
    private final String stripCharacters;

    public TransformStrip(ColumnName existingColumnName)
    {
        this(existingColumnName, null, null);
    }

    public TransformStrip(ColumnName existingColumnName, ColumnName newColumnName)
    {
        this(existingColumnName, newColumnName, null);
    }

    public TransformStrip(ColumnName existingColumnName, ColumnName newColumnName, String stripCharacters)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.stripCharacters = stripCharacters;
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

    public static TransformStrip of(ColumnName existingColumnName)
    {
        return new TransformStrip(existingColumnName);
    }

    public static TransformStrip of(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return new TransformStrip(existingColumnName, newColumnName);
    }

    public static TransformStrip of(ColumnName existingColumnName, ColumnName newColumnName, String stripCharacters)
    {
        return new TransformStrip(existingColumnName, newColumnName, stripCharacters);
    }

    public String stripCharacters()
    {
        return this.stripCharacters;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return s;
        }
        if (this.stripCharacters == null)
        {
            return s.strip();
        }
        int start = 0;
        int end = s.length();
        while (start < end && contains(this.stripCharacters, s.charAt(start)))
        {
            ++start;
        }
        while (end > start && contains(this.stripCharacters, s.charAt(end - 1)))
        {
            --end;
        }
        return s.substring(start, end);
    }

    private static boolean contains(String characters, char c)
    {
        for (int i = 0; i < characters.length(); ++i)
        {
            if (characters.charAt(i) == c)
            {
                return true;
            }
        }
        return false;
    }
}
