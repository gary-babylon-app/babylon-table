package app.babylon.table.transform;

import java.util.Map;
import java.util.Collection;
import java.util.List;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Sentence.ParseMode;

abstract class TransformConvert extends TransformBase implements TransformToColumn
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

    public ColumnName outputColumnName()
    {
        return effectiveNewColumnName();
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        return List.of(this.columnName);
    }

    @Override
    public Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        if (columnsByName == null)
        {
            return null;
        }
        Column source = columnsByName.get(this.columnName);
        return apply(source);
    }
}
