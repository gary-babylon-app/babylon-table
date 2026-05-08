package app.babylon.table.transform;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

public interface TransformToColumn extends Transform
{
    ColumnName outputColumnName();

    Collection<ColumnName> sourceColumnNames();

    Column transform(Map<ColumnName, Column> columnsByName, int rowCount);

    default Column transform(TableColumnar table)
    {
        ArgumentCheck.nonNull(table);
        Map<ColumnName, Column> columnsByName = new LinkedHashMap<>();
        for (ColumnName columnName : sourceColumnNames())
        {
            Column column = table.get(columnName);
            if (column != null)
            {
                columnsByName.put(columnName, column);
            }
        }
        return transform(columnsByName, table.getRowCount());
    }

    @Override
    default void apply(Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null)
        {
            return;
        }
        Column transformed = transform(columnsByName, rowCount(columnsByName, sourceColumnNames()));
        if (transformed != null)
        {
            columnsByName.put(outputColumnName(), transformed);
        }
    }

    private static int rowCount(Map<ColumnName, Column> columnsByName, Collection<ColumnName> sourceColumnNames)
    {
        if (columnsByName == null || columnsByName.isEmpty())
        {
            return 0;
        }
        if (sourceColumnNames != null)
        {
            for (ColumnName sourceColumnName : sourceColumnNames)
            {
                Column column = columnsByName.get(sourceColumnName);
                if (column != null)
                {
                    return column.size();
                }
            }
        }
        return columnsByName.values().iterator().next().size();
    }
}
