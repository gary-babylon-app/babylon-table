package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.HashMap;
import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public class TransformStringReplaceAll extends TransformBase
{
    private static final String FUNCTION_NAME = "StringReplaceAll";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;
    private final String target;
    private final String replacement;

    public TransformStringReplaceAll(ColumnName existingColumnName, String target, String replacement)
    {
        this(existingColumnName, existingColumnName, target, replacement);
    }

    public TransformStringReplaceAll(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = ArgumentCheck.nonNull(existingColumnName);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.target = ArgumentCheck.nonEmpty(target);
        this.replacement = ArgumentCheck.nonNull(replacement);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject<String> column = Columns.asStringColumn(columnsByName.get(this.existingColumnName));
        if (column == null)
        {
            return;
        }
        ColumnObject.Builder<String> newColumn = ColumnObject.builder(this.newColumnName, String.class);
        Map<String, String> old2New = new HashMap<>();

        for (int i = 0; i < column.size(); ++i)
        {
            String s = column.get(i);
            if (!old2New.containsKey(s))
            {
                String r = s.replaceAll(target, replacement);

                old2New.put(s, r);
                s = r;
            } else
            {
                s = old2New.get(s);
            }
            if (!Strings.isEmpty(s))
            {
                newColumn.add(s);
            } else
            {
                newColumn.addNull();
            }
        }

        columnsByName.put(this.newColumnName, newColumn.build());
    }
}
