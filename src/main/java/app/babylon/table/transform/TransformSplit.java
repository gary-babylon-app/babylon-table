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

    public enum Mode
    {
        ALL, FIRST, LAST;

        public static Mode parse(String s)
        {
            if (Strings.isEmpty(s))
            {
                return ALL;
            }
            return switch (s.toLowerCase(java.util.Locale.ROOT))
            {
                case "all" -> ALL;
                case "first" -> FIRST;
                case "last" -> LAST;
                default -> throw new IllegalArgumentException("Unknown split mode: " + s);
            };
        }
    }

    private final ColumnName columnToSplit;
    private final String splitOn;
    private final Mode mode;
    private final ColumnName[] splitColumnNames;

    public TransformSplit(ColumnName columnToSplit, String splitOn, ColumnName... splitColumnNames)
    {
        this(columnToSplit, splitOn, Mode.ALL, splitColumnNames);
    }

    public TransformSplit(ColumnName columnToSplit, String splitOn, Mode mode, ColumnName... splitColumnNames)
    {
        super(FUNCTION_NAME);
        this.columnToSplit = ArgumentCheck.nonNull(columnToSplit);
        this.splitOn = oneCharacter(splitOn);
        this.mode = mode == null ? Mode.ALL : mode;
        this.splitColumnNames = Arrays.copyOf(ArgumentCheck.nonNull(splitColumnNames), splitColumnNames.length);
        requireColumnCount(this.mode, this.splitColumnNames);
    }

    public TransformSplit(ColumnName columnToSplit, String splitOn, Collection<String> splitColumnNames)
    {
        this(columnToSplit, splitOn, Mode.ALL, splitColumnNames);
    }

    public TransformSplit(ColumnName columnToSplit, String splitOn, Mode mode, Collection<String> splitColumnNames)
    {
        super(FUNCTION_NAME);
        this.columnToSplit = ArgumentCheck.nonNull(columnToSplit);
        this.splitOn = oneCharacter(splitOn);
        this.mode = mode == null ? Mode.ALL : mode;
        this.splitColumnNames = columnNames(splitColumnNames);
        requireColumnCount(this.mode, this.splitColumnNames);
    }

    public ColumnName getColumnToSplit()
    {
        return this.columnToSplit;
    }

    public String getSplitOn()
    {
        return this.splitOn;
    }

    public Mode getMode()
    {
        return this.mode;
    }

    public ColumnName[] getSplitColumnNames()
    {
        return Arrays.copyOf(this.splitColumnNames, this.splitColumnNames.length);
    }

    public static TransformSplit of(String[] params)
    {
        if (!Is.empty(params) && params.length >= 4)
        {
            Mode mode = Mode.ALL;
            int columnStart = 2;
            if (params.length >= 5 && isMode(params[2]))
            {
                mode = Mode.parse(params[2]);
                columnStart = 3;
            }
            return of(ColumnName.of(params[0]), params[1], mode,
                    Arrays.asList(params).subList(columnStart, params.length));
        }
        return null;
    }

    public static TransformSplit of(ColumnName columnToSplit, String splitOn, ColumnName... splitColumnNames)
    {
        return new TransformSplit(columnToSplit, splitOn, splitColumnNames);
    }

    public static TransformSplit of(ColumnName columnToSplit, String splitOn, Mode mode, ColumnName... splitColumnNames)
    {
        return new TransformSplit(columnToSplit, splitOn, mode, splitColumnNames);
    }

    public static TransformSplit of(ColumnName columnToSplit, String splitOn, Collection<String> splitColumnNames)
    {
        return new TransformSplit(columnToSplit, splitOn, splitColumnNames);
    }

    public static TransformSplit of(ColumnName columnToSplit, String splitOn, Mode mode,
            Collection<String> splitColumnNames)
    {
        return new TransformSplit(columnToSplit, splitOn, mode, splitColumnNames);
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
                addSplitValues(newColumns, splitter, s);
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

    private void addSplitValues(ColumnObject.Builder<String>[] newColumns, Strings.Splitter splitter, String s)
    {
        if (this.mode == Mode.ALL)
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
            return;
        }

        int splitIndex = this.mode == Mode.FIRST
                ? s.indexOf(this.splitOn.charAt(0))
                : s.lastIndexOf(this.splitOn.charAt(0));
        if (splitIndex < 0)
        {
            addPart(newColumns[0], s, 0, s.length());
            newColumns[1].addNull();
            return;
        }
        addPart(newColumns[0], s, 0, splitIndex);
        addPart(newColumns[1], s, splitIndex + 1, s.length() - splitIndex - 1);
    }

    private static void addPart(ColumnObject.Builder<String> column, String source, int start, int length)
    {
        CharSequence stripped = Strings.stripx(source, start, length);
        column.add(stripped == null ? "" : stripped.toString());
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

    private static boolean isMode(String s)
    {
        if (Strings.isEmpty(s))
        {
            return false;
        }
        return "all".equalsIgnoreCase(s) || "first".equalsIgnoreCase(s) || "last".equalsIgnoreCase(s);
    }

    private static void requireColumnCount(Mode mode, ColumnName[] splitColumnNames)
    {
        if ((mode == Mode.FIRST || mode == Mode.LAST) && splitColumnNames.length != 2)
        {
            throw new IllegalArgumentException("Split mode " + mode.name().toLowerCase(java.util.Locale.ROOT)
                    + " requires exactly two output columns.");
        }
    }
}
