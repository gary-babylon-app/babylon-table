package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.column.Transformer;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.dsl.ConditionExpression;
import app.babylon.table.selection.RowPredicate;

abstract class TransformStringToString extends TransformBase implements TransformToColumn
{
    protected final ColumnName existingColumnName;
    protected final ColumnName newColumnName;
    private final ConditionExpression condition;

    TransformStringToString(String name, ColumnName existingColumnName, ColumnName newColumnName)
    {
        this(name, existingColumnName, newColumnName, null);
    }

    TransformStringToString(String name, ColumnName existingColumnName, ColumnName newColumnName,
            ConditionExpression condition)
    {
        super(name);
        this.existingColumnName = ArgumentCheck.nonNull(existingColumnName);
        this.newColumnName = newColumnName;
        this.condition = condition;
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

    public ConditionExpression condition()
    {
        return this.condition;
    }

    @Override
    public ColumnName outputColumnName()
    {
        return effectiveNewColumnName();
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        if (this.condition == null)
        {
            return List.of(this.existingColumnName);
        }
        Set<ColumnName> names = new LinkedHashSet<>();
        names.add(this.existingColumnName);
        names.add(effectiveNewColumnName());
        names.addAll(this.condition.columnNames());
        return names;
    }

    @Override
    public Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        Column column = columnsByName.get(this.existingColumnName);
        if (!Columns.isStringColumn(column))
        {
            return null;
        }
        ColumnObject<String> strings = Columns.asStringColumn(column);
        ColumnName targetColumnName = this.newColumnName == null ? this.existingColumnName : this.newColumnName;
        if (this.condition != null)
        {
            Column target = columnsByName.get(targetColumnName);
            if (target != null && !Columns.isStringColumn(target))
            {
                throw new IllegalArgumentException(
                        "Conditional string transform target column must be a string column.");
            }
            ColumnObject<String> targetStrings = target == null ? null : Columns.asStringColumn(target);
            ColumnObject.Builder<String> builder = ColumnObject.builder(targetColumnName, ColumnTypes.STRING);
            RowPredicate predicate = this.condition.prepare(columnsByName);
            for (int i = 0; i < strings.size(); ++i)
            {
                if (targetStrings != null && targetStrings.isSet(i))
                {
                    builder.add(targetStrings.get(i));
                }
                else if (predicate.test(i) && strings.isSet(i))
                {
                    builder.add(transformString(strings.get(i)));
                }
                else
                {
                    builder.addNull();
                }
            }
            return builder.build();
        }
        Function<String, String> transform = this::transformString;
        return strings.transform(Transformer.of(transform, ColumnTypes.STRING, targetColumnName));
    }

    protected abstract String transformString(String s);
}
