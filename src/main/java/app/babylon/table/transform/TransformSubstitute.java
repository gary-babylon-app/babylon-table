package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.HashMap;
import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.text.Strings;

public class TransformSubstitute extends TransformBase
{
    public static final String FUNCTION_NAME = "Substitute";

    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final String defaultValueNewColumn;
    private final Map<String, String> replaces;

    public TransformSubstitute(String defaultValueNewColumn, ColumnName columnName, String... x)
    {
        this(defaultValueNewColumn, columnName, columnName, x);
    }

    public TransformSubstitute(String defaultValueNewColumn, ColumnName columnName, ColumnName newColumnName,
            String... x)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.defaultValueNewColumn = defaultValueNewColumn;

        Map<String, String> replacements = new HashMap<>();

        if (x.length % 2 != 0)
        {
            throw new RuntimeException(FUNCTION_NAME + " expects replaces to be in pairs.");
        }

        for (int i = 0; i < x.length; i = i + 2)
        {
            replacements.put(x[i].strip(), x[i + 1].strip());
        }
        this.replaces = Map.copyOf(replacements);
    }

    public TransformSubstitute(ColumnName columnName, ColumnName newColumnName, Map<String, String> replaces)
    {
        this(null, columnName, newColumnName, replaces);
    }

    public TransformSubstitute(String defaultValueNewColumn, ColumnName columnName, ColumnName newColumnName,
            Map<String, String> replaces)
    {
        super(FUNCTION_NAME);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.replaces = Map.copyOf(ArgumentCheck.nonNull(replaces));
        this.defaultValueNewColumn = defaultValueNewColumn;
    }

    public static TransformSubstitute of(String[] params)
    {
        if (Is.empty(params) || params.length < 4)
        {
            return null;
        }

        ColumnName columnName = ColumnName.parse(params[0]);
        ColumnName newColumnName = ColumnName.parse(params[1]);
        String[] remaining = java.util.Arrays.copyOfRange(params, 2, params.length);

        if (remaining.length % 2 == 0)
        {
            return of(columnName, newColumnName, replacements(remaining));
        }

        if (remaining.length >= 3)
        {
            String defaultValueNewColumn = remaining[0];
            String[] replaces = java.util.Arrays.copyOfRange(remaining, 1, remaining.length);
            return of(columnName, newColumnName, replacements(replaces), defaultValueNewColumn);
        }

        return null;
    }

    public static TransformSubstitute of(ColumnName columnName, ColumnName newColumnName, Map<String, String> replaces)
    {
        return of(columnName, newColumnName, replaces, null);
    }

    public static TransformSubstitute of(ColumnName columnName, ColumnName newColumnName, Map<String, String> replaces,
            String defaultValueNewColumn)
    {
        return new TransformSubstitute(defaultValueNewColumn, columnName, newColumnName, replaces);
    }

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public String defaultValueNewColumn()
    {
        return this.defaultValueNewColumn;
    }

    public Map<String, String> replaces()
    {
        return this.replaces;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column column = columnsByName.get(this.columnName);
        if (!Columns.isStringColumn(column))
        {
            return;
        }
        ColumnObject<String> stringColumn = Columns.asStringColumn(column);
        ColumnObject.Builder<String> newColumn = ColumnObject.builder(this.newColumnName, ColumnTypes.STRING);
        for (int i = 0; i < stringColumn.size(); ++i)
        {
            String s = stringColumn.get(i);

            if (!Strings.isEmpty(s))
            {
                String replaceValue = this.replaces.get(s);
                if (replaceValue == null && this.defaultValueNewColumn != null)
                {
                    replaceValue = this.defaultValueNewColumn;
                }
                if (replaceValue == null && this.defaultValueNewColumn == null)
                {
                    replaceValue = s;
                }
                newColumn.add(replaceValue);
            }
            else
            {
                newColumn.add(this.defaultValueNewColumn);
            }
        }
        columnsByName.put(this.newColumnName, newColumn.build());
    }

    private static Map<String, String> replacements(String... replaceOldNew)
    {
        if (replaceOldNew.length % 2 != 0)
        {
            throw new RuntimeException(FUNCTION_NAME + " expects replaces to be in pairs.");
        }

        Map<String, String> replacements = new HashMap<>();
        for (int i = 0; i < replaceOldNew.length; i = i + 2)
        {
            replacements.put(replaceOldNew[i].strip(), replaceOldNew[i + 1].strip());
        }
        return replacements;
    }
}
