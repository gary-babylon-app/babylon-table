package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnByte;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.dsl.ConditionExpression;
import app.babylon.table.selection.RowPredicate;

public class TransformCopy extends TransformBase implements TransformToColumn
{
    public static final String FUNCTION_NAME = "Copy";

    public ColumnName columnToCopy;
    public ColumnName newCopyName;
    private final ConditionExpression condition;

    public TransformCopy(ColumnName columnToCopy, ColumnName newCopyName)
    {
        this(columnToCopy, newCopyName, null);
    }

    private TransformCopy(ColumnName columnToCopy, ColumnName newCopyName, ConditionExpression condition)
    {
        super(FUNCTION_NAME);
        this.columnToCopy = ArgumentCheck.nonNull(columnToCopy);
        this.newCopyName = ArgumentCheck.nonNull(newCopyName);
        if (this.columnToCopy.equals(this.newCopyName))
        {
            throw new IllegalArgumentException("Copy target must be different from source.");
        }
        this.condition = condition;
    }

    public static TransformCopy of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]));
        }
        return null;
    }

    public static TransformCopy of(ColumnName columnToCopy, ColumnName newCopyName)
    {
        return new TransformCopy(columnToCopy, newCopyName);
    }

    public static TransformCopy of(ColumnName columnToCopy, ColumnName newCopyName, ConditionExpression condition)
    {
        return new TransformCopy(columnToCopy, newCopyName, condition);
    }

    public ColumnName columnToCopy()
    {
        return this.columnToCopy;
    }

    public ColumnName newCopyName()
    {
        return this.newCopyName;
    }

    public ConditionExpression condition()
    {
        return this.condition;
    }

    @Override
    public ColumnName outputColumnName()
    {
        return this.newCopyName;
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        if (this.condition == null)
        {
            return List.of(this.columnToCopy);
        }
        Set<ColumnName> names = new LinkedHashSet<>();
        names.add(this.columnToCopy);
        names.add(this.newCopyName);
        names.addAll(this.condition.columnNames());
        return names;
    }

    @Override
    public Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        Column column = columnsByName.get(this.columnToCopy);
        if (column == null)
        {
            return null;
        }
        if (this.condition == null)
        {
            return column.copy(this.newCopyName);
        }
        Column target = columnsByName.get(this.newCopyName);
        if (target != null && !target.getType().equals(column.getType()))
        {
            throw new IllegalArgumentException("Conditional copy target column type must match source column type.");
        }
        RowPredicate predicate = this.condition.prepare(columnsByName);
        if (target == null)
        {
            return column.copy(this.newCopyName, predicate);
        }
        return conditionalCopy(column, target, row -> !target.isSet(row) && predicate.test(row));
    }

    private Column conditionalCopy(Column source, Column target, RowPredicate condition)
    {
        if (source instanceof ColumnObject<?> sourceObject)
        {
            return conditionalCopyObject(sourceObject, target, condition);
        }
        if (source instanceof ColumnBoolean sourceBoolean)
        {
            return conditionalCopyBoolean(sourceBoolean, target, condition);
        }
        if (source instanceof ColumnByte sourceByte)
        {
            return conditionalCopyByte(sourceByte, target, condition);
        }
        if (source instanceof ColumnInt sourceInt)
        {
            return conditionalCopyInt(sourceInt, target, condition);
        }
        if (source instanceof ColumnLong sourceLong)
        {
            return conditionalCopyLong(sourceLong, target, condition);
        }
        if (source instanceof ColumnDouble sourceDouble)
        {
            return conditionalCopyDouble(sourceDouble, target, condition);
        }
        throw new IllegalArgumentException("Unsupported conditional copy column type: " + source.getType());
    }

    private Column conditionalCopyObject(ColumnObject<?> source, Column target, RowPredicate condition)
    {
        ColumnObject<?> targetObject = target instanceof ColumnObject<?> object ? object : null;
        ColumnObject.Builder<Object> builder = objectBuilder(source, targetObject);
        for (int i = 0; i < source.size(); ++i)
        {
            if (targetObject != null && targetObject.isSet(i))
            {
                builder.add(targetObject.get(i));
            }
            else if (condition.test(i) && source.isSet(i))
            {
                builder.add(source.get(i));
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    private ColumnObject.Builder<Object> objectBuilder(ColumnObject<?> source, ColumnObject<?> target)
    {
        if (source instanceof ColumnCategorical<?> || target instanceof ColumnCategorical<?>)
        {
            return ColumnCategorical.builder(this.newCopyName, source.getType());
        }
        return ColumnObject.builder(this.newCopyName, source.getType());
    }

    private Column conditionalCopyBoolean(ColumnBoolean source, Column target, RowPredicate condition)
    {
        ColumnBoolean targetBoolean = target instanceof ColumnBoolean typed ? typed : null;
        ColumnBoolean.Builder builder = ColumnBoolean.builder(this.newCopyName);
        for (int i = 0; i < source.size(); ++i)
        {
            if (targetBoolean != null && targetBoolean.isSet(i))
            {
                builder.add(targetBoolean.get(i));
            }
            else if (condition.test(i) && source.isSet(i))
            {
                builder.add(source.get(i));
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    private Column conditionalCopyByte(ColumnByte source, Column target, RowPredicate condition)
    {
        ColumnByte targetByte = target instanceof ColumnByte typed ? typed : null;
        ColumnByte.Builder builder = ColumnByte.builder(this.newCopyName);
        for (int i = 0; i < source.size(); ++i)
        {
            if (targetByte != null && targetByte.isSet(i))
            {
                builder.add(targetByte.get(i));
            }
            else if (condition.test(i) && source.isSet(i))
            {
                builder.add(source.get(i));
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    private Column conditionalCopyInt(ColumnInt source, Column target, RowPredicate condition)
    {
        ColumnInt targetInt = target instanceof ColumnInt typed ? typed : null;
        ColumnInt.Builder builder = ColumnInt.builder(this.newCopyName);
        for (int i = 0; i < source.size(); ++i)
        {
            if (targetInt != null && targetInt.isSet(i))
            {
                builder.add(targetInt.get(i));
            }
            else if (condition.test(i) && source.isSet(i))
            {
                builder.add(source.get(i));
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    private Column conditionalCopyLong(ColumnLong source, Column target, RowPredicate condition)
    {
        ColumnLong targetLong = target instanceof ColumnLong typed ? typed : null;
        ColumnLong.Builder builder = ColumnLong.builder(this.newCopyName);
        for (int i = 0; i < source.size(); ++i)
        {
            if (targetLong != null && targetLong.isSet(i))
            {
                builder.add(targetLong.get(i));
            }
            else if (condition.test(i) && source.isSet(i))
            {
                builder.add(source.get(i));
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

    private Column conditionalCopyDouble(ColumnDouble source, Column target, RowPredicate condition)
    {
        ColumnDouble targetDouble = target instanceof ColumnDouble typed ? typed : null;
        ColumnDouble.Builder builder = ColumnDouble.builder(this.newCopyName);
        for (int i = 0; i < source.size(); ++i)
        {
            if (targetDouble != null && targetDouble.isSet(i))
            {
                builder.add(targetDouble.get(i));
            }
            else if (condition.test(i) && source.isSet(i))
            {
                builder.add(source.get(i));
            }
            else
            {
                builder.addNull();
            }
        }
        return builder.build();
    }

}
