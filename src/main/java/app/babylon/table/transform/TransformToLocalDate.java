package app.babylon.table.transform;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.lang.Is;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.column.Columns;
import app.babylon.table.column.Transformer;
import app.babylon.text.Sentence.ParseMode;

public class TransformToLocalDate extends TransformBase
{
    public static final String FUNCTION_NAME = "ToDate";
    private final Collection<ColumnName> columnNames;
    private final DateFormat format;
    private final ParseMode parseMode;
    private final ColumnName newColumnName;

    private TransformToLocalDate(Builder builder)
    {
        super(FUNCTION_NAME);
        this.columnNames = ArgumentCheck.nonEmpty(builder.gatherColumnNames(new ArrayList<>()));
        this.format = builder.format;
        this.parseMode = builder.parseMode;
        if (builder.newColumnName != null && this.columnNames.size() != 1)
        {
            throw new IllegalArgumentException("A LocalDate transform can only write into a new column when exactly "
                    + "one source column is configured.");
        }
        this.newColumnName = builder.newColumnName;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(ColumnName columnName, ColumnName... additionalColumnNames)
    {
        return builder().withColumnNames(columnName, additionalColumnNames);
    }

    public static Builder builder(Collection<ColumnName> columnNames)
    {
        return builder().withColumnNames(columnNames);
    }

    public static final class Builder
    {
        private ColumnName columnName;
        private ColumnName[] additionalColumnNames;
        private DateFormat format;
        private ParseMode parseMode;
        private ColumnName newColumnName;

        private Builder()
        {
        }

        public Builder withColumnName(ColumnName columnName)
        {
            return withColumnNames(columnName);
        }

        public Builder withColumnNames(ColumnName columnName, ColumnName... additionalColumnNames)
        {
            this.columnName = ArgumentCheck.nonNull(columnName);
            this.additionalColumnNames = copyAdditionalColumnNames(additionalColumnNames);
            return this;
        }

        public Builder withColumnNames(Collection<ColumnName> columnNames)
        {
            ArgumentCheck.nonEmpty(columnNames);
            ColumnName[] copy = columnNames.toArray(new ColumnName[columnNames.size()]);
            return withColumnNames(copy[0], Arrays.copyOfRange(copy, 1, copy.length));
        }

        public Builder withFormat(DateFormat format)
        {
            this.format = format;
            return this;
        }

        public Builder withParseMode(ParseMode parseMode)
        {
            this.parseMode = parseMode;
            return this;
        }

        public Builder withNewColumnName(ColumnName newColumnName)
        {
            this.newColumnName = newColumnName;
            return this;
        }

        public TransformToLocalDate build()
        {
            return new TransformToLocalDate(this);
        }

        private Collection<ColumnName> gatherColumnNames(Collection<ColumnName> x)
        {
            if (x == null)
            {
                x = new ArrayList<ColumnName>();
            }
            if (this.columnName != null)
            {
                x.add(columnName);
            }
            if (this.additionalColumnNames != null)
            {
                for (ColumnName columnName : additionalColumnNames)
                {
                    if (columnName != null)
                    {
                        x.add(columnName);
                    }
                }
            }
            return x;
        }
    }

    public static TransformToLocalDate of(String... params)
    {
        if (!Is.empty(params))
        {
            if (params.length >= 3)
            {
                ColumnName columnName = ColumnName.parse(params[0]);
                ColumnName newColumnName = ColumnName.parse(params[1]);
                DateFormat dateFormat = DateFormat.parse(params[2]);
                ParseMode parseMode = params.length >= 4 ? ParseMode.parse(params[3]) : null;

                return builder(columnName).withNewColumnName(newColumnName).withFormat(dateFormat)
                        .withParseMode(parseMode).build();

            }
        }
        return null;
    }

    public ParseMode parseMode()
    {
        return this.parseMode;
    }

    public ColumnName[] columnNames()
    {
        return this.columnNames.toArray(new ColumnName[this.columnNames.size()]);
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public DateFormat format()
    {
        return this.format;
    }

    public ParseMode effectiveParseMode()
    {
        return this.parseMode == null ? ParseMode.EXACT : this.parseMode;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        Column[] validColumns = getColumns(columnsByName, this.columnNames);
        @SuppressWarnings("unchecked")
        ColumnObject<String>[] stringColumns = new ColumnObject[validColumns.length];
        for (int i = 0; i < validColumns.length; ++i)
        {
            Column c = validColumns[i];
            if (Columns.isStringColumn(c))
            {
                stringColumns[i] = Columns.asStringColumn(c);
            }
        }
        DateFormat[] inferred = DateFormatInference.inferFormats(stringColumns);
        DateFormat[] effectiveFormats = new DateFormat[validColumns.length];
        Map<DateFormat, Map<String, LocalDate>> valuesByFormat = buildValuesByFormat(validColumns, inferred);
        List<Column> transformedColumns = new ArrayList<>();

        for (int i = 0; i < validColumns.length; ++i)
        {
            Column c = validColumns[i];
            effectiveFormats[i] = resolveEffectiveFormat(c, inferred[i]);

            if (Columns.isStringColumn(c))
            {
                ColumnObject<String> cs = Columns.asStringColumn(c);
                ColumnName newColumnName = this.newColumnName == null ? c.getName() : this.newColumnName;
                DateFormat dateFormat = effectiveFormats[i];
                final Map<String, LocalDate> values = valuesByFormat.get(dateFormat);
                Transformer<String, LocalDate> transformer = Transformer.of(values::get, ColumnTypes.LOCALDATE,
                        newColumnName);
                transformedColumns.add(cs.transform(transformer));
            }
            else if (!(c instanceof ColumnObject<?> && LocalDate.class.equals(c.getType().getValueClass())))
            {
                throw new RuntimeException("Cannot convert to LocalDate from " + c.getName());
            }
        }
        if (transformedColumns.size() > 0)
        {
            putColumns(columnsByName, transformedColumns.toArray(new Column[transformedColumns.size()]));
        }
    }

    private Map<DateFormat, Map<String, LocalDate>> buildValuesByFormat(Column[] validColumns, DateFormat[] inferred)
    {
        Map<DateFormat, Set<String>> uniqueValuesByFormat = new HashMap<>();
        for (int i = 0; i < validColumns.length; ++i)
        {
            Column column = validColumns[i];
            if (!Columns.isStringColumn(column))
            {
                continue;
            }
            DateFormat dateFormat = resolveEffectiveFormat(column, inferred[i]);
            Set<String> values = uniqueValuesByFormat.computeIfAbsent(dateFormat, k -> new HashSet<>());
            Columns.asStringColumn(column).getUniques(values);
        }

        Map<DateFormat, Map<String, LocalDate>> valuesByFormat = new HashMap<>();
        for (Map.Entry<DateFormat, Set<String>> e : uniqueValuesByFormat.entrySet())
        {
            DateFormat dateFormat = e.getKey();
            Map<String, LocalDate> values = new HashMap<>();
            for (String s : e.getValue())
            {
                values.put(s, parseLocalDate(dateFormat, s));
            }
            valuesByFormat.put(dateFormat, values);
        }
        return valuesByFormat;
    }

    private DateFormat resolveEffectiveFormat(Column column, DateFormat inferredFormat)
    {
        DateFormat format = inferredFormat;
        if (format == null || format == DateFormat.Unknown)
        {
            format = this.format;
        }
        if ((format == null || format == DateFormat.Unknown) && isEmptyStringColumn(column))
        {
            return null;
        }
        if (format == null || format == DateFormat.Unknown)
        {
            throw new IllegalArgumentException("Could not infer date format for column " + column.getName()
                    + " and no fallback DateFormat was provided.");
        }
        return format;
    }

    private static boolean isEmptyStringColumn(Column column)
    {
        if (!Columns.isStringColumn(column))
        {
            return false;
        }
        ColumnObject<String> strings = Columns.asStringColumn(column);
        for (int i = 0; i < strings.size(); ++i)
        {
            if (strings.isSet(i))
            {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseLocalDate(DateFormat format, String s)
    {
        return effectiveParseMode().apply(value -> ColumnLocalDates.stringToDate(value, format), s);
    }

    private static ColumnName[] copyAdditionalColumnNames(ColumnName... columnNames)
    {
        ColumnName[] copy = Arrays.copyOf(ArgumentCheck.nonNull(columnNames), columnNames.length);
        for (ColumnName columnName : copy)
        {
            ArgumentCheck.nonNull(columnName);
        }
        return copy;
    }
}
