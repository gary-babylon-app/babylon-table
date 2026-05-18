package app.babylon.table.transform;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
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

    private TransformTakeToType(Operation operation, ColumnName columnName, ColumnName newColumnName, Column.Type type,
            ParseMode parseMode)
    {
        super(FUNCTION_NAME);
        this.operation = ArgumentCheck.nonNull(operation);
        this.columnName = ArgumentCheck.nonNull(columnName);
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
        this.type = ArgumentCheck.nonNull(type);
        this.parseMode = parseMode;
    }

    public static Indexed indexed(Operation operation, ColumnName columnName, ColumnName newColumnName,
            Column.Type type, ParseMode parseMode, int first, int last)
    {
        return new Indexed(operation, columnName, newColumnName, type, parseMode, first, last);
    }

    public static Delimited delimited(Operation operation, ColumnName columnName, ColumnName newColumnName,
            Column.Type type, ParseMode parseMode, String delimiter)
    {
        return new Delimited(operation, columnName, newColumnName, type, parseMode, delimiter);
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
        return List.of(this.columnName);
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

    protected abstract String extract(String s);

    public static final class Indexed extends TransformTakeToType
    {
        private final int first;
        private final int last;

        private Indexed(Operation operation, ColumnName columnName, ColumnName newColumnName, Column.Type type,
                ParseMode parseMode, int first, int last)
        {
            super(operation, columnName, newColumnName, type, parseMode);
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
                ParseMode parseMode, String delimiter)
        {
            super(operation, columnName, newColumnName, type, parseMode);
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
