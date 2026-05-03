package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

abstract class TransformConvert extends TransformBase
{
    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final Column.Type type;
    private final TransformParseMode parseMode;

    protected TransformConvert(String name, ColumnName columnName, ColumnName newColumnName, Column.Type type,
            TransformParseMode parseMode)
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

    public TransformParseMode parseMode()
    {
        return this.parseMode;
    }

    public TransformParseMode effectiveParseMode()
    {
        return this.parseMode == null ? TransformParseMode.EXACT : this.parseMode;
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
