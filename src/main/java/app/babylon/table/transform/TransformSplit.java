package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.Is;
import app.babylon.text.Split;

public class TransformSplit extends TransformBase
{
    public static final String FUNCTION_NAME = "Split";

    public ColumnName columnToSplit;
    public String splitOn;
    public ColumnName[] splitColumnNames;

    public TransformSplit(ColumnName columnToSplit, String splitOn, ColumnName... splitColumnNames)
    {
        super(FUNCTION_NAME);
        this.columnToSplit = ArgumentChecks.nonNull(columnToSplit);
        this.splitOn = ArgumentChecks.nonEmpty(splitOn);
        this.splitColumnNames = ArgumentChecks.nonNull(splitColumnNames);
    }

    public static TransformSplit of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 4)
        {
            ColumnName columnToSplit = ColumnName.of(params[0]);
            String splitOn = params[1];
            ColumnName[] splitColumnNames = new ColumnName[params.length - 2];
            for (int i = 2; i < params.length; ++i)
            {
                splitColumnNames[i - 2] = ColumnName.of(params[i]);
            }
            return new TransformSplit(columnToSplit, splitOn, splitColumnNames);
        }
        return null;
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
            newColumns[i] = ColumnObject.builder(splitColumnNames[i], String.class);
        }
        for (int i = 0; i < toSplit.size(); ++i)
        {
            String s = toSplit.get(i);
            if (!Strings.isEmpty(s))
            {
                String[] splitValues = Split.literal(s, this.splitOn, true);
                int size = Math.min(splitValues.length, newColumns.length);
                for (int j = 0; j < size; ++j)
                {
                    newColumns[j].add(splitValues[j]);
                }
                for (int j = size; j < newColumns.length; ++j)
                {
                    newColumns[j].addNull();
                }
            } else
            {
                for (int j = 0; j < newColumns.length; ++j)
                {
                    newColumns[j].addNull();;
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
}
