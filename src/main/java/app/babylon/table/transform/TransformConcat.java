package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
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
    private final ColumnName[] sourceColumns;
    private final String[] literalValues;

    public TransformConcat(ColumnName concatColumn, String separator, ColumnName... sourceColumns)
    {
        super(FUNCTION_NAME);
        this.concatColumn = ArgumentCheck.nonNull(concatColumn);
        this.separator = separator;
        this.sourceColumns = Arrays.copyOf(ArgumentCheck.nonNull(sourceColumns), sourceColumns.length);
        this.literalValues = new String[sourceColumns.length];
    }

    public TransformConcat(ColumnName concatColumn, String separator, Collection<String> sourceColumns)
    {
        super(FUNCTION_NAME);
        this.concatColumn = ArgumentCheck.nonNull(concatColumn);
        this.separator = separator;
        this.sourceColumns = columnNames(sourceColumns);
        this.literalValues = new String[this.sourceColumns.length];
    }

    private TransformConcat(ColumnName concatColumn, String separator, ColumnName[] sourceColumns,
            String[] literalValues)
    {
        super(FUNCTION_NAME);
        this.concatColumn = ArgumentCheck.nonNull(concatColumn);
        this.separator = separator;
        this.sourceColumns = Arrays.copyOf(ArgumentCheck.nonNull(sourceColumns), sourceColumns.length);
        this.literalValues = Arrays.copyOf(ArgumentCheck.nonNull(literalValues), literalValues.length);
        if (this.sourceColumns.length != this.literalValues.length)
        {
            throw new IllegalArgumentException("Require the same number of source columns and literal values.");
        }
        for (int i = 0; i < this.sourceColumns.length; ++i)
        {
            if ((this.sourceColumns[i] == null) == (this.literalValues[i] == null))
            {
                throw new IllegalArgumentException("Require exactly one source column or literal value at " + i + ".");
            }
        }
    }

    public static TransformConcat of(String... params)
    {
        if (Is.empty(params) || params.length < 3)
        {
            return null;
        }

        return of(ColumnName.parse(params[0]), params[1], Arrays.asList(params).subList(2, params.length));
    }

    public static TransformConcat of(ColumnName concatColumn, String separator, ColumnName... sourceColumns)
    {
        return new TransformConcat(concatColumn, separator, sourceColumns);
    }

    public static TransformConcat of(ColumnName concatColumn, String separator, Collection<String> sourceColumns)
    {
        return new TransformConcat(concatColumn, separator, sourceColumns);
    }

    public static TransformConcat of(ColumnName concatColumn, String separator, ColumnName[] sourceColumns,
            String[] literalValues)
    {
        return new TransformConcat(concatColumn, separator, sourceColumns, literalValues);
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

    public ColumnName[] sourceColumns()
    {
        return Arrays.copyOf(this.sourceColumns, this.sourceColumns.length);
    }

    public String[] literalValues()
    {
        return Arrays.copyOf(this.literalValues, this.literalValues.length);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject.Builder<String> newColumn = ColumnObject.builder(this.concatColumn, ColumnTypes.STRING);
        Column[] columns = new Column[this.sourceColumns.length];
        String[] values = new String[this.sourceColumns.length];
        ToStringSettings settings = ToStringSettings.standard();

        for (int i = 0; i < this.sourceColumns.length; ++i)
        {
            if (this.literalValues[i] != null)
            {
                continue;
            }
            Column column = columnsByName.get(this.sourceColumns[i]);
            if (column == null)
            {
                throw new IllegalArgumentException("No column " + this.sourceColumns[i] + " found");
            }
            columns[i] = column;
        }

        int rowCount = rowCount(columnsByName);
        for (int i = 0; i < rowCount; ++i)
        {
            for (int j = 0; j < columns.length; ++j)
            {
                values[j] = this.literalValues[j] == null ? columns[j].toString(i, settings) : this.literalValues[j];
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

    private static ColumnName[] columnNames(Collection<String> sourceColumns)
    {
        Collection<String> names = ArgumentCheck.nonEmpty(sourceColumns);
        ColumnName[] copy = new ColumnName[names.size()];
        int i = 0;
        for (String name : names)
        {
            copy[i++] = ColumnName.of(name);
        }
        return copy;
    }
}
