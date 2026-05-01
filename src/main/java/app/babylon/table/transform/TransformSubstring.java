package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
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
        this.existingColumnName = ArgumentCheck.nonNull(existingColumnName);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.first = Math.max(0, first);
        this.last = Math.max(this.first + 1, last);
    }

    public static TransformSubstring of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 4)
        {
            return of(ColumnName.of(params[0]), ColumnName.of(params[1]), Integer.parseInt(params[2]),
                    Integer.parseInt(params[3]));
        }
        return null;
    }

    public static TransformSubstring of(ColumnName existingColumnName, ColumnName newColumnName, int first, int last)
    {
        return new TransformSubstring(existingColumnName, newColumnName, first, last);
    }

    public ColumnName existingColumnName()
    {
        return this.existingColumnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public int first()
    {
        return this.first;
    }

    public int last()
    {
        return this.last;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject<String> column = Columns.asStringColumn(columnsByName.get(this.existingColumnName));
        if (column == null)
        {
            return;
        }
        ColumnObject.Builder<String> newColumnBuilder = ColumnObject.builder(this.newColumnName, ColumnTypes.STRING);

        for (int i = 0; i < column.size(); ++i)
        {
            String s = column.get(i);
            if (!Strings.isEmpty(s) && s.length() >= last)
            {
                s = s.substring(this.first, this.last);
                newColumnBuilder.add(s);
            }
            else
            {
                newColumnBuilder.addNull();
            }
        }
        ColumnObject<String> newColumn = newColumnBuilder.build();
        columnsByName.put(newColumn.getName(), newColumn);
    }

}
