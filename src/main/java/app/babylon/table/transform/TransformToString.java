package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformToString extends TransformBase
{
    public static final String FUNCTION_NAME = "ToString";

    private ColumnName[] columnNames;
    private Map<ColumnName, ColumnName> newColumnNames;

    public TransformToString(ColumnName... columnNames)
    {
        super(FUNCTION_NAME);
        this.columnNames = ArgumentCheck.nonNull(columnNames);
    }

    public TransformToString(ColumnName columnName, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.columnNames = new ColumnName[]
        {ArgumentCheck.nonNull(columnName)};
        this.newColumnNames = (newColumnName != null) ? Map.of(columnName, newColumnName) : Map.of();
    }

    public static Transform of(String... params)
    {
        if (!Is.empty(params) && params.length >= 2)
        {
            ColumnName columnName = ColumnName.parse(params[0]);
            ColumnName newColumnName = ColumnName.parse(params[1]);

            return new TransformToString(columnName, newColumnName);
        }
        return null;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (!Is.empty(this.columnNames))
        {
            Column[] validColumns = getColumns(columnsByName, this.columnNames);
            Column[] newColumns = new Column[validColumns.length];

            for (int i = 0; i < validColumns.length; ++i)
            {
                Column c = validColumns[i];
                ColumnName newColumnName = newColumnNames.getOrDefault(c.getName(), c.getName());
                ColumnObject.Builder<String> cc = ColumnObject.builder(newColumnName,
                        app.babylon.table.column.ColumnTypes.STRING);
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
