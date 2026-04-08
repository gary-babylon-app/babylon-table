package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.ColumnName;
import app.babylon.table.Is;

public class TransformExtract extends TransformStringToString
{
    public static String FUNCTION_NAME = "Extract";

    private final Pattern pattern;

    public TransformExtract(ColumnName extractColumnName, Pattern pattern)
    {
        this(extractColumnName, pattern, null);
    }

    public TransformExtract(ColumnName extractColumnName, Pattern pattern, ColumnName newColumnName)
    {
        super(FUNCTION_NAME, extractColumnName, newColumnName == null ? extractColumnName : newColumnName);
        this.pattern = ArgumentChecks.nonNull(pattern);
    }

    public static TransformExtract of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 3)
        {
            ColumnName columnName = ColumnName.of(params[0]);
            ColumnName newColumnName = ColumnName.of(params[1]);
            Pattern pattern = Pattern.compile(params[2]);

            return new TransformExtract(columnName, pattern, newColumnName);
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
        Matcher matcher = pattern.matcher(s);
        if (matcher.matches())
        {
            return matcher.group(1);
        }
        return null;
    }
}
