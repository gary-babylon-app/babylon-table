package app.babylon.table.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;

abstract class TransformBase implements Transform
{
    private final String name;

    TransformBase(String name)
    {
        this.name = ArgumentCheck.nonEmpty(name);
    }

    public String getName()
    {
        return name;
    }

    protected Column[] getColumns(Map<ColumnName, Column> columnsByName, Collection<ColumnName> x)
    {
        if (x == null)
        {
            return null;
        }
        Collection<ColumnName> tableNames = columnsByName.keySet();
        List<ColumnName> retainedNames = new ArrayList<>(x);
        retainedNames.retainAll(tableNames);

        Column[] selectedColumns = new Column[retainedNames.size()];
        for (int i = 0; i < retainedNames.size(); ++i)
        {
            selectedColumns[i] = columnsByName.get(retainedNames.get(i));
        }
        return selectedColumns;
    }

    protected void putColumns(Map<ColumnName, Column> columnsByName, Column[] transformedColumns)
    {
        for (Column transformedColumn : transformedColumns)
        {
            columnsByName.put(transformedColumn.getName(), transformedColumn);
        }
    }

    protected int rowCount(Map<ColumnName, Column> columnsByName)
    {
        return columnsByName == null || columnsByName.isEmpty() ? 0 : columnsByName.values().iterator().next().size();
    }

    protected int rowCount(Map<ColumnName, Column> columnsByName, Collection<ColumnName> columnNames)
    {
        if (columnsByName == null || columnNames == null)
        {
            return 0;
        }
        for (ColumnName columnName : columnNames)
        {
            Column column = columnsByName.get(columnName);
            if (column != null)
            {
                return column.size();
            }
        }
        return 0;
    }

}
