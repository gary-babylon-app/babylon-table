package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.lang.Is;

public class TransformCreateConstant extends TransformBase
{
    public static final String FUNCTION_NAME = "CreateConstant";

    private final ColumnName[] newColumnNames;
    private final Object[] newColumnValues;
    private final Class<?>[] valueClasses;

    public TransformCreateConstant(ColumnName newColumnName, String newColumnValue)
    {
        super(FUNCTION_NAME);
        this.newColumnNames = new ColumnName[]
        {newColumnName};
        this.newColumnValues = new Object[]
        {newColumnValue};
        this.valueClasses = new Class<?>[]
        {String.class};
    }

    public <T> TransformCreateConstant(Class<T> valueClass, ColumnName newColumnName, T newColumnValue)
    {
        super(FUNCTION_NAME);
        this.newColumnNames = new ColumnName[]
        {newColumnName};
        this.newColumnValues = new Object[]
        {newColumnValue};
        this.valueClasses = new Class<?>[]
        {valueClass};
    }

    public TransformCreateConstant(ColumnName[] newColumnNames, Object[] newColumnValues, Class<?>[] valueClasses)
    {
        super(FUNCTION_NAME);
        this.newColumnNames = Arrays.copyOf(newColumnNames, newColumnNames.length);
        this.newColumnValues = Arrays.copyOf(newColumnValues, newColumnValues.length);
        this.valueClasses = Arrays.copyOf(valueClasses, valueClasses.length);
    }

    public static TransformCreateConstant of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            ColumnName newColumnName = ColumnName.parse(params[0]);
            String value = (params[1]);
            return new TransformCreateConstant(newColumnName, value);
        }
        return null;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null)
        {
            return;
        }
        int rowCount = columnsByName.isEmpty() ? 0 : columnsByName.values().iterator().next().size();
        Column[] newColumns = new Column[newColumnNames.length];
        for (int i = 0; i < newColumnNames.length; ++i)
        {
            newColumns[i] = newConstantColumn(newColumnNames[i], newColumnValues[i], rowCount, valueClasses[i]);
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

    private static Column newConstantColumn(ColumnName columnName, Object value, int rowCount, Class<?> valueClass)
    {
        return newConstantColumnTyped(columnName, value, rowCount, valueClass);
    }

    @SuppressWarnings(
    {"unchecked", "rawtypes"})
    private static <T> ColumnObject<T> newConstantColumnTyped(ColumnName columnName, Object value, int rowCount,
            Class<?> valueClass)
    {
        return Columns.newCategorical(columnName, (T) value, rowCount, (Class) valueClass);
    }

}
