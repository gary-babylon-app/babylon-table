package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;

import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TransformStringReplaceAll extends TransformStringToString
{
    public static final String FUNCTION_NAME = "StringReplaceAll";

    private final String target;
    private final String replacement;

    private TransformStringReplaceAll(ColumnName existingColumnName, String target, String replacement)
    {
        this(existingColumnName, existingColumnName, target, replacement);
    }

    private TransformStringReplaceAll(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        super(FUNCTION_NAME, existingColumnName, ArgumentCheck.nonNull(newColumnName));
        this.target = ArgumentCheck.nonEmpty(target);
        this.replacement = ArgumentCheck.nonNull(replacement);
    }

    public static TransformStringReplaceAll of(ColumnName existingColumnName, String target, String replacement)
    {
        if (existingColumnName == null || Strings.isEmpty(target) || replacement == null)
        {
            return null;
        }
        return new TransformStringReplaceAll(existingColumnName, target, replacement);
    }

    public static TransformStringReplaceAll of(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        if (existingColumnName == null || newColumnName == null || Strings.isEmpty(target) || replacement == null)
        {
            return null;
        }
        return new TransformStringReplaceAll(existingColumnName, newColumnName, target, replacement);
    }

    public static TransformStringReplaceAll of(String... params)
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
        if (s == null)
        {
            return null;
        }
        String replaced = s.replaceAll(this.target, this.replacement);
        return Strings.isEmpty(replaced) ? null : replaced;
    }
}
