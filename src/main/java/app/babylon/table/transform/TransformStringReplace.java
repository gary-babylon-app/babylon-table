package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;

import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TransformStringReplace extends TransformStringToString
{
    public static final String FUNCTION_NAME = "StringReplace";

    private final String target;
    private final String replacement;

    private TransformStringReplace(ColumnName existingColumnName, String target, String replacement)
    {
        this(existingColumnName, existingColumnName, target, replacement);
    }

    private TransformStringReplace(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        super(FUNCTION_NAME, existingColumnName, ArgumentCheck.nonNull(newColumnName));
        this.target = ArgumentCheck.nonEmpty(target);
        this.replacement = ArgumentCheck.nonNull(replacement);
    }

    public static TransformStringReplace of(ColumnName existingColumnName, String target, String replacement)
    {
        if (existingColumnName == null || Strings.isEmpty(target) || replacement == null)
        {
            return null;
        }
        return new TransformStringReplace(existingColumnName, target, replacement);
    }

    public static TransformStringReplace of(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        if (existingColumnName == null || newColumnName == null || Strings.isEmpty(target) || replacement == null)
        {
            return null;
        }
        return new TransformStringReplace(existingColumnName, newColumnName, target, replacement);
    }

    public static TransformStringReplace of(String... params)
    {
        if (Is.empty(params) || params.length < 4)
        {
            return null;
        }
        ColumnName existingColumnName = ColumnName.parse(params[0]);
        ColumnName newColumnName = ColumnName.parse(params[1]);
        return of(existingColumnName, newColumnName, params[2], params[3]);
    }

    public String target()
    {
        return this.target;
    }

    public String replacement()
    {
        return this.replacement;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        String replaced = s.replace(this.target, this.replacement);
        return Strings.isEmpty(replaced) ? null : replaced;
    }
}
