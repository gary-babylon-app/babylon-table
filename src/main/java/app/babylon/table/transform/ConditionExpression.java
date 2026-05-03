package app.babylon.table.transform;

import java.util.Map;
import java.util.Set;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.selection.RowPredicate;

public interface ConditionExpression
{
    RowPredicate prepare(Map<ColumnName, Column> columnsByName);

    Set<ColumnName> columnNames();

    String toDsl();
}
