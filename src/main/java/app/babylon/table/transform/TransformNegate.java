package app.babylon.table.transform;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnByte;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformNegate extends TransformBase
{
    public static final String FUNCTION_NAME = "Negate";

    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final ColumnName conditionColumnName;
    private final String conditionValue;

    private TransformNegate(ColumnName columnName, ColumnName newColumnName)
    {
        this(columnName, newColumnName, null, null);
    }

    private TransformNegate(ColumnName columnName, ColumnName newColumnName, ColumnName conditionColumnName,
            String conditionValue)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = newColumnName;
        this.conditionColumnName = conditionColumnName;
        this.conditionValue = conditionValue;
    }

    public static TransformNegate of(ColumnName columnName)
    {
        return columnName == null ? null : new TransformNegate(columnName, null);
    }

    public static TransformNegate of(ColumnName columnName, ColumnName newColumnName)
    {
        return columnName == null || newColumnName == null ? null : new TransformNegate(columnName, newColumnName);
    }

    public static TransformNegate when(ColumnName columnName, ColumnName conditionColumnName, String conditionValue)
    {
        return when(columnName, null, conditionColumnName, conditionValue);
    }

    public static TransformNegate when(ColumnName columnName, ColumnName newColumnName, ColumnName conditionColumnName,
            String conditionValue)
    {
        return columnName == null || conditionColumnName == null || conditionValue == null
                ? null
                : new TransformNegate(columnName, newColumnName, conditionColumnName, conditionValue);
    }

    public static TransformNegate of(String... params)
    {
        if (Is.empty(params))
        {
            return null;
        }
        if (params.length >= 2)
        {
            return of(ColumnName.parse(params[0]), ColumnName.parse(params[1]));
        }
        return of(ColumnName.parse(params[0]));
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

    public ColumnName conditionColumnName()
    {
        return this.conditionColumnName;
    }

    public String conditionValue()
    {
        return this.conditionValue;
    }

    public ColumnObject<BigDecimal> apply(Column column)
    {
        if (column == null)
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;
        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(bd.negate());
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
    }

    public ColumnObject<BigDecimal> apply(Column column, Column conditionColumn)
    {
        if (column == null || conditionColumn == null)
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> oldColumn = (ColumnObject<BigDecimal>) column;
        Object condition = parseConditionValue(conditionColumn);
        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(effectiveNewColumnName());
        for (int i = 0; i < oldColumn.size(); ++i)
        {
            if (oldColumn.isSet(i))
            {
                BigDecimal bd = oldColumn.get(i);
                newColumn.add(matches(conditionColumn, i, condition) ? bd.negate() : bd);
            }
            else
            {
                newColumn.addNull();
            }
        }
        return newColumn.build();
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnName);
        if (column != null)
        {
            ColumnObject<BigDecimal> transformed = this.conditionColumnName == null
                    ? apply(column)
                    : apply(column, columnsByName.get(this.conditionColumnName));
            if (transformed != null)
            {
                columnsByName.put(effectiveNewColumnName(), transformed);
            }
        }
    }

    private Object parseConditionValue(Column conditionColumn)
    {
        Object value = conditionColumn.getType().getParser().parse(this.conditionValue);
        if (value == null && !this.conditionValue.isEmpty())
        {
            throw new RuntimeException(FUNCTION_NAME + " could not parse condition value '" + this.conditionValue
                    + "' as " + conditionColumn.getType());
        }
        return value;
    }

    private boolean matches(Column conditionColumn, int row, Object condition)
    {
        return conditionColumn.isSet(row) && Objects.equals(value(conditionColumn, row), condition);
    }

    private Object value(Column column, int row)
    {
        if (column instanceof ColumnObject<?> objectColumn)
        {
            return objectColumn.get(row);
        }
        if (column instanceof ColumnInt intColumn)
        {
            return intColumn.get(row);
        }
        if (column instanceof ColumnLong longColumn)
        {
            return longColumn.get(row);
        }
        if (column instanceof ColumnDouble doubleColumn)
        {
            return doubleColumn.get(row);
        }
        if (column instanceof ColumnByte byteColumn)
        {
            return byteColumn.get(row);
        }
        return column.toString(row);
    }
}
