package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.text.Strings;

public class TransformSplit extends TransformBase
{
    public static final String FUNCTION_NAME = "Split";

    private final ColumnName columnToSplit;
    private final String splitOn;
    private final ColumnName[] splitColumnNames;

    public TransformSplit(ColumnName columnToSplit, String splitOn, ColumnName... splitColumnNames)
    {
        super(FUNCTION_NAME);
        this.columnToSplit = ArgumentCheck.nonNull(columnToSplit);
        this.splitOn = oneCharacter(splitOn);
        this.splitColumnNames = Arrays.copyOf(ArgumentCheck.nonNull(splitColumnNames), splitColumnNames.length);
    }

    public TransformSplit(ColumnName columnToSplit, String splitOn, Collection<String> splitColumnNames)
    {
        super(FUNCTION_NAME);
        this.columnToSplit = ArgumentCheck.nonNull(columnToSplit);
        this.splitOn = oneCharacter(splitOn);
        this.splitColumnNames = columnNames(splitColumnNames);
    }

    public ColumnName getColumnToSplit()
    {
        return this.columnToSplit;
    }

    public String getSplitOn()
    {
        return this.splitOn;
    }

    public ColumnName[] getSplitColumnNames()
    {
        return Arrays.copyOf(this.splitColumnNames, this.splitColumnNames.length);
    }

    public static TransformSplit of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 4)
        {
            return of(ColumnName.of(params[0]), params[1], Arrays.asList(params).subList(2, params.length));
        }
        return null;
    }

    public static TransformSplit of(ColumnName columnToSplit, String splitOn, ColumnName... splitColumnNames)
    {
        return new TransformSplit(columnToSplit, splitOn, splitColumnNames);
    }

    public static TransformSplit of(ColumnName columnToSplit, String splitOn, Collection<String> splitColumnNames)
    {
        return new TransformSplit(columnToSplit, splitOn, splitColumnNames);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        @SuppressWarnings("unchecked")
        ColumnObject<String> toSplit = (ColumnObject<String>) columnsByName.get(columnToSplit);
        if (toSplit == null)
        {
            return;
        }
        @SuppressWarnings("unchecked")
        ColumnObject.Builder<String>[] newColumns = new ColumnObject.Builder[splitColumnNames.length];
        for (int i = 0; i < splitColumnNames.length; ++i)
        {
            newColumns[i] = ColumnObject.builder(splitColumnNames[i], ColumnTypes.STRING);
        }
        Strings.Splitter splitter = Strings.splitter().withSplitter(this.splitOn.charAt(0)).withRemoveEmpty(false);
        for (int i = 0; i < toSplit.size(); ++i)
        {
            String s = toSplit.get(i);
            if (!Strings.isEmpty(s))
            {
                String[] splitValues = splitter.split(s);
                int size = Math.min(splitValues.length, newColumns.length);
                for (int j = 0; j < size; ++j)
                {
                    newColumns[j].add(splitValues[j]);
                }
                for (int j = size; j < newColumns.length; ++j)
                {
                    newColumns[j].addNull();
                }
            }
            else
            {
                for (int j = 0; j < newColumns.length; ++j)
                {
                    newColumns[j].addNull();
                }
            }
        }
        @SuppressWarnings("unchecked")
        ColumnObject<String>[] builtColumns = new ColumnObject[newColumns.length];
        for (int i = 0; i < newColumns.length; ++i)
        {
            builtColumns[i] = newColumns[i].build();
        }
        putColumns(columnsByName, builtColumns);
    }

    private static String oneCharacter(String splitOn)
    {
        splitOn = ArgumentCheck.nonEmpty(splitOn);
        if (splitOn.length() != 1)
        {
            throw new RuntimeException(FUNCTION_NAME + " split delimiter must be exactly one character.");
        }
        return splitOn;
    }

    private static ColumnName[] columnNames(Collection<String> splitColumnNames)
    {
        Collection<String> names = ArgumentCheck.nonEmpty(splitColumnNames);
        ColumnName[] copy = new ColumnName[names.size()];
        int i = 0;
        for (String name : names)
        {
            copy[i++] = ColumnName.of(name);
        }
        return copy;
    }
}
