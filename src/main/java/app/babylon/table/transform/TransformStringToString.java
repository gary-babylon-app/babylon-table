package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.column.Transformer;
import app.babylon.table.column.ColumnTypes;

abstract class TransformStringToString extends TransformBase implements TransformToColumn
{
    protected final ColumnName existingColumnName;
    protected final ColumnName newColumnName;

    TransformStringToString(String name, ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(name);
        this.existingColumnName = ArgumentCheck.nonNull(existingColumnName);
        this.newColumnName = newColumnName;
    }

    public ColumnName existingColumnName()
    {
        return this.existingColumnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public ColumnName effectiveNewColumnName()
    {
        return this.newColumnName == null ? this.existingColumnName : this.newColumnName;
    }

    @Override
    public ColumnName outputColumnName()
    {
        return effectiveNewColumnName();
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        return List.of(this.existingColumnName);
    }

    @Override
    public final Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        Column column = columnsByName.get(this.existingColumnName);
        if (!Columns.isStringColumn(column))
        {
            return null;
        }
        ColumnObject<String> strings = Columns.asStringColumn(column);
        ColumnName targetColumnName = this.newColumnName == null ? this.existingColumnName : this.newColumnName;
        Function<String, String> transform = this::transformString;
        return strings.transform(Transformer.of(transform, ColumnTypes.STRING, targetColumnName));
    }

    protected abstract String transformString(String s);
}
