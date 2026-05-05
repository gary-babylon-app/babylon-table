package app.babylon.table.transform;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

public class TransformExtractFromColumnName extends TransformBase
{
    public static final String FUNCTION_NAME = "ExtractFromColumnName";

    private final ColumnName sourceColumnName;
    private final Pattern pattern;
    private final Column.Type type;
    private final ColumnName newColumnName;

    private TransformExtractFromColumnName(ColumnName sourceColumnName, Pattern pattern, Column.Type type,
            ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.sourceColumnName = ArgumentCheck.nonNull(sourceColumnName, "sourceColumnName must not be null");
        this.pattern = ArgumentCheck.nonNull(pattern, "pattern must not be null");
        this.type = ArgumentCheck.nonNull(type, "type must not be null");
        this.newColumnName = ArgumentCheck.nonNull(newColumnName, "newColumnName must not be null");
    }

    public static TransformExtractFromColumnName of(ColumnName sourceColumnName, Pattern pattern,
            ColumnName newColumnName)
    {
        return of(sourceColumnName, pattern, ColumnTypes.STRING, newColumnName);
    }

    public static TransformExtractFromColumnName of(ColumnName sourceColumnName, Pattern pattern, Column.Type type,
            ColumnName newColumnName)
    {
        if (sourceColumnName == null || pattern == null || type == null || newColumnName == null)
        {
            return null;
        }
        return new TransformExtractFromColumnName(sourceColumnName, pattern, type, newColumnName);
    }

    public ColumnName sourceColumnName()
    {
        return this.sourceColumnName;
    }

    public Pattern pattern()
    {
        return this.pattern;
    }

    public Column.Type type()
    {
        return this.type;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null || !columnsByName.containsKey(this.sourceColumnName))
        {
            return;
        }
        TransformConstant.of(this.type, this.newColumnName, extractedValue()).apply(columnsByName);
    }

    private String extractedValue()
    {
        Matcher matcher = this.pattern.matcher(this.sourceColumnName.getValue());
        if (!matcher.find())
        {
            return null;
        }
        return matcher.groupCount() == 0 ? matcher.group() : matcher.group(1);
    }
}
