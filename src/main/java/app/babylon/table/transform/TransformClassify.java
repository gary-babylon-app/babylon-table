package app.babylon.table.transform;

import java.util.regex.Pattern;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.ColumnName;
import app.babylon.table.Is;

public class TransformClassify extends TransformStringToString
{
    public final static String FUNCTION_NAME = "Classify";
    private final Pattern pattern;
    private final String newColumnFoundValue;
    private final String newColumnNotFoundValue;

    public TransformClassify(ColumnName existingColumnName, ColumnName newColumnName, Pattern pattern, String newColumnFoundValue, String newColumnNotFoundValue)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName==null?existingColumnName:newColumnName);
        this.pattern = ArgumentChecks.nonNull(pattern);
        this.newColumnFoundValue = newColumnFoundValue;
        this.newColumnNotFoundValue = newColumnNotFoundValue;
    }

    public static TransformClassify of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 5)
        {
            ColumnName existingColumnName = ColumnName.of(params[0]);
            ColumnName newColumnName = ColumnName.of(params[1]);
            Pattern pattern = Pattern.compile(params[2]);
            String foundValue = params[3];
            String notFoundValue = params[4];
            return new TransformClassify(existingColumnName, newColumnName, pattern, foundValue, notFoundValue);
        }
        return null;
    }

    @Override
    protected String transformString(String s)
    {
        if (Is.empty(s))
        {
            return s;
        }
        if (pattern.matcher(s).find())
        {
            return newColumnFoundValue;
        }
        return newColumnNotFoundValue;
    }
}
