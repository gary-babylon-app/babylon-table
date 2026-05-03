package app.babylon.table.transform;

import java.util.List;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class TransformToString extends TransformBase
{
    public static final String FUNCTION_NAME = "ToString";

    private final ColumnName columnName;
    private final ColumnName newColumnName;

    public TransformToString(ColumnName columnName)
    {
        this(columnName, null);
    }

    public TransformToString(ColumnName columnName, ColumnName newColumnName)
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

    public static TransformToString of(ColumnName columnName, ColumnName newColumnName)
    {
        return new TransformToString(columnName, newColumnName);
    }

    public static TransformToString of(ColumnName columnName)
    {
        return columnName == null ? null : new TransformToString(columnName);
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
        if (this.columnName != null)
        {
            Column[] validColumns = getColumns(columnsByName, List.of(this.columnName));
            Column[] newColumns = new Column[validColumns.length];

            for (int i = 0; i < validColumns.length; ++i)
            {
                Column c = validColumns[i];
                ColumnName newColumnName = this.newColumnName == null ? c.getName() : this.newColumnName;
                ColumnObject.Builder<String> cc = ColumnObject.builder(newColumnName, ColumnTypes.STRING);
                for (int j = 0; j < c.size(); ++j)
                {
                    cc.add(c.toString(j));
                }
                newColumns[i] = cc.build();

            }
            putColumns(columnsByName, newColumns);
        }
    }
}
