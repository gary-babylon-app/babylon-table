package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.util.HashMap;
import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.Is;

public class TransformStringReplace extends TransformBase
{
    private static final String FUNCTION_NAME = "StringReplace";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;
    private final String target;
    private final String replacement;

    public TransformStringReplace(ColumnName existingColumnName, String target, String replacement)
    {
        this(existingColumnName, existingColumnName, target, replacement);
    }

    public TransformStringReplace(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = ArgumentChecks.nonNull(existingColumnName);
        this.newColumnName = ArgumentChecks.nonNull(newColumnName);
        this.target = ArgumentChecks.nonEmpty(target);
        this.replacement = ArgumentChecks.nonNull(replacement);
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
            if (!Strings.isEmpty(s))
            {
                if (!old2New.containsKey(s))
                {
                    String r = s.replace(target, replacement);
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
            } else
            {
                newColumn.addNull();
            }
        }

        columnsByName.put(this.newColumnName, newColumn.build());
    }

}
