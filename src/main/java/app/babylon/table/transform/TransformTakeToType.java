package app.babylon.table.transform;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnByte;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.dsl.ConditionExpression;
import app.babylon.table.selection.RowPredicate;
import app.babylon.text.Sentence.ParseMode;
import app.babylon.text.Strings;

public abstract class TransformTakeToType extends TransformBase implements TransformToColumn
{
    public static final String FUNCTION_NAME = "TakeToType";

    public enum Operation
    {
        LEFT, RIGHT, SUBSTRING, BEFORE, AFTER
    }

    private final Operation operation;
    private final ColumnName columnName;
    private final ColumnName newColumnName;
    private final Column.Type type;
    private final ParseMode parseMode;
    private final ConditionExpression condition;

    private TransformTakeToType(Operation operation, ColumnName columnName, ColumnName newColumnName, Column.Type type,
            ParseMode parseMode, ConditionExpression condition)
    {
        super(FUNCTION_NAME);
        this.operation = ArgumentCheck.nonNull(operation);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.type = ArgumentCheck.nonNull(type);
        this.parseMode = parseMode;
        this.condition = condition;
    }

    public static Indexed indexed(Operation operation, ColumnName columnName, ColumnName newColumnName,
            Column.Type type, ParseMode parseMode, int first, int last)
    {
        return indexed(operation, columnName, newColumnName, type, parseMode, first, last, null);
    }

    public static Indexed indexed(Operation operation, ColumnName columnName, ColumnName newColumnName,
            Column.Type type, ParseMode parseMode, int first, int last, ConditionExpression condition)
    {
        return new Indexed(operation, columnName, newColumnName, type, parseMode, first, last, condition);
    }

    public static Delimited delimited(Operation operation, ColumnName columnName, ColumnName newColumnName,
            Column.Type type, ParseMode parseMode, String delimiter)
    {
        return delimited(operation, columnName, newColumnName, type, parseMode, delimiter, null);
    }

    public static Delimited delimited(Operation operation, ColumnName columnName, ColumnName newColumnName,
            Column.Type type, ParseMode parseMode, String delimiter, ConditionExpression condition)
    {
        return new Delimited(operation, columnName, newColumnName, type, parseMode, delimiter, condition);
    }

    public Operation operation()
    {
        return this.operation;
    }

    public ColumnName columnName()
    {
        return this.columnName;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    public Column.Type type()
    {
        return this.type;
    }

    public ParseMode parseMode()
    {
        return this.parseMode;
    }

    public ConditionExpression condition()
    {
        return this.condition;
    }

    public ParseMode effectiveParseMode()
    {
        return this.parseMode == null ? ParseMode.EXACT : this.parseMode;
    }

    @Override
    public ColumnName outputColumnName()
    {
        return this.newColumnName;
    }

    @Override
    public Collection<ColumnName> sourceColumnNames()
    {
        if (this.condition == null)
        {
            return List.of(this.columnName);
        }
        Set<ColumnName> names = new LinkedHashSet<>();
        names.add(this.columnName);
        names.add(this.newColumnName);
        names.addAll(this.condition.columnNames());
        return names;
    }

    @Override
    public Column transform(Map<ColumnName, Column> columnsByName, int rowCount)
    {
        Column source = columnsByName == null ? null : columnsByName.get(this.columnName);
        if (!Columns.isStringColumn(source))
        {
            return null;
        }
        ColumnObject<String> strings = Columns.asStringColumn(source);
        Column.Builder builder = this.type.isPrimitive()
                ? Columns.newBuilder(this.newColumnName, this.type)
                : ColumnObject.builder(this.newColumnName, this.type, ColumnObject.Mode.CATEGORICAL);
        ParseMode resolvedParseMode = effectiveParseMode();
        if (this.condition != null)
        {
            Column target = columnsByName.get(this.newColumnName);
            if (target != null && !target.getType().equals(this.type))
            {
                throw new IllegalArgumentException("Conditional take target column type must match take type.");
            }
            RowPredicate predicate = this.condition.prepare(columnsByName);
            for (int i = 0; i < strings.size(); ++i)
            {
                if (target != null && target.isSet(i))
                {
                    addExisting(builder, target, i);
                }
                else if (predicate.test(i) && strings.isSet(i))
                {
                    builder.add(resolvedParseMode, extract(strings.get(i)));
                }
                else
                {
                    builder.addNull();
                }
            }
            return builder.build();
        }
        for (int i = 0; i < strings.size(); ++i)
        {
            if (!strings.isSet(i))
            {
                builder.addNull();
                continue;
            }
            builder.add(resolvedParseMode, extract(strings.get(i)));
        }
        return builder.build();
    }

    private static void addExisting(Column.Builder builder, Column column, int row)
    {
        if (column instanceof ColumnObject<?> object)
        {
            addExistingObject(builder, object, row);
        }
        else if (builder instanceof ColumnBoolean.Builder booleanBuilder
                && column instanceof ColumnBoolean booleanColumn)
        {
            booleanBuilder.add(booleanColumn.get(row));
        }
        else if (builder instanceof ColumnByte.Builder byteBuilder && column instanceof ColumnByte byteColumn)
        {
            byteBuilder.add(byteColumn.get(row));
        }
        else if (builder instanceof ColumnInt.Builder intBuilder && column instanceof ColumnInt intColumn)
        {
            intBuilder.add(intColumn.get(row));
        }
        else if (builder instanceof ColumnLong.Builder longBuilder && column instanceof ColumnLong longColumn)
        {
            longBuilder.add(longColumn.get(row));
        }
        else if (builder instanceof ColumnDouble.Builder doubleBuilder && column instanceof ColumnDouble doubleColumn)
        {
            doubleBuilder.add(doubleColumn.get(row));
        }
        else
        {
            builder.add(column.toString(row));
        }
    }

    @SuppressWarnings("unchecked")
    private static void addExistingObject(Column.Builder builder, ColumnObject<?> column, int row)
    {
        ((ColumnObject.Builder<Object>) builder).add(column.get(row));
    }

    protected abstract String extract(String s);

    public static final class Indexed extends TransformTakeToType
    {
        private final int first;
        private final int last;

        private Indexed(Operation operation, ColumnName columnName, ColumnName newColumnName, Column.Type type,
                ParseMode parseMode, int first, int last, ConditionExpression condition)
        {
            super(operation, columnName, newColumnName, type, parseMode, condition);
            if (operation != Operation.LEFT && operation != Operation.RIGHT && operation != Operation.SUBSTRING)
            {
                throw new IllegalArgumentException("Indexed take does not support " + operation);
            }
            this.first = Math.max(0, first);
            this.last = Math.max(this.first + 1, last);
        }

        public int length()
        {
            return this.first;
        }

        public int first()
        {
            return this.first;
        }

        public int last()
        {
            return this.last;
        }

        @Override
        protected String extract(String s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            return switch (operation())
            {
                case LEFT -> s.substring(0, Math.min(this.first, s.length()));
                case RIGHT -> s.substring(Math.max(0, s.length() - this.first));
                case SUBSTRING -> s.length() >= this.last ? s.substring(this.first, this.last) : null;
                case BEFORE, AFTER -> throw new IllegalStateException("Unsupported indexed take: " + operation());
            };
        }
    }

    public static final class Delimited extends TransformTakeToType
    {
        private final String delimiter;

        private Delimited(Operation operation, ColumnName columnName, ColumnName newColumnName, Column.Type type,
                ParseMode parseMode, String delimiter, ConditionExpression condition)
        {
            super(operation, columnName, newColumnName, type, parseMode, condition);
            if (operation != Operation.BEFORE && operation != Operation.AFTER)
            {
                throw new IllegalArgumentException("Delimited take does not support " + operation);
            }
            this.delimiter = ArgumentCheck.nonEmpty(delimiter);
        }

        public String delimiter()
        {
            return this.delimiter;
        }

        @Override
        protected String extract(String s)
        {
            if (Strings.isEmpty(s))
            {
                return null;
            }
            int index = s.indexOf(this.delimiter);
            if (index < 0)
            {
                return null;
            }
            return operation() == Operation.BEFORE
                    ? s.substring(0, index)
                    : s.substring(index + this.delimiter.length());
        }
    }
}
