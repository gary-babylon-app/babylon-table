package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Sentence.ParseMode;

abstract class TransformConvert extends TransformBase
{
    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final Column.Type type;
    private final ParseMode parseMode;

    protected TransformConvert(String name, ColumnName columnName, ColumnName newColumnName, Column.Type type,
            ParseMode parseMode)
    {
        super(name);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = newColumnName;
        this.type = ArgumentCheck.nonNull(type);
        this.parseMode = parseMode;
    }

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public ColumnName effectiveNewColumnName()
    {
        return this.newColumnName == null ? this.columnName : this.newColumnName;
    }

    public Column.Type type()
    {
        return this.type;
    }

    public ParseMode parseMode()
    {
        return this.parseMode;
    }

    public ParseMode effectiveParseMode()
    {
        return this.parseMode == null ? ParseMode.EXACT : this.parseMode;
    }

    public abstract Column apply(Column column);

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null)
        {
            return;
        }
        Column source = columnsByName.get(this.columnName);
        Column transformed = apply(source);
        if (transformed != null)
        {
            columnsByName.put(effectiveNewColumnName(), transformed);
        }
    }
}
