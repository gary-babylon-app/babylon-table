package app.babylon.table.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.Columns;
import app.babylon.table.Transform;
import app.babylon.table.Transformer;

abstract class TransformBase implements Transform
{
    private final String name;

    TransformBase(String name)
    {
        this.name = ArgumentChecks.nonEmpty(name);
    }

    public String getName()
    {
        return name;
    }

    protected Column[] getColumns(Map<ColumnName, Column> columnsByName, ColumnName... x)
    {
        if (x == null)
        {
            return null;
        }
        Collection<ColumnName> tableNames = columnsByName.keySet();
        java.util.List<ColumnName> retainedNames = new ArrayList<>();
        Collections.addAll(retainedNames, x);
        retainedNames.retainAll(tableNames);

        Column[] selectedColumns = new Column[retainedNames.size()];
        for (int i = 0; i < retainedNames.size(); ++i)
        {
            selectedColumns[i] = columnsByName.get(retainedNames.get(i));
        }
        return selectedColumns;
    }

    protected <T> Column[] transformStringColumns(Map<ColumnName, Column> columnsByName,
            ColumnName[] selectedColumnNames, ColumnName[] newColumnNames, Class<T> valueClass,
            Function<CharSequence, T> transformFunction)
    {
        Column[] validColumns = getColumns(columnsByName, selectedColumnNames);
        return transformStringColumns(validColumns, newColumnNames, valueClass, transformFunction);
    }

    protected <T> Column[] transformStringColumns(Column[] validColumns, ColumnName[] newColumnNames,
            Class<T> valueClass, Function<CharSequence, T> transformFunction)
    {
        Set<String> uniqueStrings = gatherUniqueStrings(validColumns);
        Map<String, T> actualConversion = new HashMap<>();
        populateConversionMap(uniqueStrings, actualConversion, transformFunction);

        Column[] transformedColumns = new Column[validColumns.length];
        for (int i = 0; i < validColumns.length; ++i)
        {
            Column c = validColumns[i];
            if (Columns.isStringColumn(c))
            {
                ColumnObject<String> stringColumn = Columns.asStringColumn(c);
                ColumnName newColumnName = (newColumnNames == null) ? c.getName() : newColumnNames[i];
                Transformer<String, T> transformer = Transformer.of(actualConversion::get, valueClass, newColumnName);
                transformedColumns[i] = stringColumn.transform(transformer);
            } else if (valueClass.equals(c.getType().getValueClass()))
            {
                transformedColumns[i] = c;
            } else
            {
                throw new RuntimeException("Cannot convert from " + c.getName());
            }
        }
        return transformedColumns;
    }

    protected void putColumns(Map<ColumnName, Column> columnsByName, Column[] transformedColumns)
    {
        for (Column transformedColumn : transformedColumns)
        {
            columnsByName.put(transformedColumn.getName(), transformedColumn);
        }
    }

    private static Set<String> gatherUniqueStrings(Column[] columns)
    {
        Set<String> uniques = new HashSet<String>();
        for (Column c : columns)
        {
            if (Columns.isStringColumn(c))
            {
                ColumnObject<String> stringColumn = Columns.asStringColumn(c);
                stringColumn.getUniques(uniques);
            }
        }
        return uniques;
    }

    private static <T> void populateConversionMap(Set<String> uniqueStrings, Map<String, T> actualConversion,
            Function<CharSequence, T> transformFunction)
    {
        for (String s : uniqueStrings)
        {
            if (!actualConversion.containsKey(s))
            {
                actualConversion.put(s, transformFunction.apply(s));
            }
        }
    }
}
