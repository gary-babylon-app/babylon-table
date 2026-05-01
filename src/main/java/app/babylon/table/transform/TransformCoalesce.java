package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.text.Strings;

public class TransformCoalesce extends TransformBase
{
    public static final String FUNCTION_NAME = "Coalesce";

    private final ColumnName newColumnName;
    private final ColumnObject.Mode mode;
    private final ColumnName[] columnNames;

    public TransformCoalesce(ColumnName newColumnName, ColumnObject.Mode mode, ColumnName firstColumnName,
            ColumnName... additionalColumnNames)
    {
        super(FUNCTION_NAME);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.mode = mode;
        this.columnNames = columnNames(firstColumnName, additionalColumnNames);
    }

    public TransformCoalesce(ColumnName newColumnName, ColumnObject.Mode mode, Collection<String> sourceColumnNames)
    {
        super(FUNCTION_NAME);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.mode = mode;
        this.columnNames = columnNames(sourceColumnNames);
    }

    public static TransformCoalesce of(String... params)
    {
        if (Is.empty(params) || params.length < 2)
        {
            return null;
        }
        ColumnObject.Mode mode = parseMode(params[1]);
        int sourceStartIndex = mode == null ? 1 : 2;
        if (sourceColumnNamesLength(params, sourceStartIndex) == 0)
        {
            return null;
        }
        return of(ColumnName.of(params[0]), mode, sourceColumnNames(params, sourceStartIndex));
    }

    public static TransformCoalesce of(ColumnName newColumnName, ColumnObject.Mode mode, ColumnName firstColumnName,
            ColumnName... additionalColumnNames)
    {
        return new TransformCoalesce(newColumnName, mode, firstColumnName, additionalColumnNames);
    }

    public static TransformCoalesce of(ColumnName newColumnName, ColumnObject.Mode mode,
            Collection<String> sourceColumnNames)
    {
        return new TransformCoalesce(newColumnName, mode, sourceColumnNames);
    }

    public ColumnObject.Mode mode()
    {
        return this.mode;
    }

    public ColumnObject.Mode effectiveMode()
    {
        return this.mode == null ? ColumnObject.Mode.AUTO : this.mode;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public ColumnName[] columnNames()
    {
        return Arrays.copyOf(this.columnNames, this.columnNames.length);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject<?>[] columns = columns(columnsByName);
        if (columns == null)
        {
            return;
        }

        validate(columns);
        coalesce(columnsByName, columns, columns[0].getType());
    }

    @SuppressWarnings(
    {"unchecked", "rawtypes"})
    private void coalesce(Map<ColumnName, Column> columnsByName, ColumnObject<?>[] columns, Column.Type type)
    {
        ColumnObject.Builder builder = ColumnObject.builder(this.newColumnName, type, effectiveMode());
        for (int i = 0; i < columns[0].size(); ++i)
        {
            Object value = null;
            boolean found = false;
            for (ColumnObject<?> column : columns)
            {
                if (column.isSet(i))
                {
                    value = column.get(i);
                    found = true;
                    break;
                }
            }
            if (found)
            {
                builder.add(value);
            }
            else
            {
                builder.addNull();
            }
        }
        columnsByName.put(this.newColumnName, builder.build());
    }

    private ColumnObject<?>[] columns(Map<ColumnName, Column> columnsByName)
    {
        ColumnObject<?>[] columns = new ColumnObject[this.columnNames.length];
        for (int i = 0; i < this.columnNames.length; ++i)
        {
            Column column = columnsByName.get(this.columnNames[i]);
            if (!(column instanceof ColumnObject<?> objectColumn))
            {
                return null;
            }
            columns[i] = objectColumn;
        }
        return columns;
    }

    private static void validate(ColumnObject<?>[] columns)
    {
        ColumnObject<?> first = columns[0];
        Class<?> valueClass = first.getType().getValueClass();
        int size = first.size();
        for (int i = 1; i < columns.length; ++i)
        {
            ColumnObject<?> column = columns[i];
            if (!valueClass.equals(column.getType().getValueClass()))
            {
                throw new IllegalArgumentException("Coalesce requires matching value classes.");
            }
            if (size != column.size())
            {
                throw new IllegalArgumentException("Coalesce requires columns to have matching sizes.");
            }
        }
    }

    private static ColumnName[] columnNames(ColumnName firstColumnName, ColumnName... additionalColumnNames)
    {
        ColumnName[] additional = ArgumentCheck.nonNull(additionalColumnNames);
        ColumnName[] columnNames = new ColumnName[additional.length + 1];
        columnNames[0] = ArgumentCheck.nonNull(firstColumnName);
        for (int i = 0; i < additional.length; ++i)
        {
            columnNames[i + 1] = ArgumentCheck.nonNull(additional[i]);
        }
        return columnNames;
    }

    private static ColumnName[] columnNames(Collection<String> sourceColumnNames)
    {
        Collection<String> names = ArgumentCheck.nonEmpty(sourceColumnNames);
        ColumnName[] copy = new ColumnName[names.size()];
        int i = 0;
        for (String name : names)
        {
            copy[i++] = ColumnName.of(name);
        }
        return copy;
    }

    private static ColumnObject.Mode parseMode(String s)
    {
        if (Strings.isEmpty(s))
        {
            return null;
        }
        try
        {
            return ColumnObject.Mode.parse(s);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private static int sourceColumnNamesLength(String[] params, int sourceStartIndex)
    {
        int length = 0;
        for (int i = sourceStartIndex; i < params.length; ++i)
        {
            if (Strings.isEmpty(params[i]))
            {
                break;
            }
            ++length;
        }
        return length;
    }

    private static Collection<String> sourceColumnNames(String[] params, int sourceStartIndex)
    {
        int length = sourceColumnNamesLength(params, sourceStartIndex);
        return Arrays.asList(params).subList(sourceStartIndex, sourceStartIndex + length);
    }
}
