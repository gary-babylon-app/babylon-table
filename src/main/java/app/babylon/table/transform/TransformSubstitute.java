package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.HashMap;
import java.util.Map;

import app.babylon.lang.Is;
import app.babylon.table.column.ColumnName;

public class TransformSubstitute extends TransformStringToString
{
    public static final String FUNCTION_NAME = "Substitute";

    private final String defaultValueNewColumn;
    private final Map<String, String> replaces;

    private TransformSubstitute(Builder builder)
    {
        super(FUNCTION_NAME, builder.columnName, builder.newColumnName);
        if (builder.replaces.isEmpty())
        {
            throw new RuntimeException(FUNCTION_NAME + " requires at least one replacement.");
        }
        this.replaces = Map.copyOf(builder.replaces);
        this.defaultValueNewColumn = builder.defaultValueNewColumn;
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
            return builder(columnName).withNewColumnName(newColumnName).withReplacements(replacements(remaining))
                    .build();
        }

        if (remaining.length >= 3)
        {
            String defaultValueNewColumn = remaining[0];
            String[] replaces = java.util.Arrays.copyOfRange(remaining, 1, remaining.length);
            return builder(columnName).withNewColumnName(newColumnName).withDefaultValue(defaultValueNewColumn)
                    .withReplacements(replacements(replaces)).build();
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
        return builder(columnName).withNewColumnName(newColumnName).withReplacements(replaces)
                .withDefaultValue(defaultValueNewColumn).build();
    }

    public static Builder builder(ColumnName columnName)
    {
        return new Builder(columnName);
    }

    public ColumnName columnName()
    {
        return this.existingColumnName();
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
    protected String transformString(String s)
    {
        String replaceValue = this.replaces.get(s);
        if (replaceValue != null)
        {
            return replaceValue;
        }
        return this.defaultValueNewColumn == null ? s : this.defaultValueNewColumn;
    }

    public static final class Builder
    {
        private final ColumnName columnName;
        private ColumnName newColumnName;
        private String defaultValueNewColumn;
        private final Map<String, String> replaces = new HashMap<>();

        private Builder(ColumnName columnName)
        {
            this.columnName = ArgumentCheck.nonNull(columnName);
        }

        public Builder withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public Builder withDefaultValue(String defaultValueNewColumn)
        {
            this.defaultValueNewColumn = defaultValueNewColumn;
            return this;
        }

        public Builder withReplacement(String oldValue, String newValue)
        {
            this.replaces.put(oldValue, newValue);
            return this;
        }

        public Builder withReplacements(Map<String, String> replaces)
        {
            this.replaces.clear();
            this.replaces.putAll(ArgumentCheck.nonNull(replaces));
            return this;
        }

        public TransformSubstitute build()
        {
            return new TransformSubstitute(this);
        }
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
