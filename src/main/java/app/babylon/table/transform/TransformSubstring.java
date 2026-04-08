package app.babylon.table.transform;

import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public class TransformSubstring extends TransformBase
{
    public static final String FUNCTION_NAME = "Substring";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;
    private final int first;
    private final int last;

    public TransformSubstring(ColumnName existingColumnName, ColumnName newColumnName, int first, int last)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = app.babylon.lang.ArgumentCheck.nonNull(existingColumnName);
        this.newColumnName = app.babylon.lang.ArgumentCheck.nonNull(newColumnName);
        this.first = Math.max(0, first);
        this.last = Math.max(this.first + 1, last);
    }

    public static TransformSubstring of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 4)
        {
            return new TransformSubstring(ColumnName.of(params[0]), ColumnName.of(params[1]),
                    Integer.parseInt(params[2]), Integer.parseInt(params[3]));
        }
        return null;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject<String> column = Columns.asStringColumn(columnsByName.get(this.existingColumnName));
        if (column == null)
        {
            return;
        }
        ColumnObject.Builder<String> newColumnBuilder = ColumnObject.builder(this.newColumnName, String.class);

        for (int i = 0; i < column.size(); ++i)
        {
            String s = column.get(i);
            if (!Strings.isEmpty(s) && s.length() >= last)
            {
                s = s.substring(this.first, this.last);
                newColumnBuilder.add(s);
            } else
            {
                newColumnBuilder.addNull();
            }
        }
        ColumnObject<String> newColumn = newColumnBuilder.build();
        columnsByName.put(newColumn.getName(), newColumn);
    }

}
