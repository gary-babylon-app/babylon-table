package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnByte;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;

public class TransformCreateConstant extends TransformBase
{
    public static final String FUNCTION_NAME = "CreateConstant";

    private final ColumnName[] newColumnNames;
    private final Object[] newColumnValues;
    private final Column.Type[] types;

    public TransformCreateConstant(ColumnName newColumnName, String newColumnValue)
    {
        super(FUNCTION_NAME);
        this.newColumnNames = new ColumnName[]
        {newColumnName};
        this.newColumnValues = new Object[]
        {newColumnValue};
        this.types = new Column.Type[]
        {ColumnTypes.STRING};
    }

    public <T> TransformCreateConstant(Column.Type type, ColumnName newColumnName, T newColumnValue)
    {
        super(FUNCTION_NAME);
        this.newColumnNames = new ColumnName[]
        {newColumnName};
        this.newColumnValues = new Object[]
        {newColumnValue};
        this.types = new Column.Type[]
        {type};
    }

    public TransformCreateConstant(ColumnName[] newColumnNames, Object[] newColumnValues, Column.Type[] types)
    {
        super(FUNCTION_NAME);
        this.newColumnNames = Arrays.copyOf(newColumnNames, newColumnNames.length);
        this.newColumnValues = Arrays.copyOf(newColumnValues, newColumnValues.length);
        this.types = Arrays.copyOf(types, types.length);
    }

    public static TransformCreateConstant of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return of(ColumnName.parse(params[0]), params[1]);
        }
        return null;
    }

    public static TransformCreateConstant of(ColumnName newColumnName, String newColumnValue)
    {
        return new TransformCreateConstant(newColumnName, newColumnValue);
    }

    public static <T> TransformCreateConstant of(Column.Type type, ColumnName newColumnName, T newColumnValue)
    {
        return new TransformCreateConstant(type, newColumnName, newColumnValue);
    }

    public ColumnName[] newColumnNames()
    {
        return Arrays.copyOf(this.newColumnNames, this.newColumnNames.length);
    }

    public Object[] newColumnValues()
    {
        return Arrays.copyOf(this.newColumnValues, this.newColumnValues.length);
    }

    public Column.Type[] types()
    {
        return Arrays.copyOf(this.types, this.types.length);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null)
        {
            return;
        }
        int rowCount = rowCount(columnsByName);
        Column[] newColumns = new Column[newColumnNames.length];
        for (int i = 0; i < newColumnNames.length; ++i)
        {
            newColumns[i] = newConstantColumn(newColumnNames[i], newColumnValues[i], rowCount, types[i]);
        }
        putColumns(columnsByName, newColumns);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(FUNCTION_NAME);
        builder.append("(");
        for (int i = 0; i < this.newColumnNames.length; ++i)
        {
            if (i != 0)
            {
                builder.append(", ");
            }
            builder.append(this.newColumnNames[i].getValue());
            builder.append(", ");
            builder.append(this.newColumnValues[i]);
        }
        builder.append(")");
        return builder.toString();
    }

    private static Column newConstantColumn(ColumnName columnName, Object value, int rowCount, Column.Type type)
    {
        Column.Builder builder = Columns.newBuilder(columnName, type);
        for (int i = 0; i < rowCount; ++i)
        {
            addValue(builder, value);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static void addValue(Column.Builder builder, Object value)
    {
        if (builder instanceof ColumnByte.Builder byteBuilder)
        {
            if (value == null)
            {
                byteBuilder.addNull();
            }
            else
            {
                byteBuilder.add(((Byte) value).byteValue());
            }
        }
        else if (builder instanceof ColumnBoolean.Builder booleanBuilder)
        {
            if (value == null)
            {
                booleanBuilder.addNull();
            }
            else
            {
                booleanBuilder.add(((Boolean) value).booleanValue());
            }
        }
        else if (builder instanceof ColumnInt.Builder intBuilder)
        {
            if (value == null)
            {
                intBuilder.addNull();
            }
            else
            {
                intBuilder.add(((Integer) value).intValue());
            }
        }
        else if (builder instanceof ColumnLong.Builder longBuilder)
        {
            if (value == null)
            {
                longBuilder.addNull();
            }
            else
            {
                longBuilder.add(((Long) value).longValue());
            }
        }
        else if (builder instanceof ColumnDouble.Builder doubleBuilder)
        {
            if (value == null)
            {
                doubleBuilder.addNull();
            }
            else
            {
                doubleBuilder.add(((Double) value).doubleValue());
            }
        }
        else if (builder instanceof ColumnObject.Builder<?> objectBuilder)
        {
            ((ColumnObject.Builder<Object>) objectBuilder).add(value);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported builder " + builder.getClass().getName());
        }
    }

}
