package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.regex.Pattern;

import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TransformClassify extends TransformStringToString
{
    public final static String FUNCTION_NAME = "Classify";
    private final Pattern pattern;
    private final String newColumnFoundValue;
    private final String newColumnNotFoundValue;

    public TransformClassify(ColumnName existingColumnName, ColumnName newColumnName, Pattern pattern,
            String newColumnFoundValue, String newColumnNotFoundValue)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName == null ? existingColumnName : newColumnName);
        this.pattern = ArgumentCheck.nonNull(pattern);
        this.newColumnFoundValue = newColumnFoundValue;
        this.newColumnNotFoundValue = newColumnNotFoundValue;
    }

    public static TransformClassify of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 5)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]), Pattern.compile(params[2]), params[3],
                    params[4]);
        }
        return null;
    }

    public static TransformClassify of(ColumnName existingColumnName, ColumnName newColumnName, Pattern pattern,
            String newColumnFoundValue, String newColumnNotFoundValue)
    {
        return new TransformClassify(existingColumnName, newColumnName, pattern, newColumnFoundValue,
                newColumnNotFoundValue);
    }

    public String newColumnNotFoundValue()
    {
        return this.newColumnNotFoundValue;
    }

    public Pattern pattern()
    {
        return this.pattern;
    }

    public String newColumnFoundValue()
    {
        return this.newColumnFoundValue;
    }

    public String effectiveNewColumnNotFoundValue()
    {
        return this.newColumnNotFoundValue == null ? "" : this.newColumnNotFoundValue;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return s;
        }
        if (pattern.matcher(s).find())
        {
            return newColumnFoundValue;
        }
        return effectiveNewColumnNotFoundValue();
    }
}
