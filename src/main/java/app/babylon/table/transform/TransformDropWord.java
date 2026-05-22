package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Sentence;
import app.babylon.text.Strings;

public class TransformDropWord extends TransformStringToString
{
    public static final String FUNCTION_NAME = "DropWord";

    public enum Position
    {
        FIRST, LAST
    }

    private final Position position;

    private TransformDropWord(ColumnName existingColumnName, ColumnName newColumnName, Position position)
    {
        super(FUNCTION_NAME, existingColumnName, newColumnName);
        this.position = ArgumentCheck.nonNull(position);
    }

    public static TransformDropWord first(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return of(existingColumnName, newColumnName, Position.FIRST);
    }

    public static TransformDropWord last(ColumnName existingColumnName, ColumnName newColumnName)
    {
        return of(existingColumnName, newColumnName, Position.LAST);
    }

    public static TransformDropWord of(ColumnName existingColumnName, ColumnName newColumnName, Position position)
    {
        if (existingColumnName == null || position == null)
        {
            return null;
        }
        return new TransformDropWord(existingColumnName, newColumnName, position);
    }

    public static TransformDropWord of(String... params)
    {
        if (Is.empty(params) || params.length < 3)
        {
            return null;
        }
        Position position = Position.valueOf(params[2].strip().toUpperCase());
        return of(ColumnName.parse(params[0]), ColumnName.parse(params[1]), position);
    }

    public Position position()
    {
        return this.position;
    }

    @Override
    protected String transformString(String s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        return switch (this.position)
        {
            case FIRST -> Sentence.dropFirstWord(s);
            case LAST -> Sentence.dropLastWord(s);
        };
    }
}
