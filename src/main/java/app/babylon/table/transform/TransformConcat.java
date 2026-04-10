package app.babylon.table.transform;

import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.lang.Is;
import app.babylon.table.ToStringSettings;

public class TransformConcat extends TransformBase
{
    public static final String FUNCTION_NAME = "Concat";

    private final ColumnName concatColumn;
    private final String separator;
    private final ColumnName[] sourceColumns;

    public TransformConcat(ColumnName concatColumn, String separator, ColumnName... sourceColumns)
    {
        super(FUNCTION_NAME);
        this.concatColumn = concatColumn;
        this.separator = separator == null ? "" : separator;
        this.sourceColumns = sourceColumns;
    }

    public static TransformConcat of(String... params)
    {
        if (Is.empty(params) || params.length < 3)
        {
            return null;
        }

        ColumnName concatColumn = ColumnName.parse(params[0]);
        String separator = params[1];
        ColumnName[] sourceColumns = new ColumnName[params.length - 2];
        for (int i = 2; i < params.length; ++i)
        {
            sourceColumns[i - 2] = ColumnName.parse(params[i]);
        }
        return new TransformConcat(concatColumn, separator, sourceColumns);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject.Builder<String> newColumn = ColumnObject.builder(this.concatColumn, String.class);
        Column[] columns = new Column[this.sourceColumns.length];
        String[] values = new String[this.sourceColumns.length];
        ToStringSettings settings = ToStringSettings.standard();

        for (int i = 0; i < this.sourceColumns.length; ++i)
        {
            Column column = columnsByName.get(this.sourceColumns[i]);
            if (column == null)
            {
                throw new IllegalArgumentException("No column " + this.sourceColumns[i] + " found");
            }
            columns[i] = column;
        }

        int rowCount = columns.length == 0 ? 0 : columns[0].size();
        for (int i = 0; i < rowCount; ++i)
        {
            for (int j = 0; j < columns.length; ++j)
            {
                values[j] = columns[j].toString(i, settings);
            }
            if (columns.length > 1)
            {
                newColumn.add(String.join(this.separator, values));
            }
            else if (columns.length == 1)
            {
                newColumn.add(values[0]);
            }
            else
            {
                newColumn.addNull();
            }
        }
        columnsByName.put(this.concatColumn, newColumn.build());
    }
}
