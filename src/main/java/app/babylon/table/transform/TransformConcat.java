package app.babylon.table.transform;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ToStringSettings;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class TransformConcat extends TransformBase
{
    public static final String FUNCTION_NAME = "Concat";

    private final ColumnName concatColumn;
    private final String separator;
    private final Part[] parts;

    public record Part(ColumnName columnName, String literalValue)
    {
        public Part
        {
            if ((columnName == null) == (literalValue == null))
            {
                throw new IllegalArgumentException("Require exactly one source column or literal value.");
            }
        }

        public static Part column(ColumnName columnName)
        {
            return new Part(ArgumentCheck.nonNull(columnName), null);
        }

        public static Part literal(String value)
        {
            return new Part(null, ArgumentCheck.nonNull(value));
        }

        public boolean isColumn()
        {
            return this.columnName != null;
        }
    }

    private TransformConcat(ColumnName concatColumn, String separator, Part... parts)
    {
        super(FUNCTION_NAME);
        this.concatColumn = ArgumentCheck.nonNull(concatColumn);
        this.separator = separator;
        this.parts = parts(parts);
    }

    private TransformConcat(ColumnName concatColumn, String separator, Iterable<Part> parts)
    {
        super(FUNCTION_NAME);
        this.concatColumn = ArgumentCheck.nonNull(concatColumn);
        this.separator = separator;
        this.parts = parts(parts);
    }

    /**
     * Creates a concat transform from the legacy registry parameter shape: output
     * column, separator, then source column names.
     *
     * @deprecated Use transform DSL through {@link QuickTransforms}, or typed
     *             factories with {@link ColumnName} and {@link Part}.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
    public static TransformConcat of(String... params)
    {
        if (params == null || params.length < 3)
        {
            return null;
        }
        ColumnName[] sourceColumns = new ColumnName[params.length - 2];
        for (int i = 2; i < params.length; ++i)
        {
            sourceColumns[i - 2] = ColumnName.of(params[i]);
        }
        return of(ColumnName.parse(params[0]), params[1], sourceColumns);
    }

    public static TransformConcat of(ColumnName concatColumn, String separator, ColumnName... sourceColumns)
    {
        return new TransformConcat(concatColumn, separator, parts(sourceColumns));
    }

    public static TransformConcat of(ColumnName concatColumn, String separator, Part... parts)
    {
        return new TransformConcat(concatColumn, separator, parts);
    }

    public static TransformConcat of(ColumnName concatColumn, String separator, Iterable<Part> parts)
    {
        return new TransformConcat(concatColumn, separator, parts);
    }

    public String separator()
    {
        return this.separator;
    }

    public String effectiveSeparator()
    {
        return this.separator == null ? "" : this.separator;
    }

    public ColumnName concatColumn()
    {
        return this.concatColumn;
    }

    public Part[] parts()
    {
        return Arrays.copyOf(this.parts, this.parts.length);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject.Builder<String> newColumn = ColumnObject.builder(this.concatColumn, ColumnTypes.STRING);
        Column[] columns = new Column[this.parts.length];
        String[] values = new String[this.parts.length];
        ToStringSettings settings = ToStringSettings.standard();

        for (int i = 0; i < this.parts.length; ++i)
        {
            Part part = this.parts[i];
            if (!part.isColumn())
            {
                continue;
            }
            Column column = columnsByName.get(part.columnName());
            if (column == null)
            {
                throw new IllegalArgumentException("No column " + part.columnName() + " found");
            }
            columns[i] = column;
        }

        int rowCount = rowCount(columnsByName);
        for (int i = 0; i < rowCount; ++i)
        {
            for (int j = 0; j < columns.length; ++j)
            {
                Part part = this.parts[j];
                values[j] = part.isColumn() ? columns[j].toString(i, settings) : part.literalValue();
            }
            if (columns.length > 1)
            {
                newColumn.add(String.join(effectiveSeparator(), values));
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

    private static Part[] parts(ColumnName... sourceColumns)
    {
        ColumnName[] columns = ArgumentCheck.nonNull(sourceColumns);
        Part[] parts = new Part[columns.length];
        for (int i = 0; i < parts.length; ++i)
        {
            parts[i] = Part.column(columns[i]);
        }
        return parts;
    }

    private static Part[] parts(Part... parts)
    {
        Part[] copy = Arrays.copyOf(ArgumentCheck.nonNull(parts), parts.length);
        for (int i = 0; i < copy.length; ++i)
        {
            copy[i] = ArgumentCheck.nonNull(copy[i]);
        }
        return copy;
    }

    private static Part[] parts(Iterable<Part> parts)
    {
        Iterable<Part> checked = ArgumentCheck.nonNull(parts);
        List<Part> copy = new ArrayList<>();
        for (Part part : checked)
        {
            copy.add(ArgumentCheck.nonNull(part));
        }
        return copy.toArray(Part[]::new);
    }
}
