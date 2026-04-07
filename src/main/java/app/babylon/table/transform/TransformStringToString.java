package app.babylon.table.transform;

import java.util.Map;
import java.util.function.Function;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.Columns;
import app.babylon.table.Transformer;

abstract class TransformStringToString extends TransformBase
{
    protected final ColumnName existingColumnName;
    protected final ColumnName newColumnName;

    TransformStringToString(String name, ColumnName existingColumnName, ColumnName newColumnName)
    {
        super(name);
        this.existingColumnName = ArgumentChecks.nonNull(existingColumnName);
        this.newColumnName = newColumnName;
    }

    @Override
    public final void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.existingColumnName);
        if (!Columns.isStringColumn(column))
        {
            return;
        }
        ColumnObject<String> strings = Columns.asStringColumn(column);
        ColumnName targetColumnName = this.newColumnName == null ? this.existingColumnName : this.newColumnName;
        Function<String, String> transform = this::transformString;
        columnsByName.put(targetColumnName,
                strings.transform(Transformer.of(transform, String.class, targetColumnName)));
    }

    protected abstract String transformString(String s);
}
