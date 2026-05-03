package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class TransformAnyToString extends TransformBase
{
    public static final String FUNCTION_NAME = "ToString";

    private final ColumnName columnName;
    private final ColumnName newColumnName;

    public TransformAnyToString(ColumnName columnName)
    {
        this(columnName, null);
    }

    public TransformAnyToString(ColumnName columnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = newColumnName;
    }

    public static Transform of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            return of(ColumnName.parse(params[0]), ColumnName.parse(params[1]));
        }
        return null;
    }

    public static TransformAnyToString of(ColumnName columnName, ColumnName newColumnName)
    {
        return new TransformAnyToString(columnName, newColumnName);
    }

    public static TransformAnyToString of(ColumnName columnName)
    {
        return columnName == null ? null : new TransformAnyToString(columnName);
    }

    public ColumnName[] columnNames()
    {
        return new ColumnName[]
        {this.columnName};
    }

    public Map<ColumnName, ColumnName> newColumnNames()
    {
        return this.newColumnName == null ? Map.of() : Map.of(this.columnName, this.newColumnName);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnName);
        if (column == null)
        {
            return;
        }
        ColumnName targetColumnName = this.newColumnName == null ? column.getName() : this.newColumnName;
        ColumnObject.Builder<String> builder = ColumnObject.builder(targetColumnName, ColumnTypes.STRING);
        for (int row = 0; row < column.size(); ++row)
        {
            builder.add(column.toString(row));
        }
        columnsByName.put(targetColumnName, builder.build());
    }
}
