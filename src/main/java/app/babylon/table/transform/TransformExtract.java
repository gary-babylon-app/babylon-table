package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TransformExtract extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Extract";

    private final Pattern pattern;

    private TransformExtract(ColumnName extractColumnName, Pattern pattern)
    {
        this(extractColumnName, pattern, null);
    }

    private TransformExtract(ColumnName extractColumnName, Pattern pattern, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, extractColumnName, newColumnName == null ? extractColumnName : newColumnName);
        this.pattern = ArgumentCheck.nonNull(pattern);
    }

    public static TransformExtract of(ColumnName extractColumnName, Pattern pattern)
    {
        if (extractColumnName == null || pattern == null)
        {
            return null;
        }
        return new TransformExtract(extractColumnName, pattern);
    }

    public static TransformExtract of(ColumnName extractColumnName, ColumnName newColumnName, Pattern pattern)
    {
        if (extractColumnName == null || newColumnName == null || pattern == null)
        {
            return null;
        }
        return new TransformExtract(extractColumnName, pattern, newColumnName);
    }

    public static TransformExtract of(String... params)
    {
        if (!Is.empty(params) && params.length >= 3)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]), Pattern.compile(params[2]));
        }
        return null;
    }

    public Pattern pattern()
    {
        return this.pattern;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        Matcher matcher = pattern.matcher(s);
        if (matcher.matches())
        {
            return matcher.group(1);
        }
        return null;
    }
}
