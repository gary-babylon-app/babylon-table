package app.babylon.table.transform;

import java.util.function.Function;
import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.ToStringSettings;

public class TransformClean extends TransformBase
{
    public static final String FUNCTION_NAME = "Clean";
    private final ColumnName columnToClean;
    private final Function<String, String> cleanFunction;

    public TransformClean(ColumnName column, Function<String, String> cleanFunction)
    {
        super(FUNCTION_NAME);
        this.columnToClean = column;
        this.cleanFunction = cleanFunction;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnToClean);
        if (column == null)
        {
            return;
        }
        ColumnObject.Builder<String> cleanedColumn = ColumnObject.builder(this.columnToClean,
                app.babylon.table.column.ColumnTypes.STRING);
        ToStringSettings settings = ToStringSettings.standard();

        for (int i = 0; i < column.size(); i++)
        {
            cleanedColumn.add(cleanFunction.apply(column.toString(i, settings)));
        }
        columnsByName.put(this.columnToClean, cleanedColumn.build());
    }
}
