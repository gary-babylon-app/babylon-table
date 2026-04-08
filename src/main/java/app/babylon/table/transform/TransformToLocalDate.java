package app.babylon.table.transform;

import java.time.LocalDate;
import java.util.ArrayList;
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
import app.babylon.table.column.Columns;
import app.babylon.table.column.Transformer;

public class TransformToLocalDate extends TransformBase
{
    public static final String FUNCTION_NAME = "ToDate";
    private final ColumnName[] columnNames;
    private final DateFormat format;
    private final ColumnName[] newColumnNames;

    public TransformToLocalDate(ColumnName... columnNames)
    {
        this(null, columnNames);
    }

    public TransformToLocalDate(Collection<ColumnName> columnNames)
    {
        this(null, app.babylon.lang.ArgumentCheck.nonNull(columnNames.toArray(new ColumnName[columnNames.size()])));
    }

    public TransformToLocalDate(DateFormat format, ColumnName... columnNames)
    {
        super(FUNCTION_NAME);
        this.columnNames = ArgumentCheck.nonEmpty(columnNames);
        this.format = format;
        this.newColumnNames = null;
    }

    public TransformToLocalDate(ColumnName columnName, ColumnName newColumnName, DateFormat format)
    {
        super(FUNCTION_NAME);
        this.columnNames = new ColumnName[]
        {app.babylon.lang.ArgumentCheck.nonNull(columnName)};
        this.format = format;
        this.newColumnNames = new ColumnName[]
        {app.babylon.lang.ArgumentCheck.nonNull(newColumnName)};
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

                return new TransformToLocalDate(columnName, newColumnName, dateFormat);

            }
        }
        return null;
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        if (!Is.empty(this.columnNames))
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
                    ColumnName newColumnName = (newColumnNames == null) ? c.getName() : newColumnNames[i];
                    DateFormat dateFormat = effectiveFormats[i];
                    final Map<String, LocalDate> values = valuesByFormat.get(dateFormat);
                    Transformer<String, LocalDate> transformer = Transformer.of(values::get, LocalDate.class,
                            newColumnName);
                    transformedColumns.add(cs.transform(transformer));
                } else if (!(c instanceof ColumnObject<?> && LocalDate.class.equals(c.getType().getValueClass())))
                {
                    throw new RuntimeException("Cannot convert to LocalDate from " + c.getName());
                }
            }
            if (transformedColumns.size() > 0)
            {
                putColumns(columnsByName, transformedColumns.toArray(new Column[transformedColumns.size()]));
            }
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

    private static LocalDate parseLocalDate(DateFormat format, CharSequence s)
    {
        return ColumnLocalDates.stringToDate(s, format);
    }
}
