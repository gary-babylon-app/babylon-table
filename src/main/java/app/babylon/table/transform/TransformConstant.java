package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.table.column.type.TypeParser;
import app.babylon.text.Strings;

public class TransformConstant extends TransformBase
{
    public static final String FUNCTION_NAME = "Constant";
    public static final String LEGACY_FUNCTION_NAME = "CreateConstant";

    private final ColumnName newColumnName;
    private final String value;
    private final Column.Type type;

    public TransformConstant(ColumnName newColumnName, String value)
    {
        this(ColumnTypes.STRING, newColumnName, value);
    }

    public TransformConstant(Column.Type type, ColumnName newColumnName, String value)
    {
        super(FUNCTION_NAME);
        this.type = ArgumentCheck.nonNull(type, "type must not be null");
        this.newColumnName = ArgumentCheck.nonNull(newColumnName, "newColumnName must not be null");
        this.value = value;
        validateValue();
    }

    public static TransformConstant of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return of(ColumnName.parse(params[0]), params[1]);
        }
        return null;
    }

    public static TransformConstant of(ColumnName newColumnName, String value)
    {
        return new TransformConstant(newColumnName, value);
    }

    public static TransformConstant of(Column.Type type, ColumnName newColumnName, String value)
    {
        return new TransformConstant(type, newColumnName, value);
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public String value()
    {
        return this.value;
    }

    public Column.Type type()
    {
        return this.type;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null)
        {
            return;
        }
        columnsByName.put(this.newColumnName, newConstantColumn(rowCount(columnsByName)));
    }

    @Override
    public String toString()
    {
        return FUNCTION_NAME + "(" + this.newColumnName.getValue() + ", " + this.value + ")";
    }

    private Column newConstantColumn(int rowCount)
    {
        if (Strings.isEmpty(this.value))
        {
            return newNullColumn(rowCount);
        }
        TypeParser<?> parser = this.type.getParser();
        Class<?> valueClass = this.type.getValueClass();
        if (boolean.class.equals(valueClass))
        {
            return Columns.newBoolean(this.newColumnName, parser.parseBoolean(this.value), rowCount);
        }
        if (byte.class.equals(valueClass))
        {
            return Columns.newByte(this.newColumnName, parser.parseByte(this.value), rowCount);
        }
        if (int.class.equals(valueClass))
        {
            return Columns.newInt(this.newColumnName, parser.parseInt(this.value), rowCount);
        }
        if (long.class.equals(valueClass))
        {
            return Columns.newLong(this.newColumnName, parser.parseLong(this.value), rowCount);
        }
        if (double.class.equals(valueClass))
        {
            return Columns.newDouble(this.newColumnName, parser.parseDouble(this.value), rowCount);
        }
        if (ColumnTypes.STRING.equals(this.type))
        {
            return Columns.newString(this.newColumnName, this.value, rowCount);
        }
        return newObjectColumn(rowCount);
    }

    private Column newObjectColumn(int rowCount)
    {
        Object parsed = this.type.getParser().parse(this.value);
        return Columns.newCategorical(this.newColumnName, parsed, rowCount, this.type);
    }

    private Column newNullColumn(int rowCount)
    {
        Column.Builder builder = Columns.newBuilder(this.newColumnName, this.type);
        for (int row = 0; row < rowCount; ++row)
        {
            builder.addNull();
        }
        return builder.build();
    }

    private void validateValue()
    {
        if (Strings.isEmpty(this.value))
        {
            return;
        }
        Column column = newConstantColumn(1);
        if (!column.isSet(0))
        {
            throw new IllegalArgumentException("Could not parse constant value '" + this.value + "' as " + this.type);
        }
    }

}
