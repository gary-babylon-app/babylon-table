package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;

import java.util.HashMap;
import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public class TransformStringReplaceAll extends TransformBase
{
    public static final String FUNCTION_NAME = "StringReplaceAll";

    private final ColumnName existingColumnName;
    private final ColumnName newColumnName;
    private final String target;
    private final String replacement;

    private TransformStringReplaceAll(ColumnName existingColumnName, String target, String replacement)
    {
        this(existingColumnName, existingColumnName, target, replacement);
    }

    private TransformStringReplaceAll(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        super(FUNCTION_NAME);
        this.existingColumnName = ArgumentCheck.nonNull(existingColumnName);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.target = ArgumentCheck.nonEmpty(target);
        this.replacement = ArgumentCheck.nonNull(replacement);
    }

    public static TransformStringReplaceAll of(ColumnName existingColumnName, String target, String replacement)
    {
        if (existingColumnName == null || Strings.isEmpty(target) || replacement == null)
        {
            return null;
        }
        return new TransformStringReplaceAll(existingColumnName, target, replacement);
    }

    public static TransformStringReplaceAll of(ColumnName existingColumnName, ColumnName newColumnName, String target,
            String replacement)
    {
        if (existingColumnName == null || newColumnName == null || Strings.isEmpty(target) || replacement == null)
        {
            return null;
        }
        return new TransformStringReplaceAll(existingColumnName, newColumnName, target, replacement);
    }

    public static TransformStringReplaceAll of(String... params)
    {
        if (Is.empty(params) || params.length < 4)
        {
            return null;
        }
        ColumnName existingColumnName = ColumnName.parse(params[0]);
        ColumnName newColumnName = ColumnName.parse(params[1]);
        return of(existingColumnName, newColumnName, params[2], params[3]);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject<String> column = Columns.asStringColumn(columnsByName.get(this.existingColumnName));
        if (column == null)
        {
            return;
        }
        ColumnObject.Builder<String> newColumn = ColumnObject.builder(this.newColumnName,
                app.babylon.table.column.ColumnTypes.STRING);
        Map<String, String> old2New = new HashMap<>();

        for (int i = 0; i < column.size(); ++i)
        {
            String s = column.get(i);
            if (s == null)
            {
                newColumn.addNull();
                continue;
            }
            if (!old2New.containsKey(s))
            {
                String r = s.replaceAll(target, replacement);

                old2New.put(s, r);
                s = r;
            }
            else
            {
                s = old2New.get(s);
            }
            if (!Strings.isEmpty(s))
            {
                newColumn.add(s);
            }
            else
            {
                newColumn.addNull();
            }
        }

        columnsByName.put(this.newColumnName, newColumn.build());
    }
}
